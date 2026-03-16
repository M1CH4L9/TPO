/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Service {
    private String kraj;
    private String waluta;

    //API klucz do openWeatherApp
    private final String WEATHER_API_KEY = "dd50b36b482f107164a72dcf63d0d07f";
    //Api klucz fixed.io
    private final String FIXER_API_KEY = "ad519f6e711c02c56170f39a4c55f856";

    public Service(String k) {
        this.kraj = k;
        this.waluta = ogarnijWalute(k);
    }

    private String ogarnijWalute(String c) {
        for (Locale l : Locale.getAvailableLocales()) {
            if (c.equalsIgnoreCase(l.getDisplayCountry(Locale.ENGLISH))) {
                try {
                    return Currency.getInstance(l).getCurrencyCode();
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

    public String getWeather(String m) {
        String u = "https://api.openweathermap.org/data/2.5/weather?q=" + m + "," + kraj + "&appid=" + WEATHER_API_KEY + "&units=metric";
        return pobierz(u);
    }

    public Double getRateFor(String kod) {
        if (waluta == null) {
            return null;
        }

        String u = "http://data.fixer.io/api/latest?access_key=" + FIXER_API_KEY + "&symbols=" + waluta + "," + kod;
        String j = pobierz(u);

        if (j == null) {
            return null;
        }

        Double r1 = wyciagnij(j, waluta);
        Double r2 = wyciagnij(j, kod);

        if (r1 != null && r2 != null && r1 != 0) {
            return r2 / r1;
        }
        return null;
    }

    public Double getNBPRate() {
        if (waluta == null) {
            return null;
        }

        if (waluta.equals("PLN")) {
            return 1.0;
        }

        String j = pobierz("http://api.nbp.pl/api/exchangerates/rates/a/" + waluta + "/?format=json");

        if (j == null) {
            j = pobierz("http://api.nbp.pl/api/exchangerates/rates/b/" + waluta + "/?format=json");
        }

        if (j != null) {
            return wyciagnij(j, "mid");
        }
        return null;
    }

    private String pobierz(String link) {
        try (InputStream is = URI.create(link).toURL().openStream();
             BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            return br.lines().collect(Collectors.joining(System.lineSeparator()));
        } catch (Exception e) {
            return null;
        }
    }

    private Double wyciagnij(String j, String k) {
        Pattern p = Pattern.compile("\"" + k + "\"\\s*:\\s*([\\d.]+)");
        Matcher m = p.matcher(j);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return null;
    }
}  

