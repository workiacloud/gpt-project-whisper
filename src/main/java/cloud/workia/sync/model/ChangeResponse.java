package cloud.workia.sync.model;

public class ChangeResponse {

    private String message;
    private String table;
    private Long id;
    private String operation;
    private int queueSize;

    public ChangeResponse() {
    }

    public ChangeResponse(String message, String table, Long id, String operation, int queueSize) {
        this.message = message;
        this.table = table;
        this.id = id;
        this.operation = operation;
        this.queueSize = queueSize;
    }

    public String getMessage() {
        return message;
    }

    public String getTable() {
        return table;
    }

    public Long getId() {
        return id;
    }

    public String getOperation() {
        return operation;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setQueueSize(int queueSize) {
        this.queueSize = queueSize;
    }
}