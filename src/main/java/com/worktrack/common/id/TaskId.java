package com.worktrack.common.id;

public record TaskId(Long value) {
    public TaskId {
        if (value == null) throw new IllegalArgumentException("TaskId cannot be null");
    }
}
