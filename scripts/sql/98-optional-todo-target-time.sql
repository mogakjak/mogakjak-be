-- MySQL: apply before deploying the application for issue #98.
-- Only relax nullability. Existing target times and focus records are preserved.
ALTER TABLE todo MODIFY COLUMN target_time_in_seconds INT NULL;

-- Expected: IS_NULLABLE = YES, DATA_TYPE = int.
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'todo'
  AND COLUMN_NAME = 'target_time_in_seconds';
