package cn.params.lab;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

public class HttpHandler implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(HttpHandler.class);

    private SSLContext sslContext;
    private Selector selector;
    private final Queue<Runnable> eventQueue = new LinkedList<>();
    private static final String SSL_PASSWORD = "abc";

    public void init() {
        prepareSSL();
        prepareSelector();
        LOG.debug("HttpHandler - 初始化完成");
    }

    void prepareSSL() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            String javaHome = System.getProperty("java.home");
            keyStore.load(new FileInputStream(javaHome + "/lib/security/cacerts"), null);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(keyStore);
            keyStore.load(null, SSL_PASSWORD.toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, SSL_PASSWORD.toCharArray());
            SSLContext mySslContext = SSLContext.getInstance("TLSv1.2");
            mySslContext.init(null, tmf.getTrustManagers(), null);
            sslContext = mySslContext;
        } catch (Exception e) {
            throw new RuntimeException("初始SSL失败", e);
        }
    }

    void prepareSelector() {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void processHttp(HttpRequest request, Consumer<HttpResponse> callback) throws Exception {
        SocketChannel channel = SocketChannel.open();
        channel.configureBlocking(false);
        SelectionKey key = channel.register(selector, SelectionKey.OP_CONNECT);

        // 初始化HttpHolder
        HttpHolder httpHolder = new HttpHolder();
        httpHolder.setChannel(channel);
        httpHolder.setRequest(request);
        httpHolder.setResponseParser(new HttpResponseParser());
        httpHolder.setCallback(callback);

        // 是否启用https协议
        if (request.getProtocol().equals("https")) {
            SSLHolder sslHolder = new SSLHolder();
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(true);
            sslEngine.beginHandshake();
            sslHolder.setSslEngine(sslEngine);

            SSLSession sslSession = sslEngine.getSession();
            int appBufferSize = sslSession.getApplicationBufferSize();
            int netBufferSize = sslSession.getPacketBufferSize();
            sslHolder.setLocalAppBuffer(ByteBuffer.allocate(appBufferSize));
            sslHolder.setLocalNetBuffer(ByteBuffer.allocate(netBufferSize));
            sslHolder.setPeerAppBuffer(ByteBuffer.allocate(appBufferSize));
            sslHolder.setPeerNetBuffer(ByteBuffer.allocate(netBufferSize));

            httpHolder.setWriteBuffer(ByteBuffer.allocate(appBufferSize));
            httpHolder.setReadBuffer(ByteBuffer.allocate(appBufferSize));
            httpHolder.setSslHolder(sslHolder);
        } else {
            httpHolder.setWriteBuffer(ByteBuffer.allocate(10240));
            httpHolder.setReadBuffer(ByteBuffer.allocate(10240));
        }

        // 连接远程服务
        key.attach(httpHolder);
        channel.connect(new InetSocketAddress(request.getHost(), request.getPort()));
        selector.wakeup();
    }

    @Override
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> selectionKeys = selector.selectedKeys().iterator();
                while (selectionKeys.hasNext()) {
                    SelectionKey key = selectionKeys.next();
                    selectionKeys.remove();

                    // 处理网络事件
                    if (key.isConnectable()) {
                        processConnect(key);
                    } else if (key.isReadable()) {
                        processRead(key);
                    }
                }

                // 有需要的情况下，处理自定事件
                while (!eventQueue.isEmpty()) {
                    Runnable task = eventQueue.poll();
                    task.run();
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processConnect(SelectionKey key) throws Exception {
        HttpHolder httpHolder = (HttpHolder) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        LOG.debug("-- OP_CONNECT:" + channel);
        key.interestOps(SelectionKey.OP_READ);
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }

        if (httpHolder.getSslHolder() != null) {
            doHandshake(httpHolder);
        } else {
            // 普通http请求
            eventQueue.offer(() -> {
                ByteBuffer requestBuffer = ByteBuffer.wrap(httpHolder.getRequest().string().getBytes());
                try {
                    httpHolder.getChannel().write(requestBuffer);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            selector.wakeup();
        }

    }

    private void processRead(SelectionKey key) {
        HttpHolder holder = (HttpHolder) key.attachment();
        SocketChannel channel = (SocketChannel) key.channel();
        LOG.debug("-- OP_READ:" + channel);

        try {
            if (holder.getSslHolder() != null && !holder.getSslHolder().isFinished()) {
                doHandshake(holder);
                return;
            }

            HttpResponseParser responseParser = holder.getResponseParser();
            ByteBuffer readBuffer = holder.getReadBuffer();

            // 处理https解密
            if (holder.getSslHolder() != null) {
                SSLHolder sslHolder = holder.getSslHolder();
                ByteBuffer peerNetBuffer = sslHolder.getPeerNetBuffer();
                while (channel.read(peerNetBuffer) > 0) {
                    SSLEngineResult result;
                    peerNetBuffer.flip();
                    do {
                        result = sslHolder.getSslEngine().unwrap(peerNetBuffer, readBuffer);
                    } while (result.getStatus() == SSLEngineResult.Status.OK);
                    readBuffer.flip();
                    responseParser.parse(readBuffer);
                    if (responseParser.isComplete()) {
                        channel.close();
                        holder.getCallback().accept(responseParser.getResponse());
                        return;
                    }
                    readBuffer.clear();
                    peerNetBuffer.compact();
                }

            } else {
                while (channel.read(readBuffer) > 0) {
                    readBuffer.flip();
                    responseParser.parse(readBuffer);
                    if (responseParser.isComplete()) {
                        channel.close();
                        holder.getCallback().accept(responseParser.getResponse());
                        return;
                    }
                    readBuffer.compact();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void doHandshake(HttpHolder holder) throws Exception {
        if (holder.getSslHolder().isFinished()) {
            return;
        }

        SSLHolder sslHolder = holder.getSslHolder();
        SSLEngine sslEngine = sslHolder.getSslEngine();
        SSLEngineResult.HandshakeStatus status = sslEngine.getHandshakeStatus();
        switch (status) {
            case NEED_WRAP -> {
                SocketChannel channel = holder.getChannel();
                LOG.debug("@@ NEED_WRAP:\t" + channel);
                ByteBuffer netBuffer = sslHolder.getLocalNetBuffer();
                ByteBuffer appBuffer = sslHolder.getLocalAppBuffer();
                netBuffer.clear();
                sslEngine.wrap(appBuffer, netBuffer);
                netBuffer.flip();
                LOG.debug("netBuffer size:" + netBuffer.remaining());
                channel.write(netBuffer);
            }
            case NEED_UNWRAP -> {
                SocketChannel channel = holder.getChannel();
                LOG.debug("@@ NEED_UNWRAP:\t" + channel);
                ByteBuffer netBuffer = sslHolder.getPeerNetBuffer();
                ByteBuffer appBuffer = sslHolder.getPeerAppBuffer();

                int count = channel.read(netBuffer);
                LOG.debug("channel.read:" + count);

                // 本来这里有个循环的设想，但是有可能是数据没读完，所以每次还是要read多次
                netBuffer.flip();
                LOG.debug("netBuffer.remaining:" + netBuffer.remaining());
                sslEngine.unwrap(netBuffer, appBuffer);
                netBuffer.compact(); // 要保存之前的信息

            }
            case NEED_TASK -> {
                Runnable runnable;
                long time = System.currentTimeMillis();
                while ((runnable = sslEngine.getDelegatedTask()) != null) {
                    runnable.run();
                }
                LOG.debug("@@ NEED_TASK:\t" + (System.currentTimeMillis() - time));
            }
            case FINISHED -> {
                sslHolder.setFinished(true);
                LOG.debug("@@ FINISHED:\t");
            }
            case NOT_HANDSHAKING -> {
                sslHolder.setFinished(true);
                sslHolder.getLocalAppBuffer().clear();
                sslHolder.getLocalNetBuffer().clear();
                sslHolder.getPeerAppBuffer().clear();
                sslHolder.getPeerNetBuffer().clear();
                ByteBuffer netBuffer = sslHolder.getLocalNetBuffer();

                ByteBuffer requestBuffer = ByteBuffer.wrap(holder.getRequest().string().getBytes());
                sslEngine.wrap(requestBuffer, netBuffer);
                netBuffer.flip();
                int count = holder.getChannel().write(netBuffer);
                LOG.debug("@@ NOT_HANDSHAKING:\t" + count);
            }
        }

        // 是否需要由read事件驱动，如果已经没有可用的缓存数据，且要进行解操作时，等待
        ByteBuffer peerNetBuffer = sslHolder.getPeerNetBuffer();
        peerNetBuffer.flip();
        boolean hasRemaining = peerNetBuffer.hasRemaining();
        peerNetBuffer.compact();
        if (sslEngine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_UNWRAP && !hasRemaining) {
            return;
        }
        eventQueue.offer(() -> {
            try {
                doHandshake(holder);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        selector.wakeup();
    }
}
