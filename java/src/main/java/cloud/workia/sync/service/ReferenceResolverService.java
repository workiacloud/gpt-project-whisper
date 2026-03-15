package cloud.workia.sync.service;

import cloud.workia.sync.model.RecordView;
import cloud.workia.sync.model.TableMeta;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class ReferenceResolverService {

    private final SupabaseService supabaseService;
    private final Map<String, Map<Long, String>> lookupCache = new ConcurrentHashMap<>();

    public ReferenceResolverService(SupabaseService supabaseService) {
        this.supabaseService = supabaseService;
    }

    public void warmUpLookup(String tableName) {
        lookupCache.put(tableName, supabaseService.loadNameLookup(tableName));
    }

    public RecordView buildResolvedView(String tableName, Long id, Map<String, Object> rawMetadata, TableMeta tableMeta) {
        RecordView view = new RecordView();
        view.setId(id);
        view.setRaw(new HashMap<>(rawMetadata));

        Map<String, Object> businessData = new HashMap<>();
        Map<String, Object> audits = new HashMap<>();

        for (Map.Entry<String, Object> entry : rawMetadata.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();

            if ("audits".equals(field)) {
                if (value instanceof Map<?, ?> auditMap) {
                    for (Map.Entry<?, ?> auditEntry : auditMap.entrySet()) {
                        audits.put(String.valueOf(auditEntry.getKey()), auditEntry.getValue());
                    }
                }
                continue;
            }

            if (tableMeta.getKeys().containsKey(field) && value != null) {
                String referenceTable = tableMeta.getKeys().get(field);
                Long referenceId = Long.valueOf(String.valueOf(value));
                String referenceName = resolveReference(referenceTable, referenceId);
                businessData.put(field, referenceName);
            } else {
                businessData.put(field, value);
            }
        }

        view.setData(businessData);
        view.setAudits(audits);
        return view;
    }

    public String resolveReference(String referenceTable, Long referenceId) {
        Map<Long, String> lookup = lookupCache.computeIfAbsent(referenceTable, supabaseService::loadNameLookup);
        return lookup.getOrDefault(referenceId, String.valueOf(referenceId));
    }

    public void refreshLookup(String tableName) {
        lookupCache.put(tableName, supabaseService.loadNameLookup(tableName));
    }
}