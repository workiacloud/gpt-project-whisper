Use this prompt in a new session:

```text
You are helping me continue a Java Spring Boot project called "valkey-supabase-sync".

Project context:
- Backend stack: Java 21, Spring Boot, JDBC, Valkey, Supabase/PostgreSQL, WebSocket/STOMP.
- Frontend client: React.
- All code and comments must be in English.
- When modifying the project, show only the classes that changed, in fully copyable code blocks.

Current architecture and behavior:
- The app reads configuration from application.yml.
- It connects to Supabase and Valkey.
- On startup, it reads public.tables_meta first.
- tables_meta has columns:
  - id bigint
  - table_name varchar
  - metadata jsonb
- tables_meta.metadata contains:
  - keys
  - headers
  - datatypes
- keys defines reference fields, for example employee.company -> company table.
- headers and datatypes are preserved for React usage.

Data model:
- Business tables in Supabase all have:
  - id bigint primary key
  - status ordinary column
  - metadata jsonb
- status is NO LONGER read from metadata JSON.
- status is now read from the normal SQL column named status.
- status = 1 means visible to React.
- status = 0 means logically deleted / hidden from React.

Caching behavior:
- On startup, the app loads visible rows from Supabase into Valkey.
- It stores both:
  - raw record
  - resolved record
- Raw record keeps IDs for references.
- Resolved record replaces reference IDs with display names for React.
- Example: raw company = 5, resolved company = "Coca Cola".

Write behavior:
- React sends partial change requests with:
  - tableName
  - id
  - whoId
  - expectedVersion
  - changes
- Operations supported:
  - INSERT
  - UPDATE
  - DELETE
- DELETE is logical:
  - set SQL column status = 0
  - remove record from visible Valkey cache
- Writes are accumulated in per-table queues.
- Queue flush conditions:
  - around 5 minutes
  - or 1000 operations
  - or shutdown
- Queue is also persisted in Valkey for recovery on restart.

Versioning:
- Records use optimistic versioning through metadata.version.
- Update and delete require expectedVersion.
- Version increments on update and logical delete.
- Insert starts with version = 1.

Audit behavior:
- There is a Supabase table public.system_audit with columns:
  - id
  - event_time
  - who
  - who_id
  - event
  - event_id
  - table_name
  - column_name
  - old_value
  - new_value
  - year
  - row_status
- Audit rows are inserted per changed field.
- A separate audit row is also inserted for status changes.
- event values:
  - insert
  - update
  - delete
- event_id values:
  - 1 insert
  - 2 update
  - 3 delete
- who is resolved from public.system_user.metadata -> 'name'
- who_id is public.system_user.id
- After inserting an audit row, its id is stored in metadata.audits[field].

Important logical split:
- Even though Supabase still stores audits inside metadata JSON, the backend treats it logically as:
  - business data
  - audit data
- Diffing is done on business fields, audits handled separately.

Query behavior:
- The API exposes GET endpoints to:
  - get all rows for a table
  - get by id
  - search by field
- DTOs are separated from internal cache/domain objects.

Realtime behavior:
- The app exposes WebSocket/STOMP updates for React.
- Endpoint:
  - /ws/updates
- Topics:
  - /topic/tables/all
  - /topic/tables/{tableName}
- Broadcast event types:
  - UPSERT
  - DELETE

Autocomplete:
- Autocomplete is implemented in Java using Valkey, not just React memory.
- It uses Valkey Sorted Sets (ZSET) with lexicographical range search.
- It uses the improved terminator-character pattern.
- Normalization:
  - lowercase
  - trimmed
  - accent-insensitive
- ZSET stores prefix entries plus terminal entries like:
  - g
  - go
  - gon
  - gonzalez{25
- Autocomplete endpoint returns lightweight suggestion DTOs.

Implementation preferences:
- Keep the architecture clean and production-oriented.
- Prefer backend autocomplete via Valkey.
- Keep React as consumer only.
- Show only changed classes when updating code.
- Do not rewrite the whole project unless I explicitly ask.
- Keep answers concise and focused on implementation.

Please continue helping me from this exact project state.
```

If you want, I can also give you a **shorter compact version** of the same prompt for faster reuse.
