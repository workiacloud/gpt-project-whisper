package cloud.workia.sync.service;

import cloud.workia.sync.model.ChangeRequest;
import cloud.workia.sync.model.OperationType;
import cloud.workia.sync.model.PendingOperation;
import cloud.workia.sync.model.RecordView;
import cloud.workia.sync.model.TableMeta;
import cloud.workia.sync.properties.AppProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TransactionQueueService {

    private final Map<String, List<PendingOperation>> queueByTable = new HashMap<>();

    private final CacheService cacheService;
    private final AuditService auditService;
    private final SupabaseService supabaseService;
    private final ReferenceResolverService referenceResolverService;
    private final WebSocketBroadcastService webSocketBroadcastService;
    private final AppProperties appProperties;

    public TransactionQueueService(
            CacheService cacheService,
            AuditService auditService,
            SupabaseService supabaseService,
            ReferenceResolverService referenceResolverService,
            WebSocketBroadcastService webSocketBroadcastService,
            AppProperties appProperties
    ) {
        this.cacheService = cacheService;
        this.auditService = auditService;
        this.supabaseService = supabaseService;
        this.referenceResolverService = referenceResolverService;
        this.webSocketBroadcastService = webSocketBroadcastService;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public synchronized void restoreQueuesFromRedis() {
        for (String tableName : appProperties.getTables().getNames()) {
            List<PendingOperation> restored = cacheService.loadQueue(tableName);
            if (!restored.isEmpty()) {
                queueByTable.put(tableName, new ArrayList<>(restored));
            }
        }
    }

    public synchronized void enqueue(ChangeRequest request) {
        request.validate();

        String tableName = request.getTableName();
        TableMeta tableMeta = cacheService.getTableMeta(tableName);
        if (tableMeta == null) {
            throw new IllegalArgumentException("Unknown table metadata for table: " + tableName);
        }

        Map<String, Object> currentRaw = resolveCurrentRaw(tableName, request.getId());
        Integer currentStatus = resolveCurrentStatus(tableName, request.getId());

        if (request.getOperationType() == OperationType.INSERT && currentRaw != null) {
            throw new IllegalArgumentException(
                    "Record already exists for insert, table: " + tableName + ", id: " + request.getId()
            );
        }

        if (request.getOperationType() != OperationType.INSERT && currentRaw == null) {
            throw new IllegalArgumentException(
                    "Record not found in cache for table: " + tableName + ", id: " + request.getId()
            );
        }

        validateVersion(request, currentRaw);

        Map<String, Object> before = currentRaw == null ? new HashMap<>() : deepCopy(currentRaw);
        Map<String, Object> after = currentRaw == null ? new HashMap<>() : deepCopy(currentRaw);

        Integer beforeStatus = currentStatus;
        Integer afterStatus;

        if (request.getOperationType() == OperationType.DELETE) {
            afterStatus = 0;
        } else {
            after.putAll(request.getChanges());
            if (request.getOperationType() == OperationType.INSERT) {
                after.put("version", 1L);
                afterStatus = 1;
            } else {
                after.put("version", nextVersion(currentRaw));
                afterStatus = beforeStatus == null ? 1 : beforeStatus;
            }
        }

        ensureAudits(after, tableMeta);

        PendingOperation operation = new PendingOperation();
        operation.setTableName(tableName);
        operation.setId(request.getId());
        operation.setWhoId(request.getWhoId());
        operation.setOperationType(request.getOperationType());
        operation.setBeforeStatus(beforeStatus);
        operation.setAfterStatus(afterStatus);
        operation.setBeforeRaw(before);
        operation.setAfterRaw(after);

        queueByTable.computeIfAbsent(tableName, key -> new ArrayList<>()).add(operation);
        persistQueue(tableName);

        if (queueByTable.get(tableName).size() >= appProperties.getQueue().getMaxSizePerTable()) {
            flushTable(tableName);
        }
    }

    @Scheduled(fixedDelayString = "#{@appProperties.queue.flushIntervalMs}")
    public synchronized void flushAllScheduled() {
        for (String tableName : new ArrayList<>(queueByTable.keySet())) {
            flushTable(tableName);
        }
    }

    @PreDestroy
    public synchronized void shutdownFlush() {
        flushAllScheduled();
    }

    public synchronized int getQueueSize(String tableName) {
        return queueByTable.getOrDefault(tableName, Collections.emptyList()).size();
    }

    private void flushTable(String tableName) {
        List<PendingOperation> operations = queueByTable.getOrDefault(tableName, Collections.emptyList());
        if (operations.isEmpty()) {
            return;
        }

        List<PendingOperation> mergedOperations = mergeByRecord(operations);
        TableMeta tableMeta = cacheService.getTableMeta(tableName);

        for (PendingOperation operation : mergedOperations) {
            Map<String, Long> auditIds = auditService.writeAudits(operation);
            mergeAuditIds(operation.getAfterRaw(), auditIds);

            OperationType persistenceOperation =
                    operation.getOperationType() == OperationType.DELETE
                            ? OperationType.UPDATE
                            : operation.getOperationType();

            supabaseService.applyRecord(
                    operation.getTableName(),
                    operation.getId(),
                    operation.getAfterRaw(),
                    operation.getAfterStatus(),
                    persistenceOperation
            );

            if (operation.getAfterStatus() != null && operation.getAfterStatus() == 0) {
                Long deletedVersion = extractVersion(operation.getAfterRaw());
                cacheService.removeRecord(operation.getTableName(), operation.getId());
                webSocketBroadcastService.broadcastDelete(
                        operation.getTableName(),
                        operation.getId(),
                        deletedVersion
                );
                continue;
            }

            cacheService.saveRawRecord(
                    operation.getTableName(),
                    operation.getId(),
                    operation.getAfterRaw()
            );

            RecordView resolved = referenceResolverService.buildResolvedView(
                    operation.getTableName(),
                    operation.getId(),
                    operation.getAfterRaw(),
                    tableMeta
            );
            cacheService.saveResolvedRecord(operation.getTableName(), operation.getId(), resolved);
            webSocketBroadcastService.broadcastUpsert(
                    operation.getTableName(),
                    operation.getId(),
                    resolved
            );
        }

        referenceResolverService.refreshLookup(tableName);
        queueByTable.put(tableName, new ArrayList<>());
        cacheService.clearQueue(tableName);
    }

    private List<PendingOperation> mergeByRecord(List<PendingOperation> operations) {
        Map<String, PendingOperation> merged = new LinkedHashMap<>();

        for (PendingOperation current : operations) {
            String key = current.getTableName() + ":" + current.getId();
            PendingOperation existing = merged.get(key);

            if (existing == null) {
                merged.put(key, current);
                continue;
            }

            existing.setAfterRaw(deepCopy(current.getAfterRaw()));
            existing.setAfterStatus(current.getAfterStatus());
            existing.setWhoId(current.getWhoId());

            if (existing.getOperationType() == OperationType.INSERT) {
                if (current.getOperationType() == OperationType.DELETE) {
                    existing.setOperationType(OperationType.DELETE);
                }
            } else {
                existing.setOperationType(current.getOperationType());
            }
        }

        return new ArrayList<>(merged.values());
    }

    private Map<String, Object> resolveCurrentRaw(String tableName, Long id) {
        List<PendingOperation> pending = queueByTable.getOrDefault(tableName, Collections.emptyList());
        for (int i = pending.size() - 1; i >= 0; i--) {
            PendingOperation operation = pending.get(i);
            if (operation.getId().equals(id)) {
                return deepCopy(operation.getAfterRaw());
            }
        }
        return cacheService.getRawRecord(tableName, id);
    }

    private Integer resolveCurrentStatus(String tableName, Long id) {
        List<PendingOperation> pending = queueByTable.getOrDefault(tableName, Collections.emptyList());
        for (int i = pending.size() - 1; i >= 0; i--) {
            PendingOperation operation = pending.get(i);
            if (operation.getId().equals(id)) {
                return operation.getAfterStatus();
            }
        }

        Map<String, Object> currentRaw = cacheService.getRawRecord(tableName, id);
        return currentRaw == null ? null : 1;
    }

    private void validateVersion(ChangeRequest request, Map<String, Object> currentRaw) {
        if (request.getOperationType() == OperationType.INSERT) {
            return;
        }

        Long currentVersion = extractVersion(currentRaw);
        if (!currentVersion.equals(request.getExpectedVersion())) {
            throw new IllegalStateException(
                    "Version conflict for table " + request.getTableName()
                            + ", id " + request.getId()
                            + ". Expected version " + request.getExpectedVersion()
                            + " but current version is " + currentVersion
            );
        }
    }

    private Long nextVersion(Map<String, Object> currentRaw) {
        return extractVersion(currentRaw) + 1L;
    }

    private Long extractVersion(Map<String, Object> metadata) {
        if (metadata == null) {
            return 0L;
        }
        Object value = metadata.get("version");
        return value == null ? 1L : Long.valueOf(String.valueOf(value));
    }

    private void persistQueue(String tableName) {
        cacheService.saveQueue(tableName, queueByTable.getOrDefault(tableName, Collections.emptyList()));
    }

    @SuppressWarnings("unchecked")
    private void mergeAuditIds(Map<String, Object> metadata, Map<String, Long> auditIds) {
        Map<String, Object> audits =
                (Map<String, Object>) metadata.computeIfAbsent("audits", key -> new HashMap<>());
        auditIds.forEach(audits::put);
    }

    @SuppressWarnings("unchecked")
    private void ensureAudits(Map<String, Object> metadata, TableMeta tableMeta) {
        Map<String, Object> audits = metadata.get("audits") instanceof Map<?, ?> existing
                ? new HashMap<>((Map<String, Object>) existing)
                : new HashMap<>();

        for (String field : tableMeta.getHeaders().keySet()) {
            audits.putIfAbsent(field, null);
        }

        metadata.put("audits", audits);
    }

    private Map<String, Object> deepCopy(Map<String, Object> source) {
        Map<String, Object> copy = new HashMap<>();

        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() instanceof Map<?, ?> nestedMap) {
                Map<String, Object> nestedCopy = new HashMap<>();
                for (Map.Entry<?, ?> nestedEntry : nestedMap.entrySet()) {
                    nestedCopy.put(String.valueOf(nestedEntry.getKey()), nestedEntry.getValue());
                }
                copy.put(entry.getKey(), nestedCopy);
            } else {
                copy.put(entry.getKey(), entry.getValue());
            }
        }

        return copy;
    }
}