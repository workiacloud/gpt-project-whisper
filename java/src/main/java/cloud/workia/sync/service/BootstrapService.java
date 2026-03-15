package cloud.workia.sync.service;

import cloud.workia.sync.model.RecordView;
import cloud.workia.sync.model.TableMeta;
import cloud.workia.sync.properties.AppProperties;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BootstrapService {

    private final SupabaseService supabaseService;
    private final CacheService cacheService;
    private final ReferenceResolverService referenceResolverService;
    private final AppProperties appProperties;

    public BootstrapService(
            SupabaseService supabaseService,
            CacheService cacheService,
            ReferenceResolverService referenceResolverService,
            AppProperties appProperties
    ) {
        this.supabaseService = supabaseService;
        this.cacheService = cacheService;
        this.referenceResolverService = referenceResolverService;
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void loadCacheOnStartup() {
        supabaseService.ensureAuditSequence();

        List<TableMeta> tableMetas = supabaseService.loadTablesMeta();

        Map<String, TableMeta> metaMap = new HashMap<>();
        for (TableMeta tableMeta : tableMetas) {
            cacheService.saveTableMeta(tableMeta);
            metaMap.put(tableMeta.getTableName(), tableMeta);
            referenceResolverService.warmUpLookup(tableMeta.getTableName());
        }

        for (String tableName : appProperties.getTables().getNames()) {
            TableMeta tableMeta = metaMap.get(tableName);
            if (tableMeta == null) {
                continue;
            }

            List<Map<String, Object>> rows = supabaseService.loadTableRecords(tableName);
            for (Map<String, Object> row : rows) {
                Long id = ((Number) row.get("id")).longValue();
                Integer status = ((Number) row.get("status")).intValue();

                @SuppressWarnings("unchecked")
                Map<String, Object> metadata = (Map<String, Object>) row.get("metadata");

                if (!isVisible(status)) {
                    continue;
                }

                ensureAuditsStructure(metadata, tableMeta);
                cacheService.saveRawRecord(tableName, id, metadata);

                RecordView resolved = referenceResolverService.buildResolvedView(
                        tableName,
                        id,
                        metadata,
                        tableMeta
                );
                cacheService.saveResolvedRecord(tableName, id, resolved);
            }
        }
    }

    private boolean isVisible(Integer status) {
        return status == null || status == 1;
    }

    private void ensureAuditsStructure(Map<String, Object> metadata, TableMeta tableMeta) {
        Object auditsObj = metadata.get("audits");
        Map<String, Object> audits;

        if (auditsObj instanceof Map<?, ?> existing) {
            audits = new HashMap<>();
            for (Map.Entry<?, ?> entry : existing.entrySet()) {
                audits.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        } else {
            audits = new HashMap<>();
        }

        for (String field : tableMeta.getHeaders().keySet()) {
            audits.putIfAbsent(field, null);
        }

        metadata.put("audits", audits);
    }
}