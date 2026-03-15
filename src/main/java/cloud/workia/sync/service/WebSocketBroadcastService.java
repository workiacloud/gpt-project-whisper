package cloud.workia.sync.service;

import cloud.workia.sync.model.RecordListItemResponse;
import cloud.workia.sync.model.RecordView;
import cloud.workia.sync.model.TableChangeEvent;
import java.time.OffsetDateTime;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final QueryService queryService;

    public WebSocketBroadcastService(SimpMessagingTemplate messagingTemplate, QueryService queryService) {
        this.messagingTemplate = messagingTemplate;
        this.queryService = queryService;
    }

    public void broadcastUpsert(String tableName, Long id, RecordView resolvedRecord) {
        TableChangeEvent event = new TableChangeEvent();
        event.setType("UPSERT");
        event.setTable(tableName);
        event.setId(id);
        event.setVersion(extractVersion(resolvedRecord));
        event.setTimestamp(OffsetDateTime.now());

        RecordListItemResponse item = queryService.toListItem(resolvedRecord);
        event.setItem(item);

        messagingTemplate.convertAndSend("/topic/tables/" + tableName, event);
        messagingTemplate.convertAndSend("/topic/tables/all", event);
    }

    public void broadcastDelete(String tableName, Long id, Long version) {
        TableChangeEvent event = new TableChangeEvent();
        event.setType("DELETE");
        event.setTable(tableName);
        event.setId(id);
        event.setVersion(version);
        event.setTimestamp(OffsetDateTime.now());

        messagingTemplate.convertAndSend("/topic/tables/" + tableName, event);
        messagingTemplate.convertAndSend("/topic/tables/all", event);
    }

    private Long extractVersion(RecordView record) {
        Object value = record.getRaw().get("version");
        return value == null ? 1L : Long.valueOf(String.valueOf(value));
    }
}