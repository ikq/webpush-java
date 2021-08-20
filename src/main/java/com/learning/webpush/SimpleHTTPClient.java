package com.learning.webpush;

import java.net.URL;
import java.io.*;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

public class SimpleHTTPClient {
    String subscriber = "admin@example.com";
    String vapidPublicKey = null;
    String vapidPrivateKey = null;
    int    ttl = 30;
    String urgency = null; // very-low low normal high

    public SimpleHTTPClient() throws Exception {
        bypassSSLSecurity();
    }

    public void setSubscriber(String subscriber) {
        this.subscriber = subscriber;
    }

    public void setUrgency(String urgency){
        if( urgency != null && !urgency.equals("very-low") && !urgency.equals("low") && !urgency.equals("normal") && !urgency.equals("high") )
            throw new Error("Illegal urgency value: \"" + urgency + "\"");
        this.urgency = urgency;
    }

    public void setVapidKeys(String vapidPrivateKey, String vapidPublicKey) {
        this.vapidPrivateKey = vapidPrivateKey;
        this.vapidPublicKey = vapidPublicKey;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public static void bypassSSLSecurity() throws Exception {
        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };

        // Install the all-trusting trust manager
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, new java.security.SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Create all-trusting host name verifier
        HostnameVerifier allHostsValid = new HostnameVerifier() {
            public boolean verify(String hostname, SSLSession session) {
                return true;
            }
        };

        // Install the all-trusting host verifier
        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
    }

    public void pushNotification(String endpoint, byte [] notification, String topic) throws Exception {
        URL myUrl = new URL(endpoint);
        HttpsURLConnection con = (HttpsURLConnection) myUrl.openConnection();
        con.setRequestProperty("Content-Encoding", "aes128gcm");
        con.setRequestProperty("Content-Length", Integer.toString(notification.length));
        con.setRequestProperty("Content-Type", "application/octet-stream");
        con.setRequestProperty("TTL", Integer.toString(ttl));
        if( topic != null) {
            con.setRequestProperty("Topic", topic);
        }
        if( urgency != null ){
            con.setRequestProperty("Urgency", urgency);
        }

        String vapidAuth = Vapid.createHeader(endpoint, subscriber, 12, vapidPublicKey, vapidPrivateKey);
        con.setRequestProperty("Authorization", vapidAuth);

        con.setDoOutput(true);
        con.setRequestMethod("POST");
        OutputStream os = con.getOutputStream();
        os.write(notification, 0, notification.length);
        os.close();
        //con.setUseCaches(false);
        //con.setDoInput(true);
        //con.setDoOutput(true);
        try {
            con.connect();

            InputStream is = con.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String inputLine;
            while ((inputLine = br.readLine()) != null) {
                System.out.println(inputLine);
            }
            br.close();
        } catch(Exception e){
            System.out.println(e);
        }
    }
}
