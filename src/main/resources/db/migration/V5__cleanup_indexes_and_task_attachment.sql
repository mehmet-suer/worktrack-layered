DROP TABLE IF EXISTS task_attachment;

DROP INDEX idx_users_username ON users;

CREATE INDEX idx_task_project_id_status ON task(project_id, status);
