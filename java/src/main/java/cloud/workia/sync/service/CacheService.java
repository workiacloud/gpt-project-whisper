package cloud.workia.sync.service;

import cloud.workia.sync.model.PendingOperation;
import cloud.workia.sync.model.RecordView;
import cloud.workia.sync.model.TableMeta;
import cloud.workia.sync.properties.AppProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CacheService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties appProperties;

    public CacheService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            AppProperties appProperties
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.appProperties = appProperties;
    }

    public void saveTableMeta(TableMeta tableMeta) {
        writeJson(metaKey(tableMeta.getTableName()), tableMeta);
    }

    public TableMeta getTableMeta(String tableName) {
        String json = redisTemplate.opsForValue().get(metaKey(tableName));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, TableMeta.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize table metadata for table: " + tableName,
                    e
            );
        }
    }

    public Map<String, String> getTableHeaders(String tableName) {
        return extractStringMap(tableName, "headers");
    }

    public Map<String, String> getTableKeys(String tableName) {
        return extractStringMap(tableName, "keys");
    }

    public void saveRawRecord(String tableName, Long id, Map<String, Object> rawMetadata) {
        writeJson(rawKey(tableName, id), rawMetadata);
        redisTemplate.opsForSet().add(activeIdsKey(tableName), String.valueOf(id));
    }

    public void saveResolvedRecord(String tableName, Long id, RecordView recordView) {
        RecordView previous = getResolvedRecord(tableName, id);
        if (previous != null) {
            deindexResolvedRecord(tableName, id, previous);
            deindexAutocompleteRecord(tableName, id, previous);
        }

        writeJson(resolvedKey(tableName, id), recordView);
        redisTemplate.opsForSet().add(activeIdsKey(tableName), String.valueOf(id));

        indexResolvedRecord(tableName, id, recordView);
        indexAutocompleteRecord(tableName, id, recordView);
    }

    public Map<String, Object> getRawRecord(String tableName, Long id) {
        String json = redisTemplate.opsForValue().get(rawKey(tableName, id));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize raw record for table: " + tableName + ", id: " + id,
                    e
            );
        }
    }

    public RecordView getResolvedRecord(String tableName, Long id) {
        String json = redisTemplate.opsForValue().get(resolvedKey(tableName, id));
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RecordView.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Failed to deserialize resolved record for table: " + tableName + ", id: " + id,
                    e
            );
        }
    }

    public List<RecordView> getAllResolvedRecords(String tableName) {
        Set<String> ids = redisTemplate.opsForSet().members(activeIdsKey(tableName));
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecordView> records = new ArrayList<>();
        for (String idValue : ids) {
            RecordView record = getResolvedRecord(tableName, Long.valueOf(idValue));
            if (record != null) {
                records.add(record);
            }
        }

        records.sort(Comparator.comparing(
                RecordView::getId,
                Comparator.nullsLast(Long::compareTo)
        ));

        return records;
    }

    public long countResolvedRecords(String tableName) {
        Long size = redisTemplate.opsForSet().size(activeIdsKey(tableName));
        return size == null ? 0L : size;
    }

    public List<RecordView> getResolvedRecordsByIndexedField(String tableName, String field, String value) {
        Set<String> ids = redisTemplate.opsForSet().members(indexKey(tableName, field, value));
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<RecordView> results = new ArrayList<>();
        for (String idValue : ids) {
            RecordView record = getResolvedRecord(tableName, Long.valueOf(idValue));
            if (record != null) {
                results.add(record);
            }
        }

        results.sort(Comparator.comparing(
                RecordView::getId,
                Comparator.nullsLast(Long::compareTo)
        ));

        return results;
    }

    public List<String> autocomplete(String tableName, String field, String query, int limit) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String key = AutocompleteService.autocompleteKey(tableName, field);
        Set<String> entries = redisTemplate.opsForZSet().range(key, 0, -1);
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedQuery = normalizeAutocompleteValue(query);
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();

        for (String entry : entries) {
            if (entry == null) {
                continue;
            }

            int pipeIndex = entry.lastIndexOf('|');
            if (pipeIndex <= 0 || pipeIndex >= entry.length() - 1) {
                continue;
            }

            String token = entry.substring(0, pipeIndex);
            if (!token.endsWith("*")) {
                continue;
            }

            String normalizedFullValue = token.substring(0, token.length() - 1);
            if (!normalizedFullValue.startsWith(normalizedQuery)) {
                continue;
            }

            Long id;
            try {
                id = Long.valueOf(entry.substring(pipeIndex + 1));
            } catch (NumberFormatException ex) {
                continue;
            }

            RecordView record = getResolvedRecord(tableName, id);
            if (record == null || record.getData() == null) {
                continue;
            }

            Object realValue = record.getData().get(field);
            if (realValue == null) {
                continue;
            }

            suggestions.add(String.valueOf(realValue));
            if (suggestions.size() >= Math.max(1, limit)) {
                break;
            }
        }

        return new ArrayList<>(suggestions);
    }

    public void removeRecord(String tableName, Long id) {
        RecordView existing = getResolvedRecord(tableName, id);
        if (existing != null) {
            deindexResolvedRecord(tableName, id, existing);
            deindexAutocompleteRecord(tableName, id, existing);
        }

        redisTemplate.delete(rawKey(tableName, id));
        redisTemplate.delete(resolvedKey(tableName, id));
        redisTemplate.opsForSet().remove(activeIdsKey(tableName), String.valueOf(id));
    }

    public void saveQueue(String tableName, List<PendingOperation> operations) {
        writeJson(queueKey(tableName), operations);
    }

    public List<PendingOperation> loadQueue(String tableName) {
        String json = redisTemplate.opsForValue().get(queueKey(tableName));
        if (json == null) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize queue for table: " + tableName, e);
        }
    }

    public void clearQueue(String tableName) {
        redisTemplate.delete(queueKey(tableName));
    }

    private void indexResolvedRecord(String tableName, Long id, RecordView recordView) {
        List<String> searchableFields = searchableFields(tableName);
        for (String field : searchableFields) {
            Object value = recordView.getData().get(field);
            if (value != null) {
                redisTemplate.opsForSet().add(
                        indexKey(tableName, field, String.valueOf(value)),
                        String.valueOf(id)
                );
            }
        }
    }

    private void deindexResolvedRecord(String tableName, Long id, RecordView recordView) {
        List<String> searchableFields = searchableFields(tableName);
        for (String field : searchableFields) {
            Object value = recordView.getData().get(field);
            if (value != null) {
                redisTemplate.opsForSet().remove(
                        indexKey(tableName, field, String.valueOf(value)),
                        String.valueOf(id)
                );
            }
        }
    }

    private void indexAutocompleteRecord(String tableName, Long id, RecordView recordView) {
        List<String> searchableFields = searchableFields(tableName);
        for (String field : searchableFields) {
            Object value = recordView.getData().get(field);
            if (value == null) {
                continue;
            }

            String key = AutocompleteService.autocompleteKey(tableName, field);
            for (String entry : AutocompleteService.autocompleteEntries(String.valueOf(value), id)) {
                redisTemplate.opsForZSet().add(key, entry, 0D);
            }
        }
    }

    private void deindexAutocompleteRecord(String tableName, Long id, RecordView recordView) {
        List<String> searchableFields = searchableFields(tableName);
        for (String field : searchableFields) {
            Object value = recordView.getData().get(field);
            if (value == null) {
                continue;
            }

            String key = AutocompleteService.autocompleteKey(tableName, field);
            for (String entry : AutocompleteService.autocompleteEntries(String.valueOf(value), id)) {
                redisTemplate.opsForZSet().remove(key, entry);
            }
        }
    }

    private List<String> searchableFields(String tableName) {
        List<String> fields = appProperties.getCache().getSearchableFields().get(tableName);
        return fields == null ? Collections.emptyList() : fields;
    }

    private Map<String, String> extractStringMap(String tableName, String propertyName) {
        TableMeta tableMeta = getTableMeta(tableName);
        if (tableMeta == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> metaMap = objectMapper.convertValue(
                tableMeta,
                new TypeReference<>() {}
        );

        Object direct = metaMap.get(propertyName);
        if (direct instanceof Map<?, ?> directMap) {
            return toStringMap(directMap);
        }

        Object metadata = metaMap.get("metadata");
        if (metadata instanceof Map<?, ?> metadataMap) {
            Object nested = metadataMap.get(propertyName);
            if (nested instanceof Map<?, ?> nestedMap) {
                return toStringMap(nestedMap);
            }
        }

        return Collections.emptyMap();
    }

    private Map<String, String> toStringMap(Map<?, ?> source) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            result.put(
                    String.valueOf(entry.getKey()),
                    entry.getValue() == null ? "" : String.valueOf(entry.getValue())
            );
        }
        return result;
    }

    private String normalizeAutocompleteValue(String value) {
        return value
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    private void writeJson(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize value for key: " + key, e);
        }
    }

    private String prefix() {
        return appProperties.getCache().getKeyPrefix();
    }

    private String metaKey(String tableName) {
        return prefix() + ":meta:" + tableName;
    }

    private String rawKey(String tableName, Long id) {
        return prefix() + ":raw:" + tableName + ":" + id;
    }

    private String resolvedKey(String tableName, Long id) {
        return prefix() + ":resolved:" + tableName + ":" + id;
    }

    private String activeIdsKey(String tableName) {
        return prefix() + ":ids:" + tableName;
    }

    private String indexKey(String tableName, String field, String value) {
        return prefix() + ":index:" + tableName + ":" + field + ":" + value;
    }

    private String queueKey(String tableName) {
        return prefix() + ":queue:" + tableName;
    }
}