package cn.params.lab;

import cn.params.lab.parser.HttpRequest;
import cn.params.lab.parser.HttpResponse;

import java.util.concurrent.*;
import java.util.function.Consumer;

public class HttpClient {

    private volatile boolean isInit;
    private final HttpHandler httpHandler = new HttpHandler();

    public void get(HttpRequest request, Consumer<HttpResponse> callback) {
        if (!isInit) {
            throw new IllegalStateException("未初始化");
        }
        try {
            httpHandler.processHttp(request, callback);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Future<HttpResponse> get(HttpRequest request) {
        FutureTask<HttpResponse> future = new FutureTask<>(() -> {
            HttpResponse[] responses = new HttpResponse[1];
            CountDownLatch countDownLatch = new CountDownLatch(1);
            get(request, response -> {
                responses[0] = response;
                countDownLatch.countDown();
            });
            countDownLatch.await(5, TimeUnit.SECONDS);
            return responses[0];
        });
        Executors.newCachedThreadPool().submit(future);
        return future;
    }

    public void init() {
        if (isInit) {
            return;
        }

        httpHandler.init();
        Thread thread = new Thread(httpHandler);
        thread.setName("http-handler");
        thread.setDaemon(true);
        thread.start();
        isInit = true;
    }

}
