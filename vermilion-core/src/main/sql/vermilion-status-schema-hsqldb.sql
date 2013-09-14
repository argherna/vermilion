--
-- vermilion-status-schema-hsqldb.sql
--

DROP TABLE IF EXISTS task_execution;
DROP TABLE IF EXISTS task_execution_status;

CREATE TABLE task_execution (
  id           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_name    VARCHAR(512) NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL 
);

COMMENT ON TABLE  task_execution              IS 'Captures the execution time for a task.';
COMMENT ON COLUMN task_execution.id           IS 'Primary key, the generated Identifier for a row.';
COMMENT ON COLUMN task_execution.task_name    IS 'The task name (as named by the application).';
COMMENT ON COLUMN task_execution.created_time IS 'Timestamp of when this record was created.';

CREATE TABLE task_execution_status (
  id           INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  task_exec_id INTEGER NOT NULL,
  exec_status  VARCHAR(9) NOT NULL,
  message      VARCHAR(512),
  update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
  CONSTRAINT fk_task_exec_id FOREIGN KEY (task_exec_id) REFERENCES task_execution (id)
);

COMMENT ON TABLE  task_execution_status              IS 'Captures the status time for a task.';
COMMENT ON COLUMN task_execution_status.id           IS 'Primary key, the generated Identifier for a row.';
COMMENT ON COLUMN task_execution_status.task_exec_id IS 'The task execution Id to associate this status with.';
COMMENT ON COLUMN task_execution_status.exec_status  IS 'An execution status, one of STARTING, STARTED, STOPPING, STOPPED, ABANDONED, COMPLETED, FAILED.';
COMMENT ON COLUMN task_execution_status.message      IS 'Optional accompanying message for a given status.';
COMMENT ON COLUMN task_execution_status.update_time  IS 'Timestamp this status was recorded.';
