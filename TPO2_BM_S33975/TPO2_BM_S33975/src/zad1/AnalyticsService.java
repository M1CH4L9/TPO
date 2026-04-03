/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;


import java.util.ArrayList;
import java.util.LinkedHashMap;

public record AnalyticsService(
    LogParser logParser,
    TimestampRepairService timestampRepairService
) {

  public AnalysisReport analyze(GeoTimeOptions options, GeoLookup lookup) throws Exception {
    return new AnalysisReport(
        0,
        0,
        0,
        0,
        0,
        new ArrayList<>(),
        new LinkedHashMap<>(),
        new LinkedHashMap<>(),
        new long[24],
        new LinkedHashMap<>()
    );
  }
}
