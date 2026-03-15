package cloud.workia.sync.model;

public class AutocompleteSuggestion {

    private Long id;
    private String label;
    private String secondary;

    public AutocompleteSuggestion() {
    }

    public AutocompleteSuggestion(Long id, String label, String secondary) {
        this.id = id;
        this.label = label;
        this.secondary = secondary;
    }

    public Long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getSecondary() {
        return secondary;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setSecondary(String secondary) {
        this.secondary = secondary;
    }
}