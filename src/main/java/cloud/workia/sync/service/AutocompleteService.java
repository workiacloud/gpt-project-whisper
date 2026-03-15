package cloud.workia.sync.service;

import cloud.workia.sync.model.AutocompleteSuggestion;
import cloud.workia.sync.model.RecordView;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AutocompleteService {

    private static final int MAX_PREFIX_LENGTH = 20;
    private static final String TERMINATOR = "{";

    private final StringRedisTemplate redisTemplate;
    private final CacheService cacheService;

    public AutocompleteService(StringRedisTemplate redisTemplate, CacheService cacheService) {
        this.redisTemplate = redisTemplate;
        this.cacheService = cacheService;
    }

    public List<AutocompleteSuggestion> autocomplete(String table, String field, String term, int limit) {
        String normalizedTerm = normalize(term);
        if (normalizedTerm.isBlank()) {
            return List.of();
        }

        String zsetKey = autocompleteKey(table, field);

        Set<String> matches = redisTemplate.opsForZSet().rangeByLex(
                zsetKey,
                Range.of(
                        Range.Bound.inclusive(normalizedTerm),
                        Range.Bound.inclusive(normalizedTerm + Character.MAX_VALUE)
                )
        );

        if (matches == null || matches.isEmpty()) {
            return List.of();
        }

        List<AutocompleteSuggestion> results = new ArrayList<>();
        Set<Long> seenIds = new LinkedHashSet<>();

        for (String entry : matches) {
            if (results.size() >= limit) {
                break;
            }

            if (!isTerminalEntry(entry)) {
                continue;
            }

            Long id = extractId(entry);
            if (id == null || !seenIds.add(id)) {
                continue;
            }

            RecordView record = cacheService.getResolvedRecord(table, id);
            if (record == null) {
                continue;
            }

            Object labelValue = record.getData().get(field);
            if (labelValue == null) {
                continue;
            }

            String label = String.valueOf(labelValue);
            String secondary = record.getData().containsKey("code")
                    ? String.valueOf(record.getData().get("code"))
                    : null;

            results.add(new AutocompleteSuggestion(id, label, secondary));
        }

        return results;
    }

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }

        String lower = input.trim().toLowerCase();
        String normalized = Normalizer.normalize(lower, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "");
    }

    public static String autocompleteKey(String table, String field) {
        return "autocomplete:zset:" + table + ":" + field;
    }

    public static List<String> autocompleteEntries(String value, Long id) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            return List.of();
        }

        if (normalized.length() > MAX_PREFIX_LENGTH) {
            normalized = normalized.substring(0, MAX_PREFIX_LENGTH);
        }

        List<String> entries = new ArrayList<>(normalized.length() + 1);

        for (int i = 1; i <= normalized.length(); i++) {
            entries.add(normalized.substring(0, i));
        }

        entries.add(normalized + TERMINATOR + id);
        return entries;
    }

    private boolean isTerminalEntry(String entry) {
        return entry.indexOf(TERMINATOR) >= 0;
    }

    private Long extractId(String entry) {
        int separatorIndex = entry.lastIndexOf(TERMINATOR);
        if (separatorIndex < 0 || separatorIndex == entry.length() - 1) {
            return null;
        }
        return Long.valueOf(entry.substring(separatorIndex + 1));
    }
}