CREATE TABLE assistant_preferences (
 user_id uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
 mode varchar(24) NOT NULL DEFAULT 'STANDARD', page_context boolean NOT NULL DEFAULT true,
 agent_actions boolean NOT NULL DEFAULT false, proactive boolean NOT NULL DEFAULT false,
 project_key varchar(80) NOT NULL DEFAULT '', fallback_allowed boolean NOT NULL DEFAULT false
);
CREATE TABLE assistant_tasks (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 conversation_id uuid NOT NULL REFERENCES conversations(id) ON DELETE CASCADE,
 goal varchar(2000) NOT NULL, status varchar(24) NOT NULL, state text NOT NULL,
 steps_used integer NOT NULL DEFAULT 0, tool_calls integer NOT NULL DEFAULT 0,
 retries integer NOT NULL DEFAULT 0, elapsed_ms bigint NOT NULL DEFAULT 0,
 lease uuid, lease_until timestamptz, created_at timestamptz NOT NULL, updated_at timestamptz NOT NULL
);
CREATE INDEX idx_assistant_tasks_owner ON assistant_tasks(user_id, updated_at DESC);
CREATE TABLE assistant_pending_actions (
 id uuid PRIMARY KEY, user_id uuid NOT NULL REFERENCES users(id) ON DELETE CASCADE,
 task_id uuid NOT NULL REFERENCES assistant_tasks(id) ON DELETE CASCADE,
 action_type varchar(32) NOT NULL, exact_payload text NOT NULL, payload_hash varchar(64) NOT NULL,
 resource_hash varchar(64) NOT NULL, status varchar(24) NOT NULL,
 created_at timestamptz NOT NULL, expires_at timestamptz NOT NULL
);
CREATE INDEX idx_assistant_actions_task ON assistant_pending_actions(task_id);
ALTER TABLE user_memories ADD COLUMN scope varchar(24) NOT NULL DEFAULT 'GLOBAL';
ALTER TABLE user_memories ADD COLUMN scope_key varchar(128) NOT NULL DEFAULT '';
ALTER TABLE user_memories ADD COLUMN preference_key varchar(80) NOT NULL DEFAULT '';
ALTER TABLE user_memories ADD COLUMN expires_at timestamptz;
ALTER TABLE user_memories ADD CONSTRAINT chk_memory_scope CHECK (scope IN ('GLOBAL','PROJECT','CONVERSATION','SESSION'));
ALTER TABLE user_memories DROP CONSTRAINT chk_memory_category;
ALTER TABLE user_memories ADD CONSTRAINT chk_memory_category CHECK (category IN ('PREFERENCE','INTEREST','ASSISTANT_SETTING','PROJECT_CONTEXT','ONGOING_TASK','RECENT_GOAL','USER_DEFINED_FACT','WORKFLOW'));
