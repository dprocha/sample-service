package com.mongodb.sample.utils;

import org.bson.types.ObjectId;

import java.math.BigInteger;

/**
 * Utility to convert MongoDB ObjectId to other Int 64 bytes.
 */
public class ObjectIdConverter {

    public static BigInteger generateDecimal128(ObjectId objectId) {
        return new BigInteger(1, objectId.toByteArray());
    }

    public static Long generateInt64(ObjectId oid) {
        byte[] b = oid.toByteArray(); // [0..11]
        // 1) Timestamp (seconds) is the first 4 bytes, big-endian
        long tsSec = ((b[0] & 0xFFL) << 24) | ((b[1] & 0xFFL) << 16)
                | ((b[2] & 0xFFL) << 8)  |  (b[3] & 0xFFL);

        // 2) Machine+process 5 bytes -> fold to 8-bit hash
        int nodeHash = foldTo8Bit(b, 4, 5);

        // 3) Counter is last 3 bytes
        long counter24 = ((b[9] & 0xFFL) << 16) | ((b[10] & 0xFFL) << 8) | (b[11] & 0xFFL);

        // Compose: ts<<32 | counter<<8 | nodeHash
        return (tsSec << 32) | (counter24 << 8) | (nodeHash & 0xFFL);
    }

    /** Optional: reverse back the timestamp (seconds) from the 64-bit id */
    public static long extractTimestampSeconds(long id64) {
        return (id64 >>> 32); // unsigned shift keeps the top 32 bits
    }

    /** Simple, fast 8-bit fold (no external deps). */
    private static int foldTo8Bit(byte[] a, int off, int len) {
        int h = 0;
        for (int i = 0; i < len; i++) {
            h = (h * 31 + (a[off + i] & 0xFF)) & 0xFF; // keep only low 8 bits
        }
        // Never return 0 if you want a non-zero tag (optional):
        return h == 0 ? 1 : h;
    }

}
