package cn.params.lab;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class HttpResponseParserTest {

    @Test
    void testProcessProtocolAndStatus() {
        HttpResponseParser parser = new HttpResponseParser();
        String line = "HTTP/1.1 200\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(line.getBytes());
        parser.processProtocolAndStatus(buffer);
        HttpResponse response = parser.getResponse();
        Assertions.assertEquals("HTTP/1.1", response.getProtocol());
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void testProcessProtocolAndStatus_Break() {
        HttpResponseParser parser = new HttpResponseParser();
        String lineA = "HTTP/1.1";
        String lineB = " 200\r\n";
        parser.processProtocolAndStatus(ByteBuffer.wrap(lineA.getBytes()));
        parser.processProtocolAndStatus(ByteBuffer.wrap(lineB.getBytes()));
        HttpResponse response = parser.getResponse();
        Assertions.assertEquals("HTTP/1.1", response.getProtocol());
        Assertions.assertEquals(200, response.getStatus());
    }

    @Test
    void testProcessHeaders() {
        String line = "HTTP/1.1 200\r\n";
        String content = line + " Server: openresty\r\n" +
                "Date: Tue, 25 Oct 2022 02:14:56 GMT\r\n" +
                "Content-Type: application/json;charset=UTF-8\r\n" +
                "Content-Length: 20\r\n" +
                "Connection: keep-alive\r\n\r\n";
        ByteBuffer buffer = ByteBuffer.wrap(content.getBytes());
        HttpResponseParser parser = new HttpResponseParser();

        parser.processProtocolAndStatus(buffer);
        parser.processHeaders(buffer);

        HttpResponse response = parser.getResponse();
        Assertions.assertEquals("openresty", response.getHeaders().get("Server"));
        Assertions.assertEquals("Tue, 25 Oct 2022 02:14:56 GMT", response.getHeaders().get("Date"));
    }

    @Test
    void testProcessHeaders_Break() {
        String line = "HTTP/1.1 200\r\n";
        String content1 = " Server: openresty\r\n" +
                "Date: Tue, 25 Oct 2022 02:14:56 GMT\r\n";
        String content2 = "Content-Type: application/json;charset=UTF-8\r\n" +
                "Content-Length: 20\r\n" +
                "Connection: keep-alive\r\n\r\n";
        HttpResponseParser parser = new HttpResponseParser();
        ByteBuffer buffer1 = ByteBuffer.wrap(line.getBytes());
        ByteBuffer buffer2 = ByteBuffer.wrap(content1.getBytes());
        ByteBuffer buffer3 = ByteBuffer.wrap(content2.getBytes());

        parser.processProtocolAndStatus(buffer1);
        parser.processHeaders(buffer1);
        parser.processHeaders(buffer2);
        parser.processHeaders(buffer2);
        parser.processHeaders(buffer2);
        parser.processHeaders(buffer3);

        HttpResponse response = parser.getResponse();
        Assertions.assertEquals("openresty", response.getHeaders().get("Server"));
        Assertions.assertEquals("Tue, 25 Oct 2022 02:14:56 GMT", response.getHeaders().get("Date"));
    }

    @Test
    void testProcessBody() {
        String line = "HTTP/1.1 200\r\n";
        String content = line + " Server: openresty\r\n" +
                "Date: Tue, 25 Oct 2022 02:14:56 GMT\r\n" +
                "Content-Type: application/json;charset=UTF-8\r\n" +
                "Content-Length: 20\r\n" +
                "Connection: keep-alive\r\n\r\n";
        String all = content + "aaaaaaa_13.11.228.11////////";
        ByteBuffer buffer = ByteBuffer.wrap(all.getBytes());

        HttpResponseParser parser = new HttpResponseParser();
        parser.processProtocolAndStatus(buffer);
        parser.processHeaders(buffer);
        parser.processBody(buffer);

        HttpResponse response = parser.getResponse();
        Assertions.assertEquals("aaaaaaa_13.11.228.11", new String(response.getBody()));
    }

    @Test
    void testProcessBody_Break() {
        String line = "HTTP/1.1 200\r\n";
        String content1 = " Server: openresty\r\n" +
                "Date: Tue, 25 Oct 2022 02:14:56 GMT\r\n" +
                "Content-Type: application/json;charset=UTF-8\r\n" +
                "Content-Length: 20\r\n" +
                "Connection: keep-alive\r\n\r\n";
        String content2 = "aaaaaaa_13.11.228.11////////";
        ByteBuffer buffer1 = ByteBuffer.wrap(line.getBytes());
        ByteBuffer buffer2 = ByteBuffer.wrap(content1.getBytes());
        ByteBuffer buffer3 = ByteBuffer.wrap(content2.getBytes());

        HttpResponseParser parser = new HttpResponseParser();
        parser.processProtocolAndStatus(buffer1);
        parser.processHeaders(buffer2);
        parser.processBody(buffer2);
        parser.processBody(buffer3);
        parser.processBody(buffer3);

        HttpResponse response = parser.getResponse();
        Assertions.assertEquals("aaaaaaa_13.11.228.11", new String(response.getBody()));
    }
}
