package com.worktrack.entity.base;

import com.worktrack.repo.hibernate.HibernateFilterNames;
import com.worktrack.repo.hibernate.HibernateFilterParameters;
import jakarta.persistence.*;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

@MappedSuperclass
@FilterDef(name = HibernateFilterNames.NOT_DELETED, parameters = @ParamDef(name = HibernateFilterParameters.DELETED_STATUS, type = String.class))
@Filter(name = HibernateFilterNames.NOT_DELETED, condition = "status != :deletedStatus") //HibernateFilterParameters.DELETED_STATUS
public abstract class StatusAwareBaseEntity extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    protected Status status;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @PrePersist
    protected void onCreation() {
        if (this.status == null) {
            this.status = Status.ACTIVE;
        }
    }
}
