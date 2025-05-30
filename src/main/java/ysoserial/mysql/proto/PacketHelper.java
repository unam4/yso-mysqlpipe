package ysoserial.mysql.proto;

import ysoserial.mysql.proto.utils.ByteUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Arrays;

@Slf4j
public class PacketHelper {
    public static byte[] readData(InputStream inputStream) {
        try {
            byte[] buffer = new byte[1024 * 10];
            int bytesRead = inputStream.read(buffer);
            if (bytesRead != -1) {
                return Arrays.copyOf(buffer, bytesRead);
            }
        } catch (Exception ex) {
            log.error("socket error");
        }
        return new byte[]{};
    }

    public static byte[] buildPacket(int num, byte[] d) {
        try {
            if (d.length > 16777216) {
                log.error("packet is too long");
                return null;
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] lenHex = ByteUtil.ReverseBytes(ByteUtil.int3ToBytes(d.length));
            byte[] numHex = ColumnPacket.hexToBytes(String.format("%02d", num));
            out.write(lenHex);
            out.write(numHex);
            out.write(d);
            return out.toByteArray();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new byte[]{};
    }
}
