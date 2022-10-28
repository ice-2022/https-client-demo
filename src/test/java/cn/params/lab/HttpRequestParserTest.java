package cn.params.lab;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

public class HttpRequestParserTest {

    @Test
    void testParseProtocol() {
        String url = "http://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequestParser parser = new HttpRequestParser();
        parser.parseProtocol(buffer);
        Assertions.assertEquals("http", parser.getRequest().getProtocol());
    }

    @Test
    void testParseProtocol_Https() {
        String url = "https://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequestParser parser = new HttpRequestParser();
        parser.parseProtocol(buffer);
        Assertions.assertEquals("https", parser.getRequest().getProtocol());
    }

    @Test
    void testParseHostAndPort() {
        String url = "http://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequestParser parser = new HttpRequestParser();
        parser.parseProtocol(buffer);
        parser.parseHostAndPort(buffer);
        Assertions.assertEquals("api.com", parser.getRequest().getHost());
        Assertions.assertEquals(80, parser.getRequest().getPort());
    }

    @Test
    void testParseHostAndPort_WithPort() {
        String url = "http://api.com:9527/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequestParser parser = new HttpRequestParser();
        parser.parseProtocol(buffer);
        parser.parseHostAndPort(buffer);
        Assertions.assertEquals("api.com", parser.getRequest().getHost());
        Assertions.assertEquals(9527, parser.getRequest().getPort());
    }

    @Test
    void testParsePath() {
        String url = "http://api.com/api/content?fdfd=23";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequestParser parser = new HttpRequestParser();
        parser.parseProtocol(buffer);
        parser.parseHostAndPort(buffer);
        parser.parsePath(buffer);
        Assertions.assertEquals("/api/content?fdfd=23", parser.getRequest().getPath());
    }

    @Test
    void testParsePath_EMPTY() {
        String url = "http://api.com";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequestParser parser = new HttpRequestParser();
        parser.parseProtocol(buffer);
        parser.parseHostAndPort(buffer);
        parser.parsePath(buffer);
        Assertions.assertEquals("/", parser.getRequest().getPath());
    }

    @Test
    void testParse() {
        String url = "https://api.com/api/name?key=game";
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequestParser parser = new HttpRequestParser();
        parser.parseProtocol(buffer);
        parser.parseHostAndPort(buffer);
        parser.parsePath(buffer);
        Assertions.assertEquals("https", parser.getRequest().getProtocol());
        Assertions.assertEquals("api.com", parser.getRequest().getHost());
        Assertions.assertEquals(443, parser.getRequest().getPort());
        Assertions.assertEquals("/api/name?key=game", parser.getRequest().getPath());
    }

}
