package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.io.HttpStream;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;
import java.io.IOException;

/**
 * Class for handling WebSocket HTTP body relay operations
 */
public class RelayWebSocket {

    private static final Logger logger = LoggerFactory.getLogger(RelayWebSocket.class);

    /**
     * Relay WebSocket frames between HttpStreamReader and HttpStreamWriter
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param in Input stream reader
     * @param out Output stream writer
     * @return HttpStream.Connection indicating whether connection should be closed
     * @throws IOException when I/O error occurs
     */
    public static HttpStream.Connection relay(String rid, HttpStream.Flow flow, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying WebSocket frames"
                    , rid
                    , flow
            );
        }
        
        while (true) {
            WebSocketFrame frame = readFrame(in);
            if (frame == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, WebSocket frame read returned null - ending relay", rid, flow);
                }
                break;
            }
            logFrame(rid, flow, "READ", frame);
            writeFrame(out, frame);
            logFrame(rid, flow, "WRITE", frame);
            if (frame.getOpcode() == 0x8) {
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, WebSocket close frame received - ending relay", rid, flow);
                }
                break; // close frame
            }
        }
        
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, WebSocket relay completed"
                    , rid
                    , flow
            );
        }
        
        // WebSocket relay completed, connection should be closed
        return HttpStream.Connection.CLOSE;
    }

    public static WebSocketFrame readFrame(HttpStreamReader in) throws IOException {
        int b = in.read();
        if (b == -1) {
            if (logger.isDebugEnabled()) {
                logger.debug("WebSocket readFrame: End of stream (first byte)");
            }
            return null;
        }
        int finAndOpcode = b;

        b = in.read();
        if (b == -1) {
            if (logger.isDebugEnabled()) {
                logger.debug("WebSocket readFrame: End of stream (second byte)");
            }
            return null;
        }
        int maskAndPayloadLen = b;

        int payloadLength = maskAndPayloadLen & 0x7F;
        byte[] extendedPayloadLengthBytes = null;

        if (payloadLength == 126) {
            extendedPayloadLengthBytes = new byte[2];
            if (in.read(extendedPayloadLengthBytes) != 2) {
                if (logger.isDebugEnabled()) {
                    logger.debug("WebSocket readFrame: Failed to read extended payload length (126)");
                }
                return null;
            }
            payloadLength = ((extendedPayloadLengthBytes[0] & 0xFF) << 8) | (extendedPayloadLengthBytes[1] & 0xFF);
        } else if (payloadLength == 127) {
            extendedPayloadLengthBytes = new byte[8];
            if (in.read(extendedPayloadLengthBytes) != 8) {
                if (logger.isDebugEnabled()) {
                    logger.debug("WebSocket readFrame: Failed to read extended payload length (127)");
                }
                return null;
            }
            long length = 0;
            for (int i = 0; i < 8; i++) {
                length = (length << 8) | (extendedPayloadLengthBytes[i] & 0xFF);
            }
            if (length > Integer.MAX_VALUE) {
                throw new IOException("Payload too large");
            }
            payloadLength = (int) length;
        }

        boolean masked = (maskAndPayloadLen & 0x80) != 0;
        byte[] maskingKey = null;
        if (masked) {
            maskingKey = new byte[4];
            if (in.read(maskingKey) != 4) {
                if (logger.isDebugEnabled()) {
                    logger.debug("WebSocket readFrame: Failed to read masking key");
                }
                return null;
            }
        }

        byte[] payload = new byte[payloadLength];
        int readTotal = 0;
        while (readTotal < payloadLength) {
            int r = in.read(payload, readTotal, payloadLength - readTotal);
            if (r == -1) {
                if (logger.isDebugEnabled()) {
                    logger.debug("WebSocket readFrame: End of stream while reading payload (read: {}, expected: {})", readTotal, payloadLength);
                }
                return null;
            }
            readTotal += r;
        }



        return new WebSocketFrame(finAndOpcode, maskAndPayloadLen, extendedPayloadLengthBytes, maskingKey, payload);
    }

    public static void writeFrame(HttpStreamWriter out, WebSocketFrame frame) throws IOException {
        out.write(frame.getFinAndOpcode());
        out.write(frame.getMaskAndPayloadLen());
        if (frame.getExtendedPayloadLengthBytes() != null) {
            out.write(frame.getExtendedPayloadLengthBytes());
        }
        if (frame.getMaskingKey() != null) {
            out.write(frame.getMaskingKey());
        }
        out.write(frame.getPayload());
        out.flush();
    }

    private static void logFrame(String rid, HttpStream.Flow flow, String direction, WebSocketFrame frame) {
        if (!logger.isDebugEnabled()) return;

        int opcode = frame.getOpcode();
        byte[] payload = frame.getPayload();
        int length = payload.length;

        logger.debug("{}, {}, WebSocket {} - Opcode: 0x{}, Payload Length: {}, Payload (hex): {}",
                rid,
                flow,
                direction,
                Integer.toHexString(opcode),
                length,
                toHexSummary(payload)
        );
    }

    private static String toHexSummary(byte[] data) {
        int limit = Math.min(data.length, 16);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("%02X", data[i]));
            if (i < limit - 1) sb.append(" ");
        }
        if (data.length > 16) sb.append(" ...");
        return sb.toString();
    }

    public static class WebSocketFrame {
        private final int finAndOpcode;
        private final int maskAndPayloadLen;
        private final byte[] extendedPayloadLengthBytes;
        private final byte[] maskingKey;
        private final byte[] payload;

        public WebSocketFrame(int finAndOpcode, int maskAndPayloadLen, byte[] extendedPayloadLengthBytes, byte[] maskingKey, byte[] payload) {
            this.finAndOpcode = finAndOpcode;
            this.maskAndPayloadLen = maskAndPayloadLen;
            this.extendedPayloadLengthBytes = extendedPayloadLengthBytes;
            this.maskingKey = maskingKey;
            this.payload = payload;
        }

        public int getFinAndOpcode() {
            return finAndOpcode;
        }

        public int getMaskAndPayloadLen() {
            return maskAndPayloadLen;
        }

        public byte[] getExtendedPayloadLengthBytes() {
            return extendedPayloadLengthBytes;
        }

        public byte[] getMaskingKey() {
            return maskingKey;
        }

        public byte[] getPayload() {
            return payload;
        }

        public int getOpcode() {
            return finAndOpcode & 0x0F;
        }
    }
}
