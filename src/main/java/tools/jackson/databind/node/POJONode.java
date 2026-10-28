package tools.jackson.databind.node;

import java.io.Serializable;

/**
 * Jackson 3 (tools.jackson) POJONode 的 JDK8 最小骨架。
 *
 * 仅用于让生成端在 JDK8 下能编译、实例化并序列化出 Jackson 3 的 POJONode 字节流。
 * 反序列化端需使用真正的 tools.jackson 3.x，本骨架只保证：
 *   - 类全限定名 = tools.jackson.databind.node.POJONode
 *   - serialVersionUID = 3L（与 jackson3 一致）
 *   - 字段 _value（与 jackson3 一致）
 *   - 构造器 POJONode(Object)
 *
 * 注意：为了在 JDK8 编译，这里去掉了 jackson3 原本的继承链（ValueNode/BaseJsonNode/JsonNode）
 * 以及 Java 16+ 的 pattern matching 语法，直接实现 Serializable。
 */
public class POJONode implements Serializable {

    private static final long serialVersionUID = 3L;

    protected final Object _value;

    public POJONode(Object v) {
        _value = v;
    }

    /** 对齐 jackson3 的取值方法，便于调试/校验 */
    public Object getPojo() {
        return _value;
    }
}
