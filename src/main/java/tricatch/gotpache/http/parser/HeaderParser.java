package tricatch.gotpache.http.parser;

import tricatch.gotpache.http.field.HeaderField;
import tricatch.gotpache.http.field.RequestField;
import tricatch.gotpache.http.field.ResponseField;
import tricatch.gotpache.util.ByteUtils;

import java.util.List;

public class HeaderParser {

    public static int parseRequestLine(byte[] buffer, final int start, final int end, RequestField toRequestField){

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
            toRequestField.methodStart = pos;
            toRequestField.methodEnd = firstSpace;

            //path
            toRequestField.pathStart = firstSpace + 1;
            toRequestField.pathEnd = secondSpace;

            // version
            toRequestField.versionStart = secondSpace + 1;
            toRequestField.versionEnd = lineEnd;

            return lineEnd + 2;

        } else {
            return -1;
        }

    }

    public static int parseStatusLine(byte[] buffer, final int start, final int end, ResponseField toResponseField) {

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
            toResponseField.versionStart = pos;
            toResponseField.versionEnd = firstSpace;

            if (secondSpace > firstSpace) {
                // Status-Code
                toResponseField.statusCodeStart = firstSpace + 1;
                toResponseField.statusCodeEnd = secondSpace;

                // Reason-Phrase
                toResponseField.reasonStart = secondSpace + 1;
                toResponseField.reasonEnd = lineEnd;
            } else {

                toResponseField.statusCodeStart = firstSpace + 1;
                toResponseField.statusCodeEnd = lineEnd;

                // No-Reason-Phrase
                toResponseField.reasonStart = -1;
                toResponseField.reasonEnd = -1;
            }

            return lineEnd + 2; // CRLF까지 포함

        } else {
            return -1; // 잘못된 상태 라인
        }
    }

    public static int parseHeader(byte[] buffer, final int start, final int end, List<HeaderField> toHeaderFiles){

        toHeaderFiles.clear();

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

            toHeaderFiles.add(new HeaderField(keyStart, keyEnd, valueStart, valueEnd));

            pos = lineEnd + 2;
        }


        return toHeaderFiles.size();
    }
}

