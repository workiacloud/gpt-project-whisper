package cloud.workia.sync.service;

import cloud.workia.sync.model.PagedResponse;
import cloud.workia.sync.model.RecordDetailResponse;
import cloud.workia.sync.model.RecordListItemResponse;
import cloud.workia.sync.model.RecordView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_SIZE = 500;

    private final CacheService cacheService;

    public QueryService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public PagedResponse<RecordListItemResponse> findAll(String tableName, int page, int size) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        List<RecordListItemResponse> items = cacheService
                .getResolvedRecordsPage(tableName, safePage, safeSize)
                .stream()
                .map(this::toListItem)
                .toList();

        long totalElements = cacheService.countActiveRecords(tableName);

        return new PagedResponse<>(items, safePage, safeSize, totalElements);
    }

    public RecordDetailResponse findById(String tableName, Long id) {
        RecordView record = cacheService.getResolvedRecord(tableName, id);
        return record == null ? null : toDetail(record);
    }

    public PagedResponse<RecordListItemResponse> findByFieldValue(
            String tableName,
            String field,
            String value,
            int page,
            int size
    ) {
        int safePage = normalizePage(page);
        int safeSize = normalizeSize(size);

        List<RecordListItemResponse> items = cacheService
                .getResolvedRecordsByIndexedFieldPage(tableName, field, value, safePage, safeSize)
                .stream()
                .filter(record -> Objects.equals(String.valueOf(record.getData().get(field)), value))
                .map(this::toListItem)
                .toList();

        long totalElements = cacheService.countResolvedRecordsByIndexedField(tableName, field, value);

        return new PagedResponse<>(items, safePage, safeSize, totalElements);
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

    private int normalizePage(int page) {
        return Math.max(page, 0);
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}