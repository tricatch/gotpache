package tricatch.gotpache.http.parser;

import tricatch.gotpache.http.HeaderField;
import tricatch.gotpache.http.RequestHeader;
import tricatch.gotpache.http.ResponseHeader;
import tricatch.gotpache.util.ByteUtils;

import java.util.List;

public class HeaderParser {

    public static int parseRequestLine(byte[] buffer, final int start, final int end, RequestHeader toRequestHeader){

        int lineEnd = ByteUtils.indexOfCRLF(buffer, start, end);

        if( lineEnd <= 0 ) return lineEnd;

        int pos = start;

        int firstSpace = -1, secondSpace = -1;

        for (int i = pos; i < lineEnd; i++) {
            if (buffer[i] == ' ') {
                if (firstSpace < 0) {
                    firstSpace = i;
                }
                else {
                    secondSpace = i;
                    break;
                }
            }
        }

        if (firstSpace > 0 && secondSpace > firstSpace) {

            //method
            toRequestHeader.methodStart = pos;
            toRequestHeader.methodEnd = firstSpace;

            //path
            toRequestHeader.pathStart = firstSpace + 1;
            toRequestHeader.pathEnd = secondSpace;

            // version
            toRequestHeader.versionStart = secondSpace + 1;
            toRequestHeader.versionEnd = lineEnd;

            return lineEnd + 2;

        } else {
            return -1;
        }

    }

    public static int parseResponseLine(byte[] buffer, final int start, final int end, ResponseHeader toResponseHeader) {

        int lineEnd = ByteUtils.indexOfCRLF(buffer, start, end);
        if (lineEnd <= 0) return lineEnd;

        int pos = start;
        int firstSpace = -1, secondSpace = -1;

        for (int i = pos; i < lineEnd; i++) {
            if (buffer[i] == ' ') {
                if (firstSpace < 0) {
                    firstSpace = i;
                } else {
                    secondSpace = i;
                    break;
                }
            }
        }

        if (firstSpace > 0) {
            // HTTP-Version
            toResponseHeader.versionStart = pos;
            toResponseHeader.versionEnd = firstSpace;

            if (secondSpace > firstSpace) {
                // Status-Code
                toResponseHeader.statusCodeStart = firstSpace + 1;
                toResponseHeader.statusCodeEnd = secondSpace;

                // Reason-Phrase
                toResponseHeader.reasonStart = secondSpace + 1;
                toResponseHeader.reasonEnd = lineEnd;
            } else {

                toResponseHeader.statusCodeStart = firstSpace + 1;
                toResponseHeader.statusCodeEnd = lineEnd;

                // No-Reason-Phrase
                toResponseHeader.reasonStart = -1;
                toResponseHeader.reasonEnd = -1;
            }

            return lineEnd + 2; // CRLF까지 포함

        } else {
            return -1; // 잘못된 상태 라인
        }
    }

    public static void parseHeader(byte[] buffer, final int start, final int end, List<HeaderField> toHeaderFileFields){

        toHeaderFileFields.clear();

        int pos = start;
        int headerEnd = end;

        while (pos < headerEnd) {

            if (buffer[pos] == '\r' && buffer[pos + 1] == '\n') break;

            int colon = -1;
            for (int i = pos; i < headerEnd; i++) {
                if (buffer[i] == ':') {
                    colon = i; break;
                }
            }

            if (colon < 0) break;

            int lineEnd = ByteUtils.indexOfCRLF( buffer, colon + 1, headerEnd);
            if (lineEnd < 0) break;

            int keyStart = pos;
            int keyEnd = colon;
            int valueStart = colon + 1;
            int valueEnd = lineEnd;

            while (valueStart < valueEnd && (buffer[valueStart] == ' ' || buffer[valueStart] == '\t')) valueStart++;
            while (valueEnd > valueStart && (buffer[valueEnd - 1] == ' ' || buffer[valueEnd - 1] == '\t')) valueEnd--;

            toHeaderFileFields.add(new HeaderField(keyStart, keyEnd, valueStart, valueEnd));

            pos = lineEnd + 2;
        }

    }

    public static HeaderField findHeader(List<HeaderField> headerFieldList, byte[] buffer, byte[] key) {

        for (HeaderField field : headerFieldList) {

            int keyLen = field.keyEnd - field.keyStart;
            if (keyLen != key.length) continue;

            boolean match = true;
            int p = field.keyStart;
            for (int i = 0; i < key.length; i++) {
                byte b1 = buffer[p + i];
                byte b2 = key[i];

                if (b1 >= 'A' && b1 <= 'Z') b1 += 32;
                if (b2 >= 'A' && b2 <= 'Z') b2 += 32;

                if (b1 != b2) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return field;
            }
        }
        return null;
    }

    public static String valueAsString(List<HeaderField> headerFieldList, byte[] buffer, byte[] key) {

        HeaderField headerField = findHeader(headerFieldList, buffer, key);

        if( headerField == null ) return null;

        return ByteUtils.toString(buffer, headerField.valueStart, headerField.valueEnd);
    }

    public static int valueAsInt(List<HeaderField> headerFieldList, byte[] buffer, byte[] key) {

        HeaderField headerField = findHeader(headerFieldList, buffer, key);

        if( headerField == null ) return -1;

        return ByteUtils.toInt(buffer, headerField.valueStart, headerField.valueEnd);
    }

}

