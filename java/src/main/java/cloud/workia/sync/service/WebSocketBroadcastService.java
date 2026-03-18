package cloud.workia.sync.service;

import cloud.workia.sync.model.RecordListItemResponse;
import cloud.workia.sync.model.RecordView;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class WebSocketBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final QueryService queryService;

    public WebSocketBroadcastService(
            SimpMessagingTemplate messagingTemplate,
            QueryService queryService
    ) {
        this.messagingTemplate = messagingTemplate;
        this.queryService = queryService;
    }

    public void broadcastUpsert(String tableName, Long id, RecordView resolvedRecord) {
        if (resolvedRecord == null) {
            return;
        }

        RecordListItemResponse item = queryService.toListItem(tableName, resolvedRecord);
        messagingTemplate.convertAndSend("/topic/tables/" + tableName + "/upsert", item);
    }

    public void broadcastDelete(String tableName, Long id) {
        messagingTemplate.convertAndSend("/topic/tables/" + tableName + "/delete", id);
    }
}