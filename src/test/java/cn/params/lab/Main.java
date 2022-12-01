package cn.params.lab;

import cn.params.lab.parser.HttpRequest;
import cn.params.lab.parser.HttpRequestParser;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        HttpClient client = new HttpClient();
        client.init();
        String url = "https://www.baidu.com";
        HttpRequest request = HttpRequestParser.parse(url);
        client.get(request, response -> {
            System.out.println(new String(response.getBody()));
        });
        // 测试时防止主线程结束
        Thread.sleep(1000);
    }
}
