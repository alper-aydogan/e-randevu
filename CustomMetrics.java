import java.util.HashMap;
import java.util.Map;

public class CustomMetrics {

    private Map<String, Long> metrics;

    public CustomMetrics() {
        metrics = new HashMap<>();
    }

    // Record appointment metrics
    public void recordAppointmentMetric(String appointmentId, long duration) {
        metrics.put("appointment_" + appointmentId, duration);
    }

    // Record user metrics
    public void recordUserMetric(String userId, long duration) {
        metrics.put("user_" + userId, duration);
    }

    // Record API response time
    public void recordApiResponseTime(String apiName, long duration) {
        metrics.put("api_" + apiName, duration);
    }

    // Record authentication metrics
    public void recordAuthenticationMetric(String userId, boolean success) {
        metrics.put("auth_" + userId, success ? 1L : 0L);
    }

    // Record cache metrics
    public void recordCacheMetric(String cacheName, long duration) {
        metrics.put("cache_" + cacheName, duration);
    }

    // Record database query metrics
    public void recordDbQueryMetric(String queryName, long duration) {
        metrics.put("db_query_" + queryName, duration);
    }

    public Map<String, Long> getMetrics() {
        return metrics;
    }
}