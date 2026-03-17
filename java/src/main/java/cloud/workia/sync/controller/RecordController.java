package cloud.workia.sync.controller;

import cloud.workia.sync.model.ChangeRequest;
import cloud.workia.sync.model.ChangeResponse;
import cloud.workia.sync.model.OperationType;
import cloud.workia.sync.model.PagedResponse;
import cloud.workia.sync.model.RecordDetailResponse;
import cloud.workia.sync.model.RecordListItemResponse;
import cloud.workia.sync.service.QueryService;
import cloud.workia.sync.service.TransactionQueueService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "*")
public class RecordController {

    private final QueryService queryService;
    private final TransactionQueueService transactionQueueService;

    public RecordController(
            QueryService queryService,
            TransactionQueueService transactionQueueService
    ) {
        this.queryService = queryService;
        this.transactionQueueService = transactionQueueService;
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<PagedResponse<RecordListItemResponse>> getAll(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(name = "sortDir", defaultValue = "asc") String sortDir
    ) {
        return ResponseEntity.ok(queryService.findAll(tableName, page, size, sortBy, sortDir));
    }

    @GetMapping("/{tableName}/{id}")
    public ResponseEntity<RecordDetailResponse> getById(
            @PathVariable("tableName") String tableName,
            @PathVariable("id") Long id
    ) {
        RecordDetailResponse record = queryService.findById(tableName, id);
        if (record == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(record);
    }

    @GetMapping("/{tableName}/search")
    public ResponseEntity<List<RecordListItemResponse>> searchByField(
            @PathVariable("tableName") String tableName,
            @RequestParam(name = "field") String field,
            @RequestParam(name = "value") String value
    ) {
        return ResponseEntity.ok(queryService.findByFieldValue(tableName, field, value));
    }

    @PostMapping("/{tableName}/insert")
    public ResponseEntity<ChangeResponse> insert(
            @PathVariable("tableName") String tableName,
            @RequestBody ChangeRequest request
    ) {
        request.setTableName(tableName);
        request.setOperationType(OperationType.INSERT);
        transactionQueueService.enqueue(request);
        return ResponseEntity.ok(buildResponse(request));
    }

    @PostMapping("/{tableName}/update")
    public ResponseEntity<ChangeResponse> update(
            @PathVariable("tableName") String tableName,
            @RequestBody ChangeRequest request
    ) {
        request.setTableName(tableName);
        request.setOperationType(OperationType.UPDATE);
        transactionQueueService.enqueue(request);
        return ResponseEntity.ok(buildResponse(request));
    }

    @PostMapping("/{tableName}/delete/{id}")
    public ResponseEntity<ChangeResponse> delete(
            @PathVariable("tableName") String tableName,
            @PathVariable("id") Long id,
            @RequestParam(name = "expectedVersion") Long expectedVersion,
            @RequestParam(name = "whoId", required = false) Long whoId
    ) {
        ChangeRequest request = new ChangeRequest();
        request.setTableName(tableName);
        request.setId(id);
        request.setWhoId(whoId);
        request.setExpectedVersion(expectedVersion);
        request.setOperationType(OperationType.DELETE);

        transactionQueueService.enqueue(request);
        return ResponseEntity.ok(buildResponse(request));
    }

    private ChangeResponse buildResponse(ChangeRequest request) {
        return new ChangeResponse(
                "Change accepted",
                request.getTableName(),
                request.getId(),
                request.getOperationType().getEventName(),
                transactionQueueService.getQueueSize(request.getTableName())
        );
    }
}