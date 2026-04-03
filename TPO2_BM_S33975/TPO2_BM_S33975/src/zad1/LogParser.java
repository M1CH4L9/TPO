/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;
import java.time.LocalDateTime; 
import java.util.regex.Pattern;


import java.util.Optional;

public class LogParser {

  public Optional<LogEntry> parseLine(String line) {

  //sprawdzenie ipv4
  private static final Pattern IPV4_PATTERN = 
      Pattern.compile("^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

  public Optional<LogEntry> parseLine(String line) {
    if (line == null || line.isBlank()) {
        return Optional.empty();
    }


    //dzielimy po rurce (-1 zeby nie uciąć na końcu)
    String[] parts = line.split("\\|", -1);
    if (parts.length != 8) {
        return Optional.empty();
    }


    try {
      String reqId = parts[0].trim();
      if (reqId.isEmpty()) return Optional.empty();

      //parsujemy date
      LocalDateTime time = LocalDateTime.parse(parts[1].trim());
      
      String ip = parts[2].trim();
      if (!IPV4_PATTERN.matcher(ip).matches()) return Optional.empty();

      String method = parts[3].trim();
      if (method.isEmpty()) return Optional.empty();

      String endpoint = parts[4].trim();
      if (endpoint.isEmpty()) return Optional.empty();

      //rzutujemy na inty
      int status = Integer.parseInt(parts[5].trim());
      int latency = Integer.parseInt(parts[6].trim());
      int bytes = Integer.parseInt(parts[7].trim());

      if (latency < 0 || bytes < 0) return Optional.empty();

      return Optional.of(new LogEntry(reqId, time, ip, method, endpoint, status, latency, bytes));
      
    } catch (Exception e) {
      //jak się rozjebie to zwracamy puste :>
      return Optional.empty();
    }
  }
}
