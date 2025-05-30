CREATE TABLE task_attachment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(1024),
    task_id BIGINT,
    version INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    created_by VARCHAR(50),
    updated_by VARCHAR(50),
    status VARCHAR(50) NOT NULL,
    CONSTRAINT fk_task_attachment_task_id FOREIGN KEY (task_id) REFERENCES task(id)
);

CREATE INDEX idx_task_attachment_task_id ON task_attachment(task_id);
CREATE INDEX idx_task_attachment_status ON task_attachment(status);