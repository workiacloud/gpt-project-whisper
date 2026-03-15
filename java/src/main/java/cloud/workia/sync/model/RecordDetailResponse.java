package cloud.workia.sync.model;

import java.util.HashMap;
import java.util.Map;

public class RecordDetailResponse {

    private Long id;
    private Long version;
    private Map<String, Object> data = new HashMap<>();
    private Map<String, Object> audits = new HashMap<>();

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
}