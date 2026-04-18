/**
 *
 *  @author Berlak Michał s33975
 *
 */

package zad1;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OptionsLoader {

    public GeoTimeOptions load(String fileName) throws Exception {
        String zoneId = null;
        List<String> logLines = new ArrayList<>();

        //odczytujemy plik
        List<String> lines = Files.readAllLines(Paths.get(fileName));
        boolean isLogLineSection = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            //wyciągnięcie strefy czasowej
            if (trimmedLine.startsWith("serverZoneId:")) {
                zoneId = trimmedLine.substring("serverZoneId:".length()).trim();

                //usuwamy cudzysłów (pojedynczy lub podwójny), jeśli istnieje, a potem robimy trim()
                if (zoneId.startsWith("\"") && zoneId.endsWith("\"") && zoneId.length() >= 2) {
                    zoneId = zoneId.substring(1, zoneId.length() - 1).trim();
                } else if (zoneId.startsWith("'") && zoneId.endsWith("'") && zoneId.length() >= 2) {
                    zoneId = zoneId.substring(1, zoneId.length() - 1).trim();
                }
            }
            //sprawdzenie czy zgadza się z logami
            else if (trimmedLine.startsWith("logLines:")) {
                isLogLineSection = true;
            }
            //jeśli jesteśmy w sekcji logów to dodajemy je do listy
            else if (isLogLineSection) {
                //usuwamy ewentualny myślnik listowy z YAML (np. "- r0001|...")
                if (trimmedLine.startsWith("-")) {
                    trimmedLine = trimmedLine.substring(1).trim();
                }
                //usuwamy cudzysłów, w które mógł być owinięty string
                if (trimmedLine.startsWith("\"") && trimmedLine.endsWith("\"") && trimmedLine.length() >= 2) {
                    trimmedLine = trimmedLine.substring(1, trimmedLine.length() - 1);
                } else if (trimmedLine.startsWith("'") && trimmedLine.endsWith("'") && trimmedLine.length() >= 2) {
                    trimmedLine = trimmedLine.substring(1, trimmedLine.length() - 1);
                }

                if (!trimmedLine.isEmpty()) {
                    logLines.add(trimmedLine);
                }
            }
        }

        if (zoneId == null || zoneId.isBlank()) {
            throw new IllegalArgumentException("brak strefy w pliku");
        }

        return new GeoTimeOptions(zoneId, logLines);
    }
}