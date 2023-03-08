package top.iot.gateway.agent;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;

public class M {
    public static void main(String[] args) {
        System.out.println(111);
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            keystore.load(new FileInputStream("D:\\project\\tsl\\1phase\\mytest.jks"), "mypass".toCharArray());
            System.out.println("success");


            String jksFile = "D:\\project\\tsl\\1phase\\mytest.jks";
            KeyStore ks = KeyStore.getInstance("jks");
            String password = "mypass";
            InputStream in = null;
            try {
                in = new FileInputStream(jksFile);
                ks.load(in, password != null ? password.toCharArray() : null);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignore) {
                        ignore.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
