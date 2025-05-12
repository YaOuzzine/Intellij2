package com.example.demo.Db;

import org.springframework.http.server.reactive.ServerHttpRequest;
import java.net.InetSocketAddress;

public class IpUtils {

    public static String getClientIp(ServerHttpRequest request) {
        // Check for X-Forwarded-For header first.
        String forwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            String ipFromHeader = forwardedFor.split(",")[0].trim();
            System.out.println("Extracted client IP from X-Forwarded-For: " + ipFromHeader);
            return normalizeLoopback(ipFromHeader);
        }

        // Fallback to remote address.
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        if (remoteAddress != null) {
            String ipFromRemote = remoteAddress.getAddress().getHostAddress();
            System.out.println("Extracted client IP from remoteAddress: " + ipFromRemote);
            return normalizeLoopback(ipFromRemote);
        }

        System.out.println("Extracted client IP: UNKNOWN");
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
