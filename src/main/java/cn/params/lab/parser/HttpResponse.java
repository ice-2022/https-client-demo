package cn.params.lab.parser;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpResponse {
    private String protocol;
    private int status;
    private final Map<String, String> headers = new HashMap<>();
    private byte[] body;
}
