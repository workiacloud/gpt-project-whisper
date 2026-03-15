package cloud.workia.sync.service;

import cloud.workia.sync.model.RecordDetailResponse;
import cloud.workia.sync.model.RecordListItemResponse;
import cloud.workia.sync.model.RecordView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class QueryService {

    private final CacheService cacheService;

    public QueryService(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    public List<RecordListItemResponse> findAll(String tableName) {
        return cacheService.getAllResolvedRecords(tableName)
                .stream()
                .map(this::toListItem)
                .toList();
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
}