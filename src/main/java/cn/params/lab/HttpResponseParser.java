package cn.params.lab;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class HttpResponseParser {
    private final ByteBuffer currentBuffer = ByteBuffer.allocate(1024);
    private final HttpResponse response = new HttpResponse();

    private boolean protocolState;
    private boolean statusState;

    private boolean headerState;
    private int splitIndex = -1;
    private final Map<String, String> headers = new HashMap<>();

    private boolean bodyState;
    private ByteBuffer bodyBuffer;

    public boolean isComplete() {
        return bodyState;
    }

    public HttpResponse getResponse() {
        return response;
    }

    public void parse(ByteBuffer buffer) {
        // 解析http协议
        processProtocolAndStatus(buffer);

        // 解析headers
        if (!statusState) {
            return;
        }
        processHeaders(buffer);

        // body解析
        if (!headerState) {
            return;
        }
        processBody(buffer);
    }

    public Map<String, String> getHeaders() {
        checkState(headerState);
        return headers;
    }

    private void checkState() {
        if (!bodyState) {
            throw new IllegalStateException("协议未完成解析");
        }
    }

    private void checkState(boolean state) {
        if (!state) {
            throw new IllegalStateException("协议未完成解析");
        }
    }

    /**
     * 解析协议和状态
     *
     * @param buffer 输入数据
     */
    void processProtocolAndStatus(ByteBuffer buffer) {
        if (!protocolState) {
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                // 找到第一个空格 格式:  HTTP/1.1 200
                if (b == ' ') {
                    protocolState = true;
                    currentBuffer.flip();
                    byte[] bytes = new byte[currentBuffer.remaining()];
                    currentBuffer.get(bytes);
                    response.setProtocol(new String(bytes));
                    currentBuffer.clear();
                    break;
                }
                currentBuffer.put(b);
            }
        }
        if (!statusState) {
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                // 找到第一个回车 格式:  HTTP/1.1 200
                if (b == '\n') {
                    statusState = true;
                    currentBuffer.flip();
                    byte[] bytes = new byte[currentBuffer.remaining()];
                    currentBuffer.get(bytes);
                    response.setStatus(Integer.parseInt(new String(bytes, 0, 3)));
                    currentBuffer.clear();
                    break;
                }
                currentBuffer.put(b);
            }
        }
    }

    /**
     * 解析http响应头
     *
     * @param buffer 输入数据
     */
    void processHeaders(ByteBuffer buffer) {
        if (!headerState) {
            while (buffer.hasRemaining()) {
                byte b = buffer.get();
                currentBuffer.put(b);
                // 只取到首个冒号
                if (b == ':' && splitIndex == -1) {
                    splitIndex = currentBuffer.position() - 1;
                }
                if (b == '\n') {
                    // 如果缓存里没有冒号，因为每个header都有冒号，所以说明header解析结束了
                    if (splitIndex == -1) {
                        headerState = true;
                        break;
                    }
                    currentBuffer.flip();
                    byte[] nameBytes = new byte[splitIndex];
                    currentBuffer.get(nameBytes);
                    String name = new String(nameBytes).trim();
                    currentBuffer.get(); // 跳过冒号
                    byte[] valueBytes = new byte[currentBuffer.remaining()];
                    currentBuffer.get(valueBytes);
                    String value = new String(valueBytes).trim();
                    response.getHeaders().put(name, value);
                    currentBuffer.clear();
                    splitIndex = -1;
                }
            }
        }
    }

    void processBody(ByteBuffer buffer) {
        if (bodyBuffer == null) {
            int contentLength = Integer.parseInt(response.getHeaders().get("Content-Length"));
            bodyBuffer = ByteBuffer.allocate(contentLength);
        }

        if (!bodyState) {
            if (buffer.remaining() >= bodyBuffer.remaining()) {
                byte[] bytes = new byte[bodyBuffer.remaining()];
                buffer.get(bytes);
                bodyBuffer.put(bytes);
                bodyBuffer.flip();
                response.setBody(bodyBuffer.array());
                bodyState = true;
            } else {
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                bodyBuffer.put(bytes);
            }
        }
    }
}
