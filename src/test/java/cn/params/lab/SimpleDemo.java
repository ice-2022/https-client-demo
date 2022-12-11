package cn.params.lab;

import javax.net.ssl.*;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.KeyStore;

public class SimpleDemo {

    private static final String SSL_PASSWORD = "abc";
    private SSLContext sslContext;
    private SSLEngine sslEngine;
    private ByteBuffer clientAppBuffer;
    private ByteBuffer clientNetBuffer;
    private ByteBuffer serverAppBuffer;
    private ByteBuffer serverNetBuffer;
    private volatile boolean handshakeFinished;
    private SocketChannel channel;
    private Selector selector;

    public void sendHttpAndPrint(String url) throws Exception {
        initSSLContext();
        initSSLEngin();
        System.out.println(url);
    }

    private void initSSLContext() throws Exception {

        // 不同的系统或者版本有所不同，如果路径不存在，可以在目录里找一下
        String javaHome = System.getProperty("java.home");
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(new FileInputStream(javaHome + "/lib/security/cacerts"), null);

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStore);
        keyStore.load(null, SSL_PASSWORD.toCharArray());
        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, SSL_PASSWORD.toCharArray());

        sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
    }

    private void initSSLEngin() throws Exception {
        SSLEngine sslEngine = sslContext.createSSLEngine();
        sslEngine.setUseClientMode(true);
        sslEngine.beginHandshake();
        SSLSession sslSession = sslEngine.getSession();
        int appBufferSize = sslSession.getApplicationBufferSize();
        int netBufferSize = sslSession.getPacketBufferSize();
        clientAppBuffer = ByteBuffer.allocate(appBufferSize);
        clientNetBuffer = ByteBuffer.allocate(netBufferSize);
        serverAppBuffer = ByteBuffer.allocate(appBufferSize);
        serverNetBuffer = ByteBuffer.allocate(netBufferSize);
    }

    private void initSocketChannel(String address, int port) throws Exception {
        selector = Selector.open();
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);
    }

    private void processHandshake(SSLEngineResult result) {
        if (handshakeFinished) {
            return;
        }

        SSLEngineResult.HandshakeStatus status;
        if (result == null) {
            status = sslEngine.getHandshakeStatus();
        } else {
            status = result.getHandshakeStatus();
        }

        switch (status) {
            case FINISHED, NOT_HANDSHAKING -> {
                handshakeFinished = true;
            }
            case NEED_UNWRAP -> {

            }
            case NEED_WRAP -> {
                // 需要发数据出去



            }
            case NEED_TASK -> {

            }
        }

    }


    public static void main(String[] args) throws Exception {
        new SimpleDemo().sendHttpAndPrint("https://www.baidu.com");
    }
}
