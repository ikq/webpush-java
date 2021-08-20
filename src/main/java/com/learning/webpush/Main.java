package com.learning.webpush;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class Main {
    public static void main(String args[]) throws Exception {
        Properties srv = getProp(true,"server");
        String vapidPublicKey = srv.getProperty("vapidPublicKey");
        String vapidPrivateKey = srv.getProperty("vapidPrivateKey");
        int ttl = Integer.parseInt(srv.getProperty("ttl", "60"));
        String urgency = srv.getProperty("urgency");
        String subscriber = srv.getProperty("subscriber", "admin@example.com");

        Properties subs = getProp(false, "subscription");
        String endpoint = subs.getProperty("endpoint");
        String key = subs.getProperty("p256dh");
        String auth = subs.getProperty("auth");

        EncryptWebNotification.init();

        System.out.println("Encrypting message...");
        String message = String.join(" ", args);
		// Encrypt notification message
        byte[] notification = EncryptWebNotification.encrypt(message, key, auth, 0);

        SimpleHTTPClient client = new SimpleHTTPClient();

        // HTTP POST headers
        client.setSubscriber(subscriber);
        client.setUrgency(urgency);
        client.setTtl(ttl);
        client.setVapidKeys(vapidPrivateKey, vapidPublicKey);

        System.out.println("Sending...");
        client.pushNotification(endpoint, notification, "Testing");
    }

    static Properties getProp(boolean isResources, String fileName) throws Exception {
        fileName += ".properties";
        InputStream input;
        if( isResources ) {
            input = Main.class.getClassLoader().getResourceAsStream(fileName);
        } else {
            input = new FileInputStream(fileName);
        }
        Properties prop = new Properties();
        prop.load(input);
        return prop;
    }
}
