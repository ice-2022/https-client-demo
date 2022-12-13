package cn.params.lab;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;
import java.util.Iterator;

public class SimpleDemo {

    private static final String SSL_PASSWORD = "abc";
    private SSLContext sslContext;
    private SSLEngine sslEngine;
    private ByteBuffer clientAppBuffer;
    private ByteBuffer clientNetBuffer;
    private ByteBuffer serverAppBuffer;
    private ByteBuffer serverNetBuffer;
    private volatile boolean handshakeFinished;

    public void sendHttpsAndPrint(String host) throws Exception {
        initSSLContext();
        initSSLEngin();
        startSocketChannel(host, 443);
    }

    private void initSSLContext() throws Exception {
        // 不同的系统或者版本有所不同，如果路径不存在，可以在目录里找一下
        String javaHome = System.getProperty("java.home");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(javaHome + "/lib/security/cacerts"), null);

        // 用来确认证书是否可以被信任的，SSL_PASSWORD可以随意写一个增加安全性
        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        keyStore.load(null, SSL_PASSWORD.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, SSL_PASSWORD.toCharArray());

        // 定好ssl版本
        sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    }

    private void initSSLEngin() throws Exception {
        sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        sslEngine.beginHandshake();

        // 取当前最大包的size，定义自己的ByteBuffer接发数据
        SSLSession sslSession = sslEngine.getSession();
        int appBufferSize = sslSession.getApplicationBufferSize();
        int netBufferSize = sslSession.getPacketBufferSize();
        clientAppBuffer = ByteBuffer.allocate(appBufferSize);
        clientNetBuffer = ByteBuffer.allocate(netBufferSize);
        serverAppBuffer = ByteBuffer.allocate(appBufferSize);
        serverNetBuffer = ByteBuffer.allocate(netBufferSize);
    }

    private void startSocketChannel(String host, int port) throws Exception {
        Selector selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_CONNECT);
        channel.connect(new InetSocketAddress(host, port));

        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();
                    SocketChannel socketChannel = (SocketChannel) key.channel();
                    if (key.isConnectable()) {
                        if (socketChannel.isConnectionPending()) {
                            socketChannel.finishConnect();
                        }

                        // 完成握手后才订阅读事件和发送数据，相当于这个地方阻塞。
                        // 其实也可以做成网络事件驱动的，会复杂一些，这里用简单粗暴的方式阻塞住。
                        processHandshake(socketChannel);

                        key.interestOps(SelectionKey.OP_READ);
                        String sendContent = "GET / HTTP/1.1\r\n";
                        sendContent += "Accept: */*\r\n";
                        sendContent += "User-Agent: curl/7.84.0\r\n";
                        sendContent += "Host: " + host + "\r\n\r\n";
                        clientAppBuffer.put(sendContent.getBytes());
                        clientAppBuffer.flip();
                        sslEngine.wrap(clientAppBuffer, clientNetBuffer);
                        clientNetBuffer.flip();
                        socketChannel.write(clientNetBuffer);
                        System.out.println("finish connect");
                    } else if (key.isReadable()) {
                        // 读取数据并解密
                        SSLEngineResult result;
                        socketChannel.read(serverNetBuffer);
                        serverNetBuffer.flip();
                        do {
                            result = sslEngine.unwrap(serverNetBuffer, serverAppBuffer);
                        } while (result.getStatus() == SSLEngineResult.Status.OK);
                        serverAppBuffer.flip();
                        byte[] contentBytes = new byte[serverAppBuffer.remaining()];
                        serverAppBuffer.get(contentBytes);
                        System.out.println(new String(contentBytes));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processHandshake(SocketChannel channel) throws Exception {
        while (!handshakeFinished) {
            SSLEngineResult.HandshakeStatus status;
            status = sslEngine.getHandshakeStatus();

            switch (status) {
                case FINISHED, NOT_HANDSHAKING -> {
                    System.out.println("FINISHED, NOT_HANDSHAKING, 完成握手");
                    handshakeFinished = true;
                    clientNetBuffer.clear();
                    clientAppBuffer.clear();
                    serverNetBuffer.clear();
                    serverAppBuffer.clear();
                }
                case NEED_UNWRAP -> {
                    channel.read(serverNetBuffer);
                    serverNetBuffer.flip();
                    System.out.println(serverNetBuffer.remaining());
                    sslEngine.unwrap(serverNetBuffer, serverAppBuffer);
                    // 很可能在NEED_TASK执行后，还会继续使用未解完的数据，所以要compact压缩保留，下次使用
                    serverNetBuffer.compact();
                    System.out.println("NEED_UNWRAP, 数据解析");
                }
                case NEED_WRAP -> {
                    // 需要发数据出去，这里的clientAppBuffer有没有数据都无所谓，wrap方法会特殊处理握手阶段
                    clientNetBuffer.clear();
                    sslEngine.wrap(clientAppBuffer, clientNetBuffer);
                    clientNetBuffer.flip();
                    channel.write(clientNetBuffer);
                    System.out.println("NEED_WRAP, 数据发出");
                }
                case NEED_TASK -> {
                    // 需要的话可以异步
                    sslEngine.getDelegatedTask().run();
                    System.out.println("NEED_TASK, 执行任务");
                }
            }
        }
    }


    public static void main(String[] args) throws Exception {
        new SimpleDemo().sendHttpsAndPrint("www.baidu.com");
    }
}
