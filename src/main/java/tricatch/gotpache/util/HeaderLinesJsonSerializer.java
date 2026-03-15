package tricatch.gotpache.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import tricatch.gotpache.http.io.ByteBuffer;
import tricatch.gotpache.http.io.HeaderLines;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Jackson serializer for HeaderLines.
 * Converts ByteBuffer entries to readable strings in JSON output.
 */
public class HeaderLinesJsonSerializer extends JsonSerializer<HeaderLines> {

    @Override
    public void serialize(HeaderLines value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }
        gen.writeStartArray();
        for (ByteBuffer bb : value) {
            String line = new String(bb.getBuffer(), 0, bb.getLength(), StandardCharsets.UTF_8);
            gen.writeString(line);
        }
        gen.writeEndArray();
    }
}
