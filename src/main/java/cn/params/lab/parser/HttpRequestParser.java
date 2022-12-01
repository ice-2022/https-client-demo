package cn.params.lab.parser;

import java.nio.ByteBuffer;

public class HttpRequestParser {

    public static HttpRequest parse(String url) {
        ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
        ByteBuffer buffer = ByteBuffer.wrap(url.getBytes());
        HttpRequest request = new HttpRequest();

        // 解析scheme
        parseProtocol(buffer, currentBuffer, request);

        // 解析 host port
        parseHostAndPort(buffer, currentBuffer, request);

        // 解析 path
        parsePath(buffer, currentBuffer, request);

        return request;
    }


    static void parseProtocol(ByteBuffer buffer, ByteBuffer currentBuffer, HttpRequest request) {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            if (b == ':') {
                currentBuffer.flip();
                byte[] bytes = new byte[currentBuffer.remaining()];
                currentBuffer.get(bytes);
                request.setProtocol(new String(bytes));
                break;
            }
            currentBuffer.put(b);
        }
    }

    static void parseHostAndPort(ByteBuffer buffer, ByteBuffer currentBuffer, HttpRequest request) {
        currentBuffer.clear();

        // 跳过2个//
        buffer.get();
        buffer.get();

        // 如果有的话，记录自定义端口位置
        int indexOfPortEnd = -1;
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            currentBuffer.put(b);
            if (b == ':') {
                indexOfPortEnd = currentBuffer.position() - 1;
            }
            if (b == '/' || !buffer.hasRemaining()) {
                currentBuffer.flip();
                if (indexOfPortEnd > 0) {
                    byte[] bytes = new byte[indexOfPortEnd];
                    currentBuffer.get(bytes);
                    request.setHost(new String(bytes));
                    currentBuffer.get(); // 跳过 ":"
                    if (b == '/') {
                        bytes = new byte[currentBuffer.remaining() - 1];
                    } else {
                        bytes = new byte[currentBuffer.remaining()];
                    }
                    currentBuffer.get(bytes);
                    request.setPort(Integer.parseInt(new String(bytes)));
                } else {
                    byte[] bytes;
                    if (b == '/') {
                        bytes = new byte[currentBuffer.remaining() - 1];
                    } else {
                        bytes = new byte[currentBuffer.remaining()];
                    }
                    currentBuffer.get(bytes);
                    currentBuffer.flip();
                    request.setHost(new String(bytes));
                    if (request.getProtocol().equals("http")) {
                        request.setPort(80);
                    } else {
                        request.setPort(443);
                    }
                }
                break;
            }
        }
    }

    static void parsePath(ByteBuffer buffer, ByteBuffer currentBuffer, HttpRequest request) {
        currentBuffer.clear();
        if (buffer.hasRemaining()) {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            request.setPath("/" + new String(bytes));
        } else {
            request.setPath("/");
        }
    }
}
