package com.example.demo.Controller;

import com.example.demo.Filter.RequestCountFilter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricsController {

    // Endpoint for total request counts.
    @GetMapping("/api/metrics/requests")
    public RequestCountResponse getTotalRequests() {
        return new RequestCountResponse(
                RequestCountFilter.getTotalRequestCount(),
                RequestCountFilter.getTotalRejectedCount()
        );
    }

    // DTO for total request counts.
    public static class RequestCountResponse {
        private final long requestCount;
        private final long rejectedCount;

        public RequestCountResponse(long requestCount, long rejectedCount) {
            this.requestCount = requestCount;
            this.rejectedCount = rejectedCount;
        }

        public long getRequestCount() {
            return requestCount;
        }

        public long getRejectedCount() {
            return rejectedCount;
        }
    }

    // Endpoint to expose per-minute metrics.
    @GetMapping("/api/metrics/minutely")
    public MinuteMetricsResponse getMinutelyMetrics() {
        RequestCountFilter.MinuteMetrics metrics = RequestCountFilter.getMinuteMetrics();
        return new MinuteMetricsResponse(
                metrics.getRequestsCurrentMinute(),
                metrics.getRequestsPreviousMinute(),
                metrics.getRejectedCurrentMinute(),
                metrics.getRejectedPreviousMinute()
        );
    }

    // DTO for per-minute metrics.
    public static class MinuteMetricsResponse {
        private final long requestsCurrentMinute;
        private final long requestsPreviousMinute;
        private final long rejectedCurrentMinute;
        private final long rejectedPreviousMinute;

        public MinuteMetricsResponse(long requestsCurrentMinute, long requestsPreviousMinute,
                                     long rejectedCurrentMinute, long rejectedPreviousMinute) {
            this.requestsCurrentMinute = requestsCurrentMinute;
            this.requestsPreviousMinute = requestsPreviousMinute;
            this.rejectedCurrentMinute = rejectedCurrentMinute;
            this.rejectedPreviousMinute = rejectedPreviousMinute;
        }

        public long getRequestsCurrentMinute() {
            return requestsCurrentMinute;
        }

        public long getRequestsPreviousMinute() {
            return requestsPreviousMinute;
        }

        public long getRejectedCurrentMinute() {
            return rejectedCurrentMinute;
        }

        public long getRejectedPreviousMinute() {
            return rejectedPreviousMinute;
        }
    }
}
