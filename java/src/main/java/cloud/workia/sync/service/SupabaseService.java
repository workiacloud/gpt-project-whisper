package cloud.workia.sync.service;

import cloud.workia.sync.model.OperationType;
import cloud.workia.sync.model.TableMeta;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SupabaseService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SupabaseService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<TableMeta> loadTablesMeta() {
        String sql = """
            select id, table_name, metadata
            from public.tables_meta
            order by id
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TableMeta meta = new TableMeta();
            meta.setId(rs.getLong("id"));
            meta.setTableName(rs.getString("table_name"));

            Map<String, Object> metadata = parseJson(rs.getString("metadata"));
            meta.setKeys(castStringMap(metadata.get("keys")));
            meta.setHeaders(castStringMap(metadata.get("headers")));
            meta.setDatatypes(castStringMap(metadata.get("datatypes")));
            return meta;
        });
    }

    public List<Map<String, Object>> loadTableRecords(String tableName) {
        String sql = "select id, status, metadata from public." + tableName + " order by id";
        return jdbcTemplate.query(sql, recordMapper());
    }

    public void applyRecord(
            String tableName,
            Long id,
            Map<String, Object> metadata,
            Integer status,
            OperationType operationType
    ) {
        if (operationType == OperationType.INSERT) {
            String sql = "insert into public." + tableName + " (id, status, metadata) values (?, ?, ?::jsonb)";
            jdbcTemplate.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(sql);
                ps.setLong(1, id);
                ps.setInt(2, status == null ? 1 : status);
                ps.setString(3, writeJson(metadata));
                return ps;
            });
            return;
        }

        String sql = "update public." + tableName + " set status = ?, metadata = ?::jsonb where id = ?";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, status == null ? 1 : status);
            ps.setString(2, writeJson(metadata));
            ps.setLong(3, id);
            return ps;
        });
    }

    public Map<Long, String> loadNameLookup(String tableName) {
        String sql = "select id, status, metadata from public." + tableName + " order by id";
        List<Map<String, Object>> rows = jdbcTemplate.query(sql, recordMapper());

        Map<Long, String> lookup = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long id = ((Number) row.get("id")).longValue();
            Integer status = row.get("status") == null ? 1 : ((Number) row.get("status")).intValue();

            if (status == 1) {
                Map<String, Object> metadata = castMap(row.get("metadata"));
                lookup.put(id, String.valueOf(metadata.getOrDefault("name", id.toString())));
            }
        }
        return lookup;
    }

    public Long nextAuditId() {
        return jdbcTemplate.queryForObject("select nextval('public.system_audit_id_seq')", Long.class);
    }

    public void ensureAuditSequence() {
        jdbcTemplate.execute("""
            create sequence if not exists public.system_audit_id_seq
            start with 1
            increment by 1
            no minvalue
            no maxvalue
            cache 1
            """);

        jdbcTemplate.execute("""
            select setval(
                'public.system_audit_id_seq',
                coalesce((select max(id) from public.system_audit), 0) + 1,
                false
            )
            """);
    }

    public String resolveUserName(Long whoId) {
        if (whoId == null) {
            return null;
        }

        String sql = "select metadata from public.system_user where id = ?";
        List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("metadata"), whoId);

        if (rows.isEmpty()) {
            return null;
        }

        Map<String, Object> metadata = parseJson(rows.get(0));
        return metadata.get("name") == null ? null : String.valueOf(metadata.get("name"));
    }

    public void insertAuditRecord(
            Long id,
            OffsetDateTime eventTime,
            String who,
            Long whoId,
            String event,
            Integer eventId,
            String tableName,
            String columnName,
            String oldValue,
            String newValue,
            Integer year
    ) {
        String sql = """
            insert into public.system_audit
            (id, event_time, who, who_id, event, event_id, table_name, column_name, old_value, new_value, year, row_status)
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1)
            """;

        jdbcTemplate.update(
                sql,
                id,
                Timestamp.from(eventTime.toInstant()),
                who,
                whoId,
                event,
                eventId,
                tableName,
                columnName,
                oldValue,
                newValue,
                year
        );
    }

    private RowMapper<Map<String, Object>> recordMapper() {
        return (rs, rowNum) -> {
            Map<String, Object> row = new HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("status", rs.getInt("status"));
            row.put("metadata", parseJson(rs.getString("metadata")));
            return row;
        };
    }

    private Map<String, Object> parseJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse JSONB payload", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> castStringMap(Object source) {
        if (source == null) {
            return new HashMap<>();
        }
        return (Map<String, String>) source;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object source) {
        if (source == null) {
            return new HashMap<>();
        }
        return (Map<String, Object>) source;
    }

    private String writeJson(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to write JSONB payload", e);
        }
    }
}