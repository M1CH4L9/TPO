/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;


import java.time.LocalDateTime;

public record LogEntry(
    String requestId,
    LocalDateTime serverLocalTime,
    String clientIp,
    String method,
    String endpoint,
    int status,
    int latencyMs,
    int bytes
) {}
