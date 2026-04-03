/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.time.ZoneId;

public class IpWhoIsGeoLookup implements GeoLookup {
    //wbudowany leniwy klient
    private final HttpClient client = HttpClient.newHttpClient();

  @Override
  public GeoInfo lookup(String ip) throws GeoLookupException {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create("https://ipwho.is/" + ip))
          .GET()
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      return parseGeoInfo(response.body());
    } catch (Exception e) {
      throw new GeoLookupException("strzal po api jebnal", e);
    }
  }

  public GeoInfo parseGeoInfo(String json) throws GeoLookupException {
    if (json == null || json.isEmpty()) {
      throw new GeoLookupException("pusty json");
    }

    //sprawdzamy czy sukces jest true
    if (!json.contains("\"success\":true") && !json.contains("\"success\": true")) {
      throw new GeoLookupException("api zwrocilo blad albo success na false");
    }

    //parsowanie po kosztach bez żadnej biblioteki
    String countryCode = extractRegex(json, "\"country_code\"\\s*:\\s*\"([^\"]+)\"");
    String zoneId = extractRegex(json, "\"timezone\"\\s*:\\s*\\{[^}]*\"id\"\\s*:\\s*\"([^\"]+)\"");

    if (countryCode == null || zoneId == null) {
      throw new GeoLookupException("brak pol w jsonie");
    }

    try {
      return new GeoInfo(countryCode, ZoneId.of(zoneId));
    } catch (Exception e) {
      // zlapie jak string strefy jest jakis z kosmosu
      throw new GeoLookupException("nierozpoznana strefa czasowa", e);
    }
  }
  //pomocnicze żeby nie pisać za dużo
  private String extractRegex(String text, String regex) {
    Matcher m = Pattern.compile(regex).matcher(text);
    return m.find() ? m.group(1) : null;
  }
    
}
