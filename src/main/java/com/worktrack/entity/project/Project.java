package com.worktrack.entity.project;

import com.worktrack.entity.auth.User;
import com.worktrack.entity.base.AuditableBaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "project")
public class Project extends AuditableBaseEntity {

    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "owner_id", nullable = true)
    private User owner;

    public Project() {
    }

    public Project(String name, String description, User owner) {
        this.name = name;
        this.description = description;
        this.owner = owner;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public User getOwner() {
        return owner;
    }
    public void setOwner(User owner) {
        this.owner = owner;
    }
}
