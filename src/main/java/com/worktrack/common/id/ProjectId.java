package com.worktrack.common.id;

public record ProjectId(Long value) {
    public ProjectId {
        if (value == null) throw new IllegalArgumentException("ProjectId cannot be null");
    }
}
