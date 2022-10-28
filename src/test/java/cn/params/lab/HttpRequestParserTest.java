package cn.params.lab;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class HttpRequestParserTest {

    @Test
    void testParseProtocol() {
        ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
        HttpRequest request = new HttpRequest();
        String url = "http://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());

        HttpRequestParser.parseProtocol(buffer, currentBuffer, request);
        Assertions.assertEquals("http", request.getProtocol());
    }

    @Test
    void testParseProtocol_Https() {
        ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
        HttpRequest request = new HttpRequest();
        String url = "https://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());

        HttpRequestParser.parseProtocol(buffer, currentBuffer, request);
        Assertions.assertEquals("https", request.getProtocol());
    }

    @Test
    void testParseHostAndPort() {
        ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
        HttpRequest request = new HttpRequest();
        String url = "http://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());

        HttpRequestParser.parseProtocol(buffer, currentBuffer, request);
        HttpRequestParser.parseHostAndPort(buffer, currentBuffer, request);
        Assertions.assertEquals("api.com", request.getHost());
        Assertions.assertEquals(80, request.getPort());
    }

    @Test
    void testParseHostAndPort_WithPort() {
        ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
        HttpRequest request = new HttpRequest();
        String url = "http://api.com:9527/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());

        HttpRequestParser.parseProtocol(buffer, currentBuffer, request);
        HttpRequestParser.parseHostAndPort(buffer, currentBuffer, request);
        Assertions.assertEquals("api.com", request.getHost());
        Assertions.assertEquals(9527, request.getPort());
    }

    @Test
    void testParsePath() {
        ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
        HttpRequest request = new HttpRequest();
        String url = "http://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());

        HttpRequestParser.parseProtocol(buffer, currentBuffer, request);
        HttpRequestParser.parseHostAndPort(buffer, currentBuffer, request);
        HttpRequestParser.parsePath(buffer, currentBuffer, request);
        Assertions.assertEquals("/api/content?fdfd=23", request.getPath());
    }

    @Test
    void testParsePath_EMPTY() {
        ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
        HttpRequest request = new HttpRequest();
        String url = "http://api.com";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());

        HttpRequestParser.parseProtocol(buffer, currentBuffer, request);
        HttpRequestParser.parseHostAndPort(buffer, currentBuffer, request);
        HttpRequestParser.parsePath(buffer, currentBuffer, request);
        Assertions.assertEquals("/", request.getPath());
    }

    @Test
    void testParse() {
        String url = "https://api.com/api/name?key=game";
        HttpRequest request = HttpRequestParser.parse(url);

        Assertions.assertEquals("https", request.getProtocol());
        Assertions.assertEquals("api.com", request.getHost());
        Assertions.assertEquals(443, request.getPort());
        Assertions.assertEquals("/api/name?key=game", request.getPath());
    }

}
