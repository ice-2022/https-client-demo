package cn.params.lab;

import lombok.Data;

import javax.net.ssl.SSLEngine;
import java.nio.ByteBuffer;

@Data
public class SSLHolder {
    private SSLEngine sslEngine;
    private boolean finished;
    private ByteBuffer localNetBuffer;
    private ByteBuffer localAppBuffer;
    private ByteBuffer peerNetBuffer;
    private ByteBuffer peerAppBuffer;
}
