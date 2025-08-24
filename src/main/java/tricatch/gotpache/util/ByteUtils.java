package tricatch.gotpache.util;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

public class ByteUtils {

    private static final int CRLFCRLF = 0x0D0A0D0A;
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static boolean startsWith(byte[] source, byte[] match) {
        return startsWith(source, 0, match);
    }

    public static boolean startsWith(byte[] source, int offset, byte[] match) {

        if (match.length > (source.length - offset)) {
            return false;
        }

        for (int i = 0; i < match.length; i++) {
            if (source[offset + i] != match[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(byte[] src, byte[] dst){

        return Arrays.equals(src, dst);
    }

    public static int indexOf(byte[] outerArray, byte[] smallerArray) {

        for(int i = 0; i < outerArray.length - smallerArray.length+1; ++i) {
            boolean found = true;
            for(int j = 0; j < smallerArray.length; ++j) {
                if (outerArray[i+j] != smallerArray[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return i;
        }
        return -1;
    }

    public static int indexOf(byte[] src, byte b){

        for(int i=0;i<src.length;i++){

            if( src[i] == b ) return i;
        }

        return -1;
    }

    public static int lastIndexOf(byte[] src, byte b){

        for(int i=src.length-1;i>=0;i--){

            if( src[i] == b ) return i;
        }

        return -1;
    }

    public static byte[] cut(byte[] src, int from, int to){

        byte[] tmp = new byte[ to-from ];

        System.arraycopy( src, from, tmp, 0, tmp.length );

        return tmp;
    }


    public static int indexOfCRLFCRLF(byte[] buf, int start, int end) {

        if (end - start < 4) return -1;

        int pos = Math.max(0, start - 3);

        for (int i = pos; i <= end - 4; i++) {

            if (buf[i] == '\r'
                    && buf[i + 1] == '\n'
                    && buf[i + 2] == '\r'
                    && buf[i + 3] == '\n')
            {
                return i;
            }
        }

        return -1;
    }


    public static int indexOfCRLF(byte[] buf, int start, int end) {

        for (int i = start; i < end - 1; i++) {

            if (buf[i] == '\r'
                    && buf[i + 1] == '\n')
            {
                return i;
            }
        }

        return -1;
    }

    public static String toHexPretty(byte[] bytes, int start, int end) {
        if (bytes == null || start < 0 || end > bytes.length || start >= end) {
            return "";
        }
        StringBuilder sb = new StringBuilder((end - start) * 3);
        for (int i = start; i < end; i++) {
            sb.append(HEX_ARRAY[(bytes[i] >> 4) & 0x0F]);
            sb.append(HEX_ARRAY[bytes[i] & 0x0F]);
            if (i < end - 1) sb.append(' ');
        }
        return sb.toString();
    }

    public static int parseHex(byte[] buf, int start, int end) {

        int result = 0;

        for (int i = start; i < end; i++) {
            int c = buf[i];
            if (c >= '0' && c <= '9') {
                result = (result << 4) + (c - '0');
            } else if (c >= 'a' && c <= 'f') {
                result = (result << 4) + (c - 'a' + 10);
            } else if (c >= 'A' && c <= 'F') {
                result = (result << 4) + (c - 'A' + 10);
            } else {
                throw new IllegalArgumentException("Invalid HEX: " + (char) c);
            }
        }

        return result;
    }
}
