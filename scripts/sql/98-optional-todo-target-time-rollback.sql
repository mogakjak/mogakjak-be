-- Run only after the null-count check documented in docs/optional-todo-target-time.md.
-- Do not replace unset targets with invented values to force a rollback.
ALTER TABLE todo MODIFY COLUMN target_time_in_seconds INT NOT NULL;
