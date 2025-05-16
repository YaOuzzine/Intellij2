package com.example.demo.Db;

import org.springframework.http.server.reactive.ServerHttpRequest;
import java.net.InetSocketAddress;

public class IpUtils {

    public static String getClientIp(ServerHttpRequest request) {
        // More comprehensive header checking
        String[] headers = {
                "X-Forwarded-For", "X-Real-IP", "Proxy-Client-IP",
                "WL-Proxy-Client-IP", "HTTP_X_FORWARDED_FOR", "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP", "HTTP_CLIENT_IP", "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED", "HTTP_VIA", "REMOTE_ADDR"
        };

        // Try all common proxy headers first
        for (String header : headers) {
            String value = request.getHeaders().getFirst(header);
            if (value != null && !value.isEmpty() && !"unknown".equalsIgnoreCase(value)) {
                // Multiple values are comma-separated, take first one
                String ip = value.split(",")[0].trim();
                System.out.println("Found IP " + ip + " from header: " + header);
                return normalizeLoopback(ip);
            }
        }

        // Fallback to remote address
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            String ipFromRemote = remoteAddress.getAddress().getHostAddress();
            System.out.println("Extracted client IP from remoteAddress: " + ipFromRemote);
            return normalizeLoopback(ipFromRemote);
        }

        System.out.println("Unable to extract client IP - returning UNKNOWN");
        return "UNKNOWN";
    }

    private static String normalizeLoopback(String ip) {
        if ("::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            return "127.0.0.1";
        }
        if ("localhost".equalsIgnoreCase(ip)) {
            return "127.0.0.1";
        }
        return ip;
    }
}
