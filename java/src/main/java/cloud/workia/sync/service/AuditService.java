package cloud.workia.sync.service;

import cloud.workia.sync.model.OperationType;
import cloud.workia.sync.model.PendingOperation;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class AuditService {

    private final SupabaseService supabaseService;

    public AuditService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    public Map<String, Long> writeAudits(PendingOperation operation) {
        Map<String, Long> auditIdsByField = new HashMap<>();

        Map<String, Object> before = operation.getBeforeRaw();
        Map<String, Object> after = operation.getAfterRaw();

        String who = supabaseService.resolveUserName(operation.getWhoId());
        OffsetDateTime now = OffsetDateTime.now();

        for (String field : after.keySet()) {
            if ("audits".equals(field)) {
                continue;
            }

            Object oldValue = before.get(field);
            Object newValue = after.get(field);

            if (operation.getOperationType() == OperationType.INSERT && newValue == null) {
                continue;
            }

            if (operation.getOperationType() == OperationType.UPDATE && valuesEqual(oldValue, newValue)) {
                continue;
            }

            Long auditId = supabaseService.nextAuditId();
            supabaseService.insertAuditRecord(
                    auditId,
                    now,
                    who,
                    operation.getWhoId(),
                    operation.getOperationType().getEventName(),
                    operation.getOperationType().getEventId(),
                    operation.getTableName(),
                    field,
                    stringify(oldValue),
                    stringify(newValue),
                    now.getYear()
            );
            auditIdsByField.put(field, auditId);
        }

        if (statusChanged(operation)) {
            Long auditId = supabaseService.nextAuditId();
            supabaseService.insertAuditRecord(
                    auditId,
                    now,
                    who,
                    operation.getWhoId(),
                    operation.getOperationType().getEventName(),
                    operation.getOperationType().getEventId(),
                    operation.getTableName(),
                    "status",
                    stringify(operation.getBeforeStatus()),
                    stringify(operation.getAfterStatus()),
                    now.getYear()
            );
            auditIdsByField.put("status", auditId);
        }

        return auditIdsByField;
    }

    private boolean statusChanged(PendingOperation operation) {
        Integer before = operation.getBeforeStatus();
        Integer after = operation.getAfterStatus();

        if (operation.getOperationType() == OperationType.INSERT) {
            return after != null;
        }

        return !stringify(before).equals(stringify(after));
    }

    private boolean valuesEqual(Object a, Object b) {
        return stringify(a).equals(stringify(b));
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}