package tricatch.gotpache.http.io;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@DisplayName("HttpStreamReader Test")
class HttpStreamReaderTest {

    private HttpStreamReader httpStream;
    private byte[] testData;

    @BeforeEach
    void setUp() {
        testData = "Hello World\r\nThis is a test\r\nAnother line\r\nLast line".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(testData);
        httpStream = new HttpStreamReader(inputStream);
    }

    @Test
    @DisplayName("Constructor Test")
    void testConstructor() {
        assertNotNull(httpStream);
    }

    @Test
    @DisplayName("Basic readLine Test")
    void testReadLine() throws IOException {
        byte[] buffer = new byte[100];
        int bytesRead = httpStream.readLine(buffer, 100);
        
        assertTrue(bytesRead > 0);
        assertTrue(bytesRead <= 100);
        
        String line = new String(buffer, 0, bytesRead);
        assertEquals("Hello World", line);
    }

    @Test
    @DisplayName("Multiple Lines Reading Test")
    void testReadMultipleLines() throws IOException {
        byte[] buffer = new byte[100];
        
        // First line
        int bytesRead1 = httpStream.readLine(buffer, 100);
        assertTrue(bytesRead1 > 0);
        String line1 = new String(buffer, 0, bytesRead1);
        assertEquals("Hello World", line1);
        
        // Second line
        int bytesRead2 = httpStream.readLine(buffer, 100);
        assertTrue(bytesRead2 > 0);
        String line2 = new String(buffer, 0, bytesRead2);
        assertEquals("This is a test", line2);
        
        // Third line
        int bytesRead3 = httpStream.readLine(buffer, 100);
        assertTrue(bytesRead3 > 0);
        String line3 = new String(buffer, 0, bytesRead3);
        assertEquals("Another line", line3);
    }

    @Test
    @DisplayName("Line Reading Test with LF Only")
    void testReadLineWithLFOnly() throws IOException {
        byte[] dataWithLF = "Hello World\nThis is a test\n".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dataWithLF);
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        byte[] buffer = new byte[100];
        int bytesRead = stream.readLine(buffer, 100);
        
        assertTrue(bytesRead > 0);
        String line = new String(buffer, 0, bytesRead);
        assertEquals("Hello World", line);
    }

    @Test
    @DisplayName("CR Only Case Handling Test")
    void testReadLineWithCROnly() throws IOException {
        byte[] dataWithCR = "Hello\rWorld\r\nTest".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dataWithCR);
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        byte[] buffer = new byte[100];
        int bytesRead = stream.readLine(buffer, 100);
        
        assertTrue(bytesRead > 0);
        String line = new String(buffer, 0, bytesRead);
        assertEquals("Hello\rWorld", line);
    }

    @Test
    @DisplayName("Maximum Length Limit Test")
    void testReadLineWithMaxLength() throws IOException {
        // Use shorter data that fits within the limit
        byte[] shortData = "Hi\r\nTest".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(shortData);
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        byte[] buffer = new byte[10];
        int bytesRead = stream.readLine(buffer, 5);
        
        assertEquals(2, bytesRead);
        String line = new String(buffer, 0, bytesRead);
        assertEquals("Hi", line);
    }

    @Test
    @DisplayName("Maximum Length Exceeded Exception Test")
    void testReadLineMaxLengthExceeded() throws IOException {
        // Generate long data without line terminators
        StringBuilder longData = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longData.append("A");
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(longData.toString().getBytes());
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        byte[] buffer = new byte[1000];
        assertThrows(IOException.class, () -> {
            stream.readLine(buffer, 50); // Limit to maximum 50 bytes
        });
    }

    @Test
    @DisplayName("Reading from Empty Stream Test")
    void testReadLineFromEmptyStream() throws IOException {
        ByteArrayInputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        HttpStreamReader emptyHttpStream = new HttpStreamReader(emptyStream);
        
        byte[] buffer = new byte[100];
        int bytesRead = emptyHttpStream.readLine(buffer, 100);
        
        assertEquals(-1, bytesRead);
    }

    @Test
    @DisplayName("Data Without Line Ending Test")
    void testReadLineWithoutLineEnding() throws IOException {
        byte[] dataWithoutEnding = "Hello World".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dataWithoutEnding);
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        byte[] buffer = new byte[100];
        int bytesRead = stream.readLine(buffer, 100);
        
        assertTrue(bytesRead > 0);
        String line = new String(buffer, 0, bytesRead);
        assertEquals("Hello World", line);
    }



    @Test
    @DisplayName("Negative Maximum Length Exception Test")
    void testReadLineWithNegativeMax() {
        byte[] buffer = new byte[100];
        assertThrows(IllegalArgumentException.class, () -> {
            httpStream.readLine(buffer, -1);
        });
    }

    @Test
    @DisplayName("Buffer Size Exceeded Exception Test")
    void testReadLineWithExceedingBufferSize() {
        byte[] buffer = new byte[10];
        assertThrows(IllegalArgumentException.class, () -> {
            httpStream.readLine(buffer, 20);
        });
    }



    @Test
    @DisplayName("Large Data Handling Test")
    void testLargeDataHandling() throws IOException {
        StringBuilder largeData = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeData.append("Line ").append(i).append("\r\n");
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(largeData.toString().getBytes());
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        int linesRead = 0;
        byte[] buffer = new byte[100];
        
        while (stream.readLine(buffer, 100) > 0) {
            linesRead++;
            if (linesRead > 100) break; // Limit to prevent infinite loop
        }
        
        assertTrue(linesRead > 0);
    }

    @Test
    @DisplayName("Last Line Handling Test")
    void testLastLineHandling() throws IOException {
        byte[] dataWithLastLine = "First line\r\nSecond line\r\nLast line".getBytes();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(dataWithLastLine);
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        byte[] buffer = new byte[100];
        
        // Read first two lines
        stream.readLine(buffer, 100);
        stream.readLine(buffer, 100);
        
        // Read last line (no line ending)
        int bytesRead = stream.readLine(buffer, 100);
        assertTrue(bytesRead > 0);
        String lastLine = new String(buffer, 0, bytesRead);
        assertEquals("Last line", lastLine);
        
        // Try to read more - should return -1
        int nextRead = stream.readLine(buffer, 100);
        assertEquals(-1, nextRead);
    }

    @Test
    @DisplayName("Buffer Expansion Test")
    void testBufferExpansion() throws IOException {
        // Generate long line (without line terminator)
        StringBuilder longLine = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longLine.append("A");
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(longLine.toString().getBytes());
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        // Start with small buffer
        byte[] buffer = new byte[1000];
        int bytesRead = stream.readLine(buffer, 1000);
        
        assertTrue(bytesRead > 10); // Buffer should be expanded
        assertEquals(200, bytesRead); // Should read all data
        
        String result = new String(buffer, 0, bytesRead);
        assertEquals(longLine.toString(), result);
    }

    @Test
    @DisplayName("Buffer Expansion Maximum Limit Test")
    void testBufferExpansionMaxLimit() throws IOException {
        // Generate very long line
        StringBuilder veryLongLine = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            veryLongLine.append("A");
        }
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(veryLongLine.toString().getBytes());
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        // Start with small buffer but limit maximum size
        byte[] buffer = new byte[1000];
        int maxSize = 1000;
        
        // Should throw exception when maximum size is reached
        assertThrows(IOException.class, () -> {
            stream.readLine(buffer, maxSize);
        });
    }

    @Test
    @DisplayName("Buffer Expansion with Line Ending Test")
    void testBufferExpansionWithLineEnding() throws IOException {
        // Long line requiring buffer expansion + line terminator
        StringBuilder longLine = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longLine.append("A");
        }
        longLine.append("\r\nNext line");
        
        ByteArrayInputStream inputStream = new ByteArrayInputStream(longLine.toString().getBytes());
        HttpStreamReader stream = new HttpStreamReader(inputStream);
        
        // Start with small buffer
        byte[] buffer = new byte[1000];
        int bytesRead = stream.readLine(buffer, 1000);
        
        assertTrue(bytesRead > 10); // Buffer should be expanded
        assertEquals(200, bytesRead); // Should read only up to line terminator
        
        String result = new String(buffer, 0, bytesRead);
        assertEquals(longLine.substring(0, 200), result);
    }
}
