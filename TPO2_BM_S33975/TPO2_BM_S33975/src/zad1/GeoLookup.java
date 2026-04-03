/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;


public interface GeoLookup {
  GeoInfo lookup(String ip) throws GeoLookupException;
}
