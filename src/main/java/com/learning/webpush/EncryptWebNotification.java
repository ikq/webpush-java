package com.learning.webpush;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;

import java.util.Base64;


public class EncryptWebNotification {
    static final SecureRandom SECURE_RANDOM;

    static {
        SECURE_RANDOM = new SecureRandom();
        try {
            Security.addProvider(new BouncyCastleProvider());
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // The first usage of secure random initialize seed and take about 1.5 seconds.
    public static byte[] init(){
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);
        return salt;
    }

    public static byte[] encrypt(String message, String p256dh, String auth, int recordSize) throws Exception {
        byte[] clearText = message.getBytes("UTF8");
        ECPublicKey remotePublicKey = (ECPublicKey) loadPublicKey(base64decode(p256dh));
        byte[] authSecret = base64decode(auth);

        long start = System.nanoTime();

        //System.out.println("0: salt...");
        byte[] salt = new byte[16];
        SECURE_RANDOM.nextBytes(salt);

        // Application server key pairs (single use)
        ECNamedCurveParameterSpec parameterSpec = ECNamedCurveTable.getParameterSpec("prime256v1");
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDH", "BC");
        keyPairGenerator.initialize(parameterSpec);
        KeyPair localKeyPair = keyPairGenerator.generateKeyPair();

        ECPublicKey localPublicKey = (ECPublicKey) localKeyPair.getPublic();
        ECPrivateKey localPrivateKey = (ECPrivateKey) localKeyPair.getPrivate();

        KeyAgreement ka = KeyAgreement.getInstance("ECDH", "BC");
        ka.init(localPrivateKey);
        ka.doPhase(remotePublicKey, true);
        byte[] sharedECDHSecret = ka.generateSecret();

        // Input Key Material
        byte[] prkInfoBuf = concat("WebPush: info\0".getBytes(), storePublicKey(remotePublicKey), storePublicKey(localPublicKey));
        byte[] ikm = getHKDFKey(sharedECDHSecret, authSecret, prkInfoBuf, 32);

        // Derive Content Encryption Key
        byte[] contentEncryptionKey = getHKDFKey(ikm, salt, "Content-Encoding: aes128gcm\0".getBytes(), 16);

        // Derive the Nonce
        byte[] nonce = getHKDFKey(ikm, salt, "Content-Encoding: nonce\0".getBytes(), 12);

        // Cipher
        // Note: Cipher adds the tag to the end of the ciphertext. Tag length is 16 bytes.
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "BC");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, new SecretKeySpec(contentEncryptionKey, "AES"), new GCMParameterSpec(128, nonce));

        if (recordSize == 0) {
            recordSize = 4096; // max record size
        }

        // Content-Coding header
        ByteBuffer recordSizeBuf = ByteBuffer.allocate(4);
        recordSizeBuf.putInt(recordSize);
        byte[] localPublicKeyData = storePublicKey(localPublicKey);
        byte[] keylenByte = new byte[]{(byte) localPublicKeyData.length};
        byte[] header = concat(salt, recordSizeBuf.array(), keylenByte, localPublicKeyData);

        // Padding
        int recordLength = recordSize - 16;
        int maxPadLen = recordLength - header.length;
        int padLen = maxPadLen - clearText.length;
        if (padLen <= 0)
            throw new Error("message is too big");
        byte[] padding = new byte[padLen];
        padding[0] = 2;

        byte[] cipherText1 = cipher.update(clearText);
        byte[] cipherText2 = cipher.update(padding);
        byte[] cipherText3 = cipher.doFinal();
        byte[] result = concat(header, cipherText1, cipherText2, cipherText3);
        // Most time used for generate DH shared secret.
        System.out.println("Encryption take " + ((System.nanoTime() - start) / 1000000) + " ms");
        return result;
    }

    static byte[] base64decode(String base64) {
        return Base64.getUrlDecoder().decode(base64);
    }

    static PublicKey loadPublicKey(byte[] data) throws Exception {
        ECParameterSpec params = ECNamedCurveTable.getParameterSpec("prime256v1");
        ECPublicKeySpec pubKey = new ECPublicKeySpec(params.getCurve().decodePoint(data), params);
        KeyFactory kf = KeyFactory.getInstance("ECDH", "BC");
        return kf.generatePublic(pubKey);
    }

    static byte[] storePublicKey(ECPublicKey publicKey) {
        return publicKey.getQ().getEncoded(false);
    }

    // HMAC-based Extract-and-Expand Key Derivation Function (HKDF)
    static byte[] getHKDFKey(byte[] ikm, byte[] salt, byte[] info, int length) {
        HKDFBytesGenerator hkdf = new HKDFBytesGenerator(new SHA256Digest());
        hkdf.init(new HKDFParameters(ikm, salt, info));
        byte[] okm = new byte[length];
        hkdf.generateBytes(okm, 0, length);
        return okm;
    }

    static byte[] concat(byte[]... arrays) {
        int combinedLength = 0;
        for (byte[] array : arrays) {
            if (array == null) {
                continue;
            }
            combinedLength += array.length;
        }

        byte[] combined = new byte[combinedLength];
        int lastPos = 0;
        for (byte[] array : arrays) {
            if (array == null) {
                continue;
            }
            System.arraycopy(array, 0, combined, lastPos, array.length);
            lastPos += array.length;
        }
        return combined;
    }

    static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
