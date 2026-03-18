package cloud.workia.sync.service;

import cloud.workia.sync.model.PagedResponse;
import cloud.workia.sync.model.RecordDetailResponse;
import cloud.workia.sync.model.RecordListItemResponse;
import cloud.workia.sync.model.RecordView;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 200;

    private final CacheService cacheService;

    public QueryService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public PagedResponse<RecordListItemResponse> findAll(
            String tableName,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = normalizeSize(size);
        String safeSortBy = normalizeSortBy(sortBy);
        boolean descending = isDescending(sortDir);

        List<RecordView> allRecords = cacheService.getAllResolvedRecords(tableName);

        Comparator<RecordView> comparator = buildComparator(tableName, safeSortBy);
        if (descending) {
            comparator = comparator.reversed();
        }

        List<RecordView> sortedRecords = allRecords.stream()
                .sorted(comparator)
                .toList();

        long totalItems = sortedRecords.size();
        int totalPages = totalItems == 0
                ? 0
                : (int) Math.ceil((double) totalItems / safeSize);

        int fromIndex = safePage * safeSize;
        List<RecordListItemResponse> items;

        if (fromIndex >= sortedRecords.size()) {
            items = List.of();
        } else {
            int toIndex = Math.min(fromIndex + safeSize, sortedRecords.size());
            items = sortedRecords.subList(fromIndex, toIndex)
                    .stream()
                    .map(record -> toListItem(tableName, record))
                    .toList();
        }

        return new PagedResponse<>(
                items,
                safePage,
                safeSize,
                totalItems,
                totalPages,
                safePage + 1 < totalPages,
                safePage > 0
        );
    }

    public RecordDetailResponse findById(String tableName, Long id) {
        RecordView record = cacheService.getResolvedRecord(tableName, id);
        return record == null ? null : toDetail(tableName, record);
    }

    public List<RecordListItemResponse> findByFieldValue(String tableName, String field, String value) {
        if (value == null) {
            return List.of();
        }

        return cacheService.getAllResolvedRecords(tableName)
                .stream()
                .filter(record -> {
                    Object resolvedValue = resolveSortableFieldValue(tableName, record, field);
                    return Objects.equals(
                            normalizeText(resolvedValue),
                            normalizeText(value)
                    );
                })
                .map(record -> toListItem(tableName, record))
                .toList();
    }

    public Map<String, String> getHeaders(String tableName) {
        return cacheService.getTableHeaders(tableName);
    }

    public List<String> autocomplete(String tableName, String field, String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        Map<String, String> keys = cacheService.getTableKeys(tableName);
        String keySpec = keys.get(field);

        if (keySpec != null && keySpec.contains(":")) {
            ReferenceKey referenceKey = parseReferenceKey(keySpec);
            if (referenceKey != null) {
                return autocompleteReferencedField(referenceKey, query, limit);
            }
        }

        return cacheService.autocomplete(tableName, field, query, limit);
    }

    private List<String> autocompleteReferencedField(
            ReferenceKey referenceKey,
            String query,
            int limit
    ) {
        String normalizedQuery = normalizeText(query);
        Set<String> result = new LinkedHashSet<>();

        for (RecordView record : cacheService.getAllResolvedRecords(referenceKey.tableName())) {
            Object value = record.getData() == null ? null : record.getData().get(referenceKey.displayField());
            if (value == null) {
                continue;
            }

            String text = String.valueOf(value);
            if (!normalizeText(text).startsWith(normalizedQuery)) {
                continue;
            }

            result.add(text);
            if (result.size() >= Math.max(1, limit)) {
                break;
            }
        }

        return new ArrayList<>(result);
    }

    RecordListItemResponse toListItem(String tableName, RecordView record) {
        RecordListItemResponse response = new RecordListItemResponse();
        response.setId(record.getId());
        response.setVersion(extractVersion(record));
        response.setData(resolveDisplayData(tableName, record));
        return response;
    }

    private RecordDetailResponse toDetail(String tableName, RecordView record) {
        RecordDetailResponse response = new RecordDetailResponse();
        response.setId(record.getId());
        response.setVersion(extractVersion(record));
        response.setData(resolveDisplayData(tableName, record));
        response.setAudits(record.getAudits());
        return response;
    }

    private Map<String, Object> resolveDisplayData(String tableName, RecordView record) {
        Map<String, Object> source = record.getData();
        if (source == null || source.isEmpty()) {
            return Map.of();
        }

        Map<String, String> keys = cacheService.getTableKeys(tableName);
        if (keys.isEmpty()) {
            return source;
        }

        Map<String, Object> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            String keySpec = keys.get(field);
            if (keySpec == null || keySpec.isBlank()) {
                resolved.put(field, value);
                continue;
            }

            ReferenceKey referenceKey = parseReferenceKey(keySpec);
            if (referenceKey == null) {
                resolved.put(field, value);
                continue;
            }

            resolved.put(field, resolveReferenceValue(referenceKey, value));
        }

        return resolved;
    }

    private Object resolveSortableFieldValue(String tableName, RecordView record, String sortBy) {
        if ("id".equalsIgnoreCase(sortBy)) {
            return record.getId();
        }

        if ("version".equalsIgnoreCase(sortBy)) {
            return extractVersion(record);
        }

        Map<String, Object> resolvedData = resolveDisplayData(tableName, record);
        return resolvedData.get(sortBy);
    }

    private Object resolveReferenceValue(ReferenceKey referenceKey, Object rawValue) {
        if (rawValue == null) {
            return null;
        }

        Long id = toLong(rawValue);
        if (id == null) {
            return rawValue;
        }

        RecordView referenced = cacheService.getResolvedRecord(referenceKey.tableName(), id);
        if (referenced == null || referenced.getData() == null) {
            return rawValue;
        }

        Object displayValue = referenced.getData().get(referenceKey.displayField());
        return displayValue == null ? rawValue : displayValue;
    }

    private Long extractVersion(RecordView record) {
        Object value = record.getRaw().get("version");
        return value == null ? 1L : Long.valueOf(String.valueOf(value));
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private String normalizeSortBy(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "id";
        }
        return sortBy.trim();
    }

    private boolean isDescending(String sortDir) {
        return sortDir != null && "desc".equalsIgnoreCase(sortDir.trim());
    }

    private Comparator<RecordView> buildComparator(String tableName, String sortBy) {
        return (left, right) -> {
            Object leftValue = resolveSortableFieldValue(tableName, left, sortBy);
            Object rightValue = resolveSortableFieldValue(tableName, right, sortBy);

            int result = compareNullableValues(leftValue, rightValue);
            if (result != 0) {
                return result;
            }

            Long leftId = left.getId() == null ? 0L : left.getId();
            Long rightId = right.getId() == null ? 0L : right.getId();
            return Long.compare(leftId, rightId);
        };
    }

    private int compareNullableValues(Object leftValue, Object rightValue) {
        if (leftValue == null && rightValue == null) {
            return 0;
        }
        if (leftValue == null) {
            return -1;
        }
        if (rightValue == null) {
            return 1;
        }

        if (isNumeric(leftValue) && isNumeric(rightValue)) {
            BigDecimal leftNumber = toBigDecimal(leftValue);
            BigDecimal rightNumber = toBigDecimal(rightValue);
            return leftNumber.compareTo(rightNumber);
        }

        String leftText = normalizeText(leftValue);
        String rightText = normalizeText(rightValue);
        return leftText.compareTo(rightText);
    }

    private boolean isNumeric(Object value) {
        if (value instanceof Number) {
            return true;
        }

        try {
            new BigDecimal(String.valueOf(value).trim());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value).trim());
    }

    private String normalizeText(Object value) {
        return value == null
                ? ""
                : String.valueOf(value).trim().toLowerCase(Locale.ROOT);
    }

    private Long toLong(Object value) {
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private ReferenceKey parseReferenceKey(String keySpec) {
        String[] parts = keySpec.split(":", 2);
        if (parts.length != 2) {
            return null;
        }

        String tableName = parts[0].trim();
        String displayField = parts[1].trim();

        if (tableName.isBlank() || displayField.isBlank()) {
            return null;
        }

        return new ReferenceKey(tableName, displayField);
    }

    private record ReferenceKey(String tableName, String displayField) {
    }
}