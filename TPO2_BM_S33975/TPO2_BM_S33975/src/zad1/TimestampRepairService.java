/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneRules;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class TimestampRepairService {

  public List<ResolvedLogEntry> repair(List<LogEntry> entries, ZoneId serverZone) {

  List<ResolvedLogEntry> result = new ArrayList<>();
  ZoneRules rules = serverZone.getRules();

  //bufor na wpisy które nałożyły się na siebie podczas zmiany czasu
  List<LogEntry> overlapBuffer = new ArrayList<>();
  ZoneOffsetTransition currentOverlap = null;

  for (LogEntry e : entries) {
      LocalDateTime time = e.serverLocalTime();
      ZoneOffsetTransition transition = rules.getTransition(time);
      
      boolean isOverlap = transition != null && transition.isOverlap();
      
      if (isOverlap) {
         //dla pewnosci jakby przyszla inna zakladka (chociaz w logach to niemozliwe)
         if (currentOverlap != null && !currentOverlap.equals(transition)) {
             resolveBuffer(overlapBuffer, currentOverlap, serverZone, result);
             overlapBuffer.clear();
         }
         currentOverlap = transition;
         overlapBuffer.add(e);
      } else {
         //weszlismy w normalny czas, wiec czyscimy bufor zakladek
         if (!overlapBuffer.isEmpty()) {
             resolveBuffer(overlapBuffer, currentOverlap, serverZone, result);
             overlapBuffer.clear();
             currentOverlap = null;
         }
         
         boolean isGap = transition != null && transition.isGap();
         if (isGap) {
             //atZone z automatu przesuwa czas do przodu w luce wiec mamy to z glowy
             ZonedDateTime zdt = time.atZone(serverZone);
             result.add(new ResolvedLogEntry(e, zdt, ResolutionKind.GAP_REPAIRED));
         } else {
             //normalny poprawny czas
             ZonedDateTime zdt = time.atZone(serverZone);
             result.add(new ResolvedLogEntry(e, zdt, ResolutionKind.OK));
         }
      }
    }

    //jakby logi skonczyly sie w trakcie zakladki
    if (!overlapBuffer.isEmpty()) {
       resolveBuffer(overlapBuffer, currentOverlap, serverZone, result);
    }

    return result;
  }
  //czy zakladka jest do naprawienia
  private void resolveBuffer(List<LogEntry> buffer, ZoneOffsetTransition transition, ZoneId zone, List<ResolvedLogEntry> result) {
    int drops = 0;
    int dropIndex = -1;
    
    //liczymy ile razy czas polecial do tylu
    for (int i = 1; i < buffer.size(); i++) {
        if (buffer.get(i).serverLocalTime().isBefore(buffer.get(i-1).serverLocalTime())) {
            drops++;
            dropIndex = i;
        }
    }
    
    if (drops == 1) {
        //jeden punkt cofniecia, da sie naprawic (przed to stary offset, po to nowy)
        for (int i = 0; i < buffer.size(); i++) {
            LogEntry e = buffer.get(i);
            ZonedDateTime zdt;
            if (i < dropIndex) {
                zdt = ZonedDateTime.ofStrict(e.serverLocalTime(), transition.getOffsetBefore(), zone);
            } else {
                zdt = ZonedDateTime.ofStrict(e.serverLocalTime(), transition.getOffsetAfter(), zone);
            }
            result.add(new ResolvedLogEntry(e, zdt, ResolutionKind.OVERLAP_RESOLVED));
        }
    } else {
        //brak albo kilka punktow cofniecia - caly blok leci do kosza
        for (LogEntry e : buffer) {
            //parsujemy cokolwiek bo i tak to odrzucimy w analityce
            ZonedDateTime zdt = e.serverLocalTime().atZone(zone);
            result.add(new ResolvedLogEntry(e, zdt, ResolutionKind.AMBIGUOUS_DROPPED));
        }
    }
  }
}
