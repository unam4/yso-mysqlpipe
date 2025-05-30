package ysoserial.mysql.proto;


import ysoserial.mysql.proto.constant.Capability;
import ysoserial.mysql.proto.constant.Charset;
import ysoserial.mysql.proto.constant.Status;
import ysoserial.mysql.proto.utils.ByteUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;

@Slf4j
public class GreetingMessage {
    private byte[] ProtocolVersion; // 1
    private byte[] VersionString; // max:31
    private byte[] ServerThreadID; // 4
    private byte[] Random; // 8
    private byte[] Padding; // 1
    private byte[] CaLow; // 2
    private byte[] Encode; // 1
    private byte[] ServerStatus; // 2
    private byte[] CaHigh; // 2
    private byte[] CL; // 1
    private byte[] OtherPadding; // 10
    private byte[] SECURE_CONNECTION; // 13
    private byte[] PLUGIN_AUTH; // plugin
    private byte[] End; // 1

    public GreetingMessage() {
        try {
            this.ProtocolVersion = new byte[]{(byte) 0x0a};
            log.debug("protocol version: {}", ColumnPacket.bytesToHex(this.ProtocolVersion));

            this.VersionString = "5.0.2".getBytes();
            log.debug("version string: {}", new String(this.VersionString));

            this.ServerThreadID = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            log.debug("server thread id: {}", ColumnPacket.bytesToHex(this.ServerThreadID));

            this.Random = new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                    (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01};
            log.debug("random: {}", ColumnPacket.bytesToHex(this.Random));

            this.Padding = new byte[]{(byte) 0x00};
            log.debug("padding: {}", ColumnPacket.bytesToHex(this.Padding));

            int finalCapability = Capability.LONG_PASSWORD + Capability.LONG_FLAG +
                    Capability.CONNECT_WITH_DB + Capability.PROTOCOL_41 + Capability.TRANSACTIONS +
                    Capability.SECURE_CONNECTION + Capability.PLUGIN_AUTH;
            int low = finalCapability & 0xFFFF;
            int high = (finalCapability >> 16) & 0xFFFF;

            this.CaLow = ByteUtil.int16ToByteArray((short) low);
            log.debug("capability low hex: {}", ColumnPacket.bytesToHex(this.CaLow));

            this.Encode = new byte[]{Charset.UTF8};
            log.debug("encode: {}", ColumnPacket.bytesToHex(this.Encode));

            this.ServerStatus = ByteUtil.int16ToByteArray((short) Status.STATUS_AUTOCOMMIT);
            log.debug("capability low hex: {}", ColumnPacket.bytesToHex(this.ServerStatus));

            this.CaHigh = ByteUtil.int16ToByteArray((short) high);
            log.debug("capability high hex: {}", ColumnPacket.bytesToHex(this.CaHigh));

            this.CL = new byte[]{0x00};
            log.debug("cl: {}", ColumnPacket.bytesToHex(this.CL));

            this.OtherPadding = new byte[]{(byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01,
                    (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01, (byte) 0x01};
            log.debug("other padding: {}", ColumnPacket.bytesToHex(this.OtherPadding));

            this.SECURE_CONNECTION = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                    (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00};
            log.debug("SECURE_CONNECTION: {}", ColumnPacket.bytesToHex(this.SECURE_CONNECTION));

            this.PLUGIN_AUTH = "mysql_clear_password".getBytes();
            log.debug("PLUGIN_AUTH: {}", ColumnPacket.bytesToHex(this.PLUGIN_AUTH));

            this.End = new byte[]{(byte) 0x00};
            log.debug("end: {}", ColumnPacket.bytesToHex(this.End));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public byte[] getBytes() {
        ByteArrayOutputStream out;
        try {
            out = new ByteArrayOutputStream();
            out.write(this.ProtocolVersion);
            out.write(this.VersionString);
            // string end
            out.write(new byte[]{(byte) 0x00});
            out.write(this.ServerThreadID);
            out.write(this.Random);
            out.write(this.Padding);
            out.write(this.CaLow);
            out.write(this.Encode);
            out.write(this.ServerStatus);
            out.write(this.CaHigh);
            out.write(this.CL);
            out.write(this.OtherPadding);
            out.write(this.SECURE_CONNECTION);
            out.write(this.PLUGIN_AUTH);
            out.write(this.End);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        return out.toByteArray();
    }
}
