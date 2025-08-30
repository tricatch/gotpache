package tricatch.gotpache.pass;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tricatch.gotpache.http.HTTP;
import tricatch.gotpache.http.io.HttpStream;
import tricatch.gotpache.http.io.ByteBuffer;
import tricatch.gotpache.http.io.HttpStreamReader;
import tricatch.gotpache.http.io.HttpStreamWriter;

import java.io.IOException;

/**
 * Class for handling chunked transfer encoding HTTP body relay operations
 */
public class RelayChunked {
    
    private static final Logger logger = LoggerFactory.getLogger(RelayChunked.class);
    
    /**
     * Relay chunked transfer encoding response body
     * @param rid Request ID for logging
     * @param flow Body stream flow direction (REQ/RES)
     * @param in Input stream reader
     * @param out Output stream writer
     * @return HttpStream.Connection indicating whether connection should be closed
     * @throws IOException when I/O error occurs
     */
    public static HttpStream.Connection relay(String rid, HttpStream.Flow flow, HttpStreamReader in, HttpStreamWriter out) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Relaying chunked body"
                    , rid
                    , flow
            );
        }

        ByteBuffer chunkSizeBuffer = new ByteBuffer(HTTP.CHUNK_SIZE_LINE_LENGTH);
        ByteBuffer chunkTrailerBuffer = new ByteBuffer(HTTP.CHUNK_SIZE_LINE_LENGTH);
        byte[] chunkBodyBuffer = new byte[HTTP.BODY_BUFFER_SIZE];
        
        while (true) {
            // Read chunk size line
            int bytesRead = in.readLine(chunkSizeBuffer, HTTP.CHUNK_SIZE_LINE_LENGTH);
            
            if (bytesRead == -1) {
                logger.warn("{}, {}, Unexpected end of stream while reading chunk size"
                        , rid
                        , flow
                );
                break;
            }
            
            String chunkSizeLine = new String(chunkSizeBuffer.getBuffer(), 0, chunkSizeBuffer.getLength());
            int semicolonIndex = chunkSizeLine.indexOf(';');
            if (semicolonIndex > 0) {
                chunkSizeLine = chunkSizeLine.substring(0, semicolonIndex);
            }
            
            int chunkSize;
            try {
                chunkSize = Integer.parseInt(chunkSizeLine.trim(), 16);
            } catch (NumberFormatException e) {
                logger.error("{}, {}, Invalid chunk size: {}"
                        , rid
                        , flow
                        , chunkSizeLine
                );
                break;
            }

            if (logger.isDebugEnabled()) {
                logger.debug("{}, {}, Chunk size: {}"
                        , rid
                        , flow
                        , chunkSize
                );
            }

            // Write chunk size to client
            out.write(chunkSizeBuffer.getBuffer(), 0, chunkSizeBuffer.getLength());
            out.write(HTTP.CRLF);
            
            if (chunkSize == 0) {

                for(;;){
                    bytesRead = in.readLine(chunkTrailerBuffer, HTTP.CHUNK_SIZE_LINE_LENGTH);
                    if( bytesRead < 0 ){
                        logger.warn("{}, {}, Unexpected end of stream while reading chunk trailer"
                                , rid
                                , flow
                        );
                        break;
                    }

                    if( bytesRead>0 ){
                        out.write(chunkTrailerBuffer.getBuffer(), 0, chunkTrailerBuffer.getLength());
                    }

                    out.write(HTTP.CRLF);
                    out.flush();

                    if( bytesRead == 0 ) break;
                }

                // End of chunked body - read and relay trailer headers
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, End of chunked body (chunk size 0) - reading trailer headers"
                            , rid
                            , flow
                    );
                }
                
                break;
            }
            
            // Relay chunk data
            int remainingBytes = chunkSize;
            while (remainingBytes > 0) {
                int bytesToRead = Math.min(chunkBodyBuffer.length, remainingBytes);
                bytesRead = in.read(chunkBodyBuffer, 0, bytesToRead);
                
                if (bytesRead == -1) {
                    logger.warn("{}, {}, Unexpected end of stream while reading chunk data"
                            , rid
                            , flow
                    );
                    break;
                }
                out.write(chunkBodyBuffer, 0, bytesRead);
                out.flush();

                remainingBytes -= bytesRead;
                
                if (logger.isDebugEnabled()) {
                    logger.debug("{}, {}, Relayed {} bytes of chunk, remaining: {}"
                            , rid
                            , flow
                            , bytesRead
                            , remainingBytes
                    );
                }
            }
            
            // Read and relay chunk end (CR-LF)
            int cr = in.read();
            int lf = in.read();
            if (cr == '\r' && lf == '\n') {
                out.write(HTTP.CRLF);
                out.flush();
            } else {
                logger.warn("{}, {}, Invalid chunk end marker", rid, flow);
                break;
            }
        }
        
        out.flush();

        if (logger.isDebugEnabled()) {
            logger.debug("{}, {}, Chunked body relay completed"
                    , rid
                    , flow
            );
        }
        
        return HttpStream.Connection.KEEP_ALIVE;
    }
}
