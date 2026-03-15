package cloud.workia.sync.model;

public enum OperationType {
    INSERT(1),
    UPDATE(2),
    DELETE(3);

    private final int eventId;

    OperationType(int eventId) {
        this.eventId = eventId;
    }

    public int getEventId() {
        return eventId;
    }

    public String getEventName() {
        return name().toLowerCase();
    }
}