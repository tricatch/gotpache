package tricatch.gotpache.util;

import java.util.Arrays;

public class ByteUtils {

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
}
