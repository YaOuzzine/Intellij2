// demo 2/src/main/java/com/example/demo/Service/GeolocationService.java
package com.example.demo.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeolocationService {

    private static final Logger log = LoggerFactory.getLogger(GeolocationService.class);

    private final WebClient webClient;

    // Cache to avoid repeated lookups (simple in-memory cache)
    private final Map<String, GeolocationData> cache = new ConcurrentHashMap<>();

    public GeolocationService() {
        this.webClient = WebClient.builder()
                .baseUrl("http://ip-api.com/json")
                .build();
    }

    public GeolocationData getLocation(String ipAddress) {
        // Check cache first
        if (cache.containsKey(ipAddress)) {
            return cache.get(ipAddress);
        }

        // Skip private/local IPs
        if (isPrivateOrLocalIP(ipAddress)) {
            GeolocationData localData = new GeolocationData(ipAddress, "Local", "Local", "Private Network", "Local");
            cache.put(ipAddress, localData);
            return localData;
        }

        try {
            // Call free IP geolocation API
            IPApiResponse response = webClient
                    .get()
                    .uri("/" + ipAddress + "?fields=status,message,country,countryCode,region,regionName,city,query")
                    .retrieve()
                    .bodyToMono(IPApiResponse.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();

            GeolocationData result;
            if (response != null && "success".equals(response.status)) {
                result = new GeolocationData(
                        ipAddress,
                        response.country,
                        response.city,
                        response.regionName,
                        response.countryCode
                );
            } else {
                log.warn("Geolocation lookup failed for IP {}: {}", ipAddress,
                        response != null ? response.message : "No response");
                result = new GeolocationData(ipAddress, "Unknown", "Unknown", "Unknown", "XX");
            }

            // Cache the result
            cache.put(ipAddress, result);
            return result;

        } catch (Exception e) {
            log.warn("Geolocation lookup error for IP {}: {}", ipAddress, e.getMessage());
            GeolocationData errorResult = new GeolocationData(ipAddress, "Unknown", "Unknown", "Unknown", "XX");
            cache.put(ipAddress, errorResult);
            return errorResult;
        }
    }

    private boolean isPrivateOrLocalIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) return true;

        return ip.startsWith("127.") ||           // Loopback
                ip.startsWith("10.") ||            // Private Class A
                ip.startsWith("192.168.") ||       // Private Class C
                ip.startsWith("172.16.") ||        // Private Class B
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") ||
                ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") ||
                ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") ||
                ip.startsWith("172.31.") ||
                "::1".equals(ip) ||                // IPv6 loopback
                "localhost".equalsIgnoreCase(ip);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IPApiResponse {
        public String status;
        public String message;
        public String country;
        @JsonProperty("countryCode")
        public String countryCode;
        public String region;
        @JsonProperty("regionName")
        public String regionName;
        public String city;
        public String query;
    }

    public static class GeolocationData {
        private final String ipAddress;
        private final String country;
        private final String city;
        private final String region;
        private final String countryCode;

        public GeolocationData(String ipAddress, String country, String city, String region, String countryCode) {
            this.ipAddress = ipAddress;
            this.country = country != null ? country : "Unknown";
            this.city = city != null ? city : "Unknown";
            this.region = region != null ? region : "Unknown";
            this.countryCode = countryCode != null ? countryCode : "XX";
        }

        // Getters
        public String getIpAddress() { return ipAddress; }
        public String getCountry() { return country; }
        public String getCity() { return city; }
        public String getRegion() { return region; }
        public String getCountryCode() { return countryCode; }

        @Override
        public String toString() {
            return String.format("%s, %s, %s (%s)", city, region, country, countryCode);
        }
    }
}