package cloud.workia.sync.model;

import java.util.HashMap;
import java.util.Map;

public class ChangeRequest {

    private String tableName;
    private Long id;
    private Long whoId;
    private Long expectedVersion;
    private Map<String, Object> changes = new HashMap<>();
    private OperationType operationType;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWhoId() {
        return whoId;
    }

    public void setWhoId(Long whoId) {
        this.whoId = whoId;
    }

    public Long getExpectedVersion() {
        return expectedVersion;
    }

    public void setExpectedVersion(Long expectedVersion) {
        this.expectedVersion = expectedVersion;
    }

    public Map<String, Object> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Object> changes) {
        this.changes = changes;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public void validate() {
        if (tableName == null || tableName.isBlank()) {
            throw new IllegalArgumentException("tableName is required");
        }
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be greater than zero");
        }
        if (operationType == null) {
            throw new IllegalArgumentException("operationType is required");
        }
        if (operationType != OperationType.DELETE && (changes == null || changes.isEmpty())) {
            throw new IllegalArgumentException("changes must not be empty for insert or update");
        }
        if (changes != null && changes.containsKey("audits")) {
            throw new IllegalArgumentException("changes must not contain audits");
        }
        if (changes != null && changes.containsKey("version")) {
            throw new IllegalArgumentException("changes must not contain version");
        }
        if (operationType == OperationType.UPDATE && expectedVersion == null) {
            throw new IllegalArgumentException("expectedVersion is required for update");
        }
        if (operationType == OperationType.DELETE && expectedVersion == null) {
            throw new IllegalArgumentException("expectedVersion is required for delete");
        }
    }
}