package com.learning.webpush;

import java.security.*;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPrivateKeySpec;
import org.bouncycastle.util.BigIntegers;
import java.math.BigInteger;
import java.net.URL;
import java.security.PrivateKey;
import java.util.Base64;

/*
  Create web-push authorization header with vapid JSON Web Tokens (JWT)
 */
public class Vapid {
    public static String createHeader(String endpoint, String subscriber, int expiresHours, String vapidPublicKey, String vapidPrivateKey) throws Exception {
        String header = "{\"alg\":\"ES256\",\"typ\":\"JWT\"}";
        URL url = new URL(endpoint);
        String exp = Long.toString(System.currentTimeMillis() / 1000 + expiresHours * 3600);
        String payload = "{\"aud\":\"" + url.getProtocol() + "://" + url.getHost() + "\"," +
                "\"exp\":" + exp +
                ",\"sub\":\"mailto:" + subscriber + "\"}";

        String data = encode(header) + "." + encode(payload);

        PrivateKey privateKey = loadPrivateKey(vapidPrivateKey);
        Signature dsa = Signature.getInstance("SHA256withECDSA");
        dsa.initSign(privateKey);
        dsa.update(data.getBytes());
        byte[] signature = dsa.sign();
        byte[] concatSignature = concatSignature(signature);

        String jwt = data + "." + encode(concatSignature);
        return "vapid t=" + jwt + ",k=" + vapidPublicKey;
    }


    static String encode(String str) {
        return encode(str.getBytes());
    }

    static String encode(byte[] data) {
        return new String(Base64.getUrlEncoder().withoutPadding().encode(data));
    }

    static PrivateKey loadPrivateKey(String encodedPrivateKey) throws Exception {
        byte[] decodedPrivateKey = Base64.getUrlDecoder().decode(encodedPrivateKey);
        BigInteger s = BigIntegers.fromUnsignedByteArray(decodedPrivateKey);
        ECParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        ECPrivateKeySpec privateKeySpec = new ECPrivateKeySpec(s, parameterSpec);
        KeyFactory keyFactory = KeyFactory.getInstance("ECDH", "BC");
        return keyFactory.generatePrivate(privateKeySpec);
    }

    static byte[] concatSignature(byte signature[]) throws Exception {
        final int outLen = 64; // for ES256
        if (signature.length < 8 || signature[0] != 48)
            throw new Exception("Invalid signature");
        int offset;
        if (signature[1] > 0)
            offset = 2;
        else if (signature[1] == (byte) 0x81)
            offset = 3;
        else
            throw new Exception("Invalid signature (2)");
        byte rLength = signature[offset + 1];
        int i;
        for (i = rLength; (i > 0) && (signature[(offset + 2 + rLength) - i] == 0); i--) ;
        byte sLength = signature[offset + 2 + rLength + 1];
        int j;
        for (j = sLength; (j > 0) && (signature[(offset + 2 + rLength + 2 + sLength) - j] == 0); j--) ;
        int rawLen = Math.max(i, j);
        rawLen = Math.max(rawLen, outLen / 2);
        if ((signature[offset - 1] & 0xff) != signature.length - offset
                || (signature[offset - 1] & 0xff) != 2 + rLength + 2 + sLength
                || signature[offset] != 2
                || signature[offset + 2 + rLength] != 2)
            throw new Exception("Invalid signature (3)");

        byte result[] = new byte[2 * rawLen];
        System.arraycopy(signature, (offset + 2 + rLength) - i, result, rawLen - i, i);
        System.arraycopy(signature, (offset + 2 + rLength + 2 + sLength) - j, result, 2 * rawLen - j, j);
        return result;
    }
}
