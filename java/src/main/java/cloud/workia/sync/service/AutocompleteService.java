package cloud.workia.sync.service;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AutocompleteService {

    public static String autocompleteKey(String tableName, String field) {
        return "autocomplete:" + tableName + ":" + field;
    }

    public static Set<String> autocompleteEntries(String value, Long id) {
        Set<String> entries = new LinkedHashSet<>();
        if (value == null || value.isBlank() || id == null) {
            return entries;
        }

        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return entries;
        }

        for (int i = 1; i <= normalized.length(); i++) {
            entries.add(normalized.substring(0, i) + "|" + id);
        }

        entries.add(normalized + "*" + "|" + id);
        return entries;
    }

    private static String normalize(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }
}