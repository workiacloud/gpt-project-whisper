package cloud.workia.sync.model;

import java.time.OffsetDateTime;

public class TableChangeEvent {

    private String type;
    private String table;
    private Long id;
    private Long version;
    private OffsetDateTime timestamp;
    private RecordListItemResponse item;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public RecordListItemResponse getItem() {
        return item;
    }

    public void setItem(RecordListItemResponse item) {
        this.item = item;
    }
}