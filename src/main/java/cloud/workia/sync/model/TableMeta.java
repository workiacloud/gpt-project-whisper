package cloud.workia.sync.model;

import java.util.HashMap;
import java.util.Map;

public class TableMeta {

    private Long id;
    private String tableName;
    private Map<String, String> keys = new HashMap<>();
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> datatypes = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Map<String, String> getKeys() {
        return keys;
    }

    public void setKeys(Map<String, String> keys) {
        this.keys = keys;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public Map<String, String> getDatatypes() {
        return datatypes;
    }

    public void setDatatypes(Map<String, String> datatypes) {
        this.datatypes = datatypes;
    }
}