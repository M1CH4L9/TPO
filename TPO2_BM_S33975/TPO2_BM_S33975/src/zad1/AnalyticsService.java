/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public record AnalyticsService(
    LogParser logParser,
    TimestampRepairService timestampRepairService
) {

  public AnalysisReport analyze(GeoTimeOptions options, GeoLookup lookup) throws Exception {
    int invalidLines = 0;
    List<LogEntry> validEntries = new ArrayList<>();

    //1. odrzucamy śmieci z logów
    for (String line : options.logLines()) {
        var parsed = logParser.parseLine(line);
        if (parsed.isEmpty()) {
            invalidLines++;
        } else {
            validEntries.add(parsed.get());
        }
    }
    
    //2. naprawiamy czasy
    ZoneId serverZone = ZoneId.of(options.serverZoneId());
    List<ResolvedLogEntry> resolved = timestampRepairService.repair(validEntries, serverZone);

    int gapRepaired = 0;
    int overlapResolved = 0;
    int ambiguousDropped = 0;
    int geoFailures = 0;

    List<String> droppedIds = new ArrayList<>();
    Map<String, Long> byCountry = new HashMap<>();
    Map<String, Long> byTimezone = new HashMap<>();
    long[] globalHourHist = new long[24];
    Map<String, long[]> hourHistByZone = new HashMap<>();

    //3. analityka
    for (ResolvedLogEntry r : resolved) {
        switch (r.resolutionKind()) {
            case GAP_REPAIRED -> gapRepaired++;
            case OVERLAP_RESOLVED -> overlapResolved++;
            case AMBIGUOUS_DROPPED -> {
                ambiguousDropped++;
                droppedIds.add(r.source().requestId());
                //wyrzuciliśmy wpis i idziemy do nexta
                continue;
            }
            case OK -> {}
        }

        try {
            //strzal po geolokalizacje
            GeoInfo geo = lookup.lookup(r.source().clientIp());
            
            //przeliczamy czas
            ZonedDateTime clientTime = r.serverTime().withZoneSameInstant(geo.zoneId());
            int hour = clientTime.getHour();

            //nabijamy liczniki
            byCountry.merge(geo.countryCode(), 1L, Long::sum);
            byTimezone.merge(geo.zoneId().getId(), 1L, Long::sum);
            
            globalHourHist[hour]++;
            hourHistByZone.computeIfAbsent(geo.zoneId().getId(), k -> new long[24])[hour]++;

        } catch (GeoLookupException e) {
            //jak api jebnie to po prostu zaliczamy jako blad geo
            geoFailures++;
        }
    }

    //zwracamy gotowy raport
    return new AnalysisReport(
        invalidLines,
        gapRepaired,
        overlapResolved,
        ambiguousDropped,
        geoFailures,
        droppedIds,
        byCountry,
        byTimezone,
        globalHourHist,
        hourHistByZone
    );
  }
}
