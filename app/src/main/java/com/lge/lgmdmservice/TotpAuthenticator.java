package com.lge.lgmdmservice;

import java.security.SecureRandom;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** RFC 6238 TOTP helper compatible with Android API 23. */
final class TotpAuthenticator {
    private static final char[] BASE32 = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();
    private TotpAuthenticator() {}

    static String newSecret() {
        byte[] bytes = new byte[20];
        new SecureRandom().nextBytes(bytes);
        return encodeBase32(bytes);
    }

    static boolean verify(String secret, String code, long nowMillis) {
        if (secret == null || code == null || !code.matches("[0-9]{6}")) return false;
        try {
            byte[] key = decodeBase32(secret);
            long counter = (nowMillis / 1000L) / 30L;
            for (long offset = -1; offset <= 1; offset++) {
                if (generate(key, counter + offset).equals(code)) return true;
            }
        } catch (Exception ignored) {
            return false;
        }
        return false;
    }

    private static String generate(byte[] key, long counter) throws Exception {
        byte[] message = new byte[8];
        for (int i = 7; i >= 0; i--) { message[i] = (byte) counter; counter >>>= 8; }
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(new SecretKeySpec(key, "HmacSHA1"));
        byte[] hash = mac.doFinal(message);
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % 1000000;
        return String.format(java.util.Locale.US, "%06d", otp);
    }

    private static String encodeBase32(byte[] data) {
        StringBuilder out = new StringBuilder();
        int buffer = 0, bits = 0;
        for (byte value : data) {
            buffer = (buffer << 8) | (value & 0xff); bits += 8;
            while (bits >= 5) { out.append(BASE32[(buffer >> (bits - 5)) & 31]); bits -= 5; }
        }
        if (bits > 0) out.append(BASE32[(buffer << (5 - bits)) & 31]);
        return out.toString();
    }

    private static byte[] decodeBase32(String input) {
        String normalized = input.toUpperCase(java.util.Locale.US).replace("=", "").replace(" ", "");
        byte[] out = new byte[normalized.length() * 5 / 8 + 1];
        int buffer = 0, bits = 0, count = 0;
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int value = c >= 'A' && c <= 'Z' ? c - 'A'
                    : c >= '2' && c <= '7' ? c - '2' + 26 : -1;
            if (value < 0) throw new IllegalArgumentException("Invalid Base32 secret");
            buffer = (buffer << 5) | value; bits += 5;
            if (bits >= 8) { out[count++] = (byte) (buffer >> (bits - 8)); bits -= 8; }
        }
        return java.util.Arrays.copyOf(out, count);
    }
}
