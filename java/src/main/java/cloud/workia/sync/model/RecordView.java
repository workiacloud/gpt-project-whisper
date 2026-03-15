package cloud.workia.sync.model;

import java.util.HashMap;
import java.util.Map;

public class RecordView {

    private Long id;
    private Map<String, Object> data = new HashMap<>();
    private Map<String, Object> audits = new HashMap<>();
    private Map<String, Object> raw = new HashMap<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getAudits() {
        return audits;
    }

    public void setAudits(Map<String, Object> audits) {
        this.audits = audits;
    }

    public Map<String, Object> getRaw() {
        return raw;
    }

    public void setRaw(Map<String, Object> raw) {
        this.raw = raw;
    }
}