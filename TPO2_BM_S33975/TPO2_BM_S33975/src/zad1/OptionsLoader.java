/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;
import java.util.List;

public class OptionsLoader {
  @SuppressWarnings("unchecked")
  public GeoTimeOptions load(String fileName) throws Exception {
      Yaml yaml = new Yaml();

      //wczytujemy plik
      try (InputStream in = new FileInputStream(fileName)) {
      Map<String, Object> data = yaml.load(in);
      
      String zoneId = (String) data.get("serverZoneId");
      if (zoneId == null || zoneId.isBlank()) {
        throw new IllegalArgumentException("brak strefy");
      }
      
      //jak nie ma logow to dajemy pusta liste
      List<String> lines = (List<String>) data.get("logLines");
      if (lines == null) {
        lines = List.of();
      }
      
      return new GeoTimeOptions(zoneId, lines);
    }
  }
}
