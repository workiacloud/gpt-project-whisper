package cloud.workia.sync.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class PendingOperation {

    private String tableName;
    private Long id;
    private Long whoId;
    private OperationType operationType;
    private Integer beforeStatus;
    private Integer afterStatus;
    private Map<String, Object> beforeRaw = new HashMap<>();
    private Map<String, Object> afterRaw = new HashMap<>();
    private Instant createdAt = Instant.now();

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

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public Integer getBeforeStatus() {
        return beforeStatus;
    }

    public void setBeforeStatus(Integer beforeStatus) {
        this.beforeStatus = beforeStatus;
    }

    public Integer getAfterStatus() {
        return afterStatus;
    }

    public void setAfterStatus(Integer afterStatus) {
        this.afterStatus = afterStatus;
    }

    public Map<String, Object> getBeforeRaw() {
        return beforeRaw;
    }

    public void setBeforeRaw(Map<String, Object> beforeRaw) {
        this.beforeRaw = beforeRaw;
    }

    public Map<String, Object> getAfterRaw() {
        return afterRaw;
    }

    public void setAfterRaw(Map<String, Object> afterRaw) {
        this.afterRaw = afterRaw;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}