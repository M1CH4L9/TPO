/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;


import java.time.ZoneId;

public class IpWhoIsGeoLookup implements GeoLookup {
  @Override
  public GeoInfo lookup(String ip) throws GeoLookupException {
    return parseGeoInfo("JSON");
  }

  public GeoInfo parseGeoInfo(String json) throws GeoLookupException {
    throw new GeoLookupException("TODO");
  }
}
