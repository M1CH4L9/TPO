/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;



import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public record AnalysisReport(
    int invalidLines,
    int repairedGapTimes,
    int resolvedOverlapEntries,
    int droppedAmbiguousEntries,
    int geoLookupFailures,
    List<String> droppedAmbiguousRequestIds,
    Map<String, Long> requestsByCountryCode,
    Map<String, Long> requestsBySenderTimezoneId,
    long[] senderHourHistogram,
    Map<String, long[]> senderHourHistogramByTimezoneId
) {
  private static final int COUNTRY_CODE_WIDTH = 4;
  private static final int TIMEZONE_WIDTH = 24;
  private static final int HOUR_RANGE_WIDTH = 11;
  private static final int REQUESTS_COUNT_WIDTH = 5;

  public String toText() {
    long[] hourlyHistogram = Optional.ofNullable(senderHourHistogram)
        .filter(hist -> hist.length >= 24)
        .orElse(new long[24]);

    String ambiguousIdsSection = Optional.ofNullable(droppedAmbiguousRequestIds)
        .filter(Predicate.not(List::isEmpty))
        .map(ids -> ids.stream().collect(Collectors.joining(System.lineSeparator())))
        .orElse("(none)");

    String countriesSection = Optional.ofNullable(requestsByCountryCode)
        .filter(Predicate.not(Map::isEmpty))
        .map(counts -> counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> ("%-" + COUNTRY_CODE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "d")
                .formatted(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(System.lineSeparator())))
        .orElse("(none)");

    String timezonesSection = Optional.ofNullable(requestsBySenderTimezoneId)
        .filter(Predicate.not(Map::isEmpty))
        .map(counts -> counts.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .map(entry -> ("%-" + TIMEZONE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "d")
                .formatted(entry.getKey(), entry.getValue()))
            .collect(Collectors.joining(System.lineSeparator())))
        .orElse("(none)");

    String hoursSection = IntStream.range(0, 24)
        .filter(hour -> hourlyHistogram[hour] > 0)
        .mapToObj(hour -> ("%-" + HOUR_RANGE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "d")
            .formatted("%02d:00-%02d:59".formatted(hour, hour), hourlyHistogram[hour]))
        .collect(Collectors.joining(System.lineSeparator()));

    if (hoursSection.isBlank()) {
      hoursSection = "(none)";
    }

    return """
        SUMMARY
        Metric                      Value
        --------------------------  -----
        Invalid lines               %d
        Repaired gap times          %d
        Resolved overlap entries    %d
        Dropped ambiguous entries   %d
        GeoLookup failures          %d

        AMBIGUOUS REQUEST IDS
        %s

        COUNTRIES
        %s
        %s
        %s

        TIMEZONES
        %s
        %s
        %s

        HOURS (sender)
        %s
        %s
        %s
        """.formatted(
            invalidLines,
            repairedGapTimes,
            resolvedOverlapEntries,
            droppedAmbiguousEntries,
            geoLookupFailures,
            ambiguousIdsSection,
            ("%-" + COUNTRY_CODE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("Code", "Count"),
            ("%-" + COUNTRY_CODE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("-".repeat(COUNTRY_CODE_WIDTH), "-".repeat(REQUESTS_COUNT_WIDTH)),
            countriesSection,
            ("%-" + TIMEZONE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("Timezone", "Count"),
            ("%-" + TIMEZONE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("-".repeat(TIMEZONE_WIDTH), "-".repeat(REQUESTS_COUNT_WIDTH)),
            timezonesSection,
            ("%-" + HOUR_RANGE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("Hour range", "Count"),
            ("%-" + HOUR_RANGE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("-".repeat(HOUR_RANGE_WIDTH), "-".repeat(REQUESTS_COUNT_WIDTH)),
            hoursSection
        );
  }

  public String timezoneHistogram(String timezoneId) {
    String key = timezoneId == null ? "" : timezoneId.trim();
    Map<String, long[]> histogramsByTimezone = Optional.ofNullable(senderHourHistogramByTimezoneId)
        .orElse(Map.of());
    long[] hist = Optional.ofNullable(histogramsByTimezone.get(key))
        .filter(hourly -> hourly.length >= 24)
        .orElse(new long[24]);

    String hourlyLines = IntStream.range(0, 24)
        .mapToObj(hour -> ("%-" + HOUR_RANGE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "d")
            .formatted("%02d:00-%02d:59".formatted(hour, hour), hist[hour]))
        .collect(Collectors.joining(System.lineSeparator()));

    return """
        TIMEZONE HISTOGRAM
        Timezone: %s

        %s
        %s
        %s
        """.formatted(
            key,
            ("%-" + HOUR_RANGE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("Hour range", "Count"),
            ("%-" + HOUR_RANGE_WIDTH + "s %" + REQUESTS_COUNT_WIDTH + "s").formatted("-".repeat(HOUR_RANGE_WIDTH), "-".repeat(REQUESTS_COUNT_WIDTH)),
            hourlyLines
        );
  }
}
