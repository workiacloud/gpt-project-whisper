package cloud.workia.sync.service;

import cloud.workia.sync.model.PagedResponse;
import cloud.workia.sync.model.RecordDetailResponse;
import cloud.workia.sync.model.RecordListItemResponse;
import cloud.workia.sync.model.RecordView;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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

        Comparator<RecordView> comparator = buildComparator(safeSortBy);
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
                    .map(this::toListItem)
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
        return record == null ? null : toDetail(record);
    }

    public List<RecordListItemResponse> findByFieldValue(String tableName, String field, String value) {
        return cacheService.getResolvedRecordsByIndexedField(tableName, field, value)
                .stream()
                .filter(record -> Objects.equals(String.valueOf(record.getData().get(field)), value))
                .map(this::toListItem)
                .toList();
    }

    public RecordListItemResponse toListItem(RecordView record) {
        RecordListItemResponse response = new RecordListItemResponse();
        response.setId(record.getId());
        response.setVersion(extractVersion(record));
        response.setData(record.getData());
        return response;
    }

    private RecordDetailResponse toDetail(RecordView record) {
        RecordDetailResponse response = new RecordDetailResponse();
        response.setId(record.getId());
        response.setVersion(extractVersion(record));
        response.setData(record.getData());
        response.setAudits(record.getAudits());
        return response;
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

    private Comparator<RecordView> buildComparator(String sortBy) {
        return (left, right) -> {
            Object leftValue = extractSortableFieldValue(left, sortBy);
            Object rightValue = extractSortableFieldValue(right, sortBy);

            int result = compareNullableValues(leftValue, rightValue);
            if (result != 0) {
                return result;
            }

            Long leftId = left.getId() == null ? 0L : left.getId();
            Long rightId = right.getId() == null ? 0L : right.getId();
            return Long.compare(leftId, rightId);
        };
    }

    private Object extractSortableFieldValue(RecordView record, String sortBy) {
        if ("id".equalsIgnoreCase(sortBy)) {
            return record.getId();
        }

        if ("version".equalsIgnoreCase(sortBy)) {
            return extractVersion(record);
        }

        if (record.getData() == null) {
            return null;
        }

        return record.getData().get(sortBy);
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

        String leftText = String.valueOf(leftValue).trim().toLowerCase(Locale.ROOT);
        String rightText = String.valueOf(rightValue).trim().toLowerCase(Locale.ROOT);
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
}