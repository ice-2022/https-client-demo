package cn.params.lab;

import lombok.Data;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.function.Consumer;

@Data
public class HttpHolder {
    private HttpRequest request;
    private HttpResponseParser responseParser;
    private SocketChannel channel;
    private Consumer<HttpResponse> callback;
    private SSLHolder sslHolder;
    private ByteBuffer writeBuffer;
    private ByteBuffer readBuffer;
}
