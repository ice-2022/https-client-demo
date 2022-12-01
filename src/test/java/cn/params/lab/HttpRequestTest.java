package cn.params.lab;

import cn.params.lab.parser.HttpRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpRequestTest {
    @Test
    void testString() {
        HttpRequest request = new HttpRequest();
        request.setHost("abc.com");
        request.setPort(80);
        request.setPath("/api/name");
        request.addHeader("Host", request.getHost());
        request.addHeader("User-Agent", "curl/7.82.0");
        request.addHeader("Accept", "*/*");
        String expect = """
                GET /api/name HTTP/1.1\r
                Accept: */*\r
                User-Agent: curl/7.82.0\r
                Host: abc.com\r
                \r
                """;
        Assertions.assertEquals(expect, request.string());
    }
}
