package cloud.workia.sync.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("appProperties")
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private final Tables tables = new Tables();
    private final Queue queue = new Queue();
    private final Cache cache = new Cache();

    public Tables getTables() {
        return tables;
    }

    public Queue getQueue() {
        return queue;
    }

    public Cache getCache() {
        return cache;
    }

    public static class Tables {
        private List<String> names = new ArrayList<>();

        public List<String> getNames() {
            return names;
        }

        public void setNames(List<String> names) {
            this.names = names;
        }
    }

    public static class Queue {
        private int maxSizePerTable = 1000;
        private long flushIntervalMs = 300000L;

        public int getMaxSizePerTable() {
            return maxSizePerTable;
        }

        public void setMaxSizePerTable(int maxSizePerTable) {
            this.maxSizePerTable = maxSizePerTable;
        }

        public long getFlushIntervalMs() {
            return flushIntervalMs;
        }

        public void setFlushIntervalMs(long flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
        }
    }

    public static class Cache {
        private String keyPrefix = "appcache";
        private Map<String, List<String>> searchableFields = new HashMap<>();

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Map<String, List<String>> getSearchableFields() {
            return searchableFields;
        }

        public void setSearchableFields(Map<String, List<String>> searchableFields) {
            this.searchableFields = searchableFields;
        }
    }
}