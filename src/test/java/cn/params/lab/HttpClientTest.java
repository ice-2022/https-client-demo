package cn.params.lab;

import cn.params.lab.parser.HttpRequest;
import cn.params.lab.parser.HttpRequestParser;
import cn.params.lab.parser.HttpResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class HttpClientTest {

    @Test
    void testHttp() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        HttpClient client = new HttpClient();
        client.init();

        String url = "http://www.baidu.com";
        HttpRequest request = HttpRequestParser.parse(url);
        client.get(request, response -> {
            String content = new String(response.getBody());
            Assertions.assertTrue(content.startsWith("<!DOCTYPE html>"));
            countDownLatch.countDown();
        });
        countDownLatch.await(2, TimeUnit.SECONDS);
    }

    @Test
    void testHttps() throws Exception {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        HttpClient client = new HttpClient();
        client.init();

        String url = "https://www.baidu.com";
        HttpRequest request = HttpRequestParser.parse(url);
        client.get(request, response -> {
            String content = new String(response.getBody());
            Assertions.assertTrue(content.startsWith("<!DOCTYPE html>"));
            countDownLatch.countDown();
        });
        countDownLatch.await(2, TimeUnit.SECONDS);
    }

    @Test
    void testHttp_Future() throws Exception {
        HttpClient client = new HttpClient();
        client.init();

        String url = "http://www.baidu.com";
        HttpRequest request = HttpRequestParser.parse(url);
        Future<HttpResponse> future = client.get(request);
        String content = new String(future.get().getBody());
        Assertions.assertTrue(content.startsWith("<!DOCTYPE html>"));
    }

    @Test
    void testHttps_Future() throws Exception {
        HttpClient client = new HttpClient();
        client.init();

        String url = "https://www.baidu.com";
        HttpRequest request = HttpRequestParser.parse(url);
        Future<HttpResponse> future = client.get(request);
        String content = new String(future.get().getBody());
        Assertions.assertTrue(content.startsWith("<!DOCTYPE html>"));
    }


}
