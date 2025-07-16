package com.worktrack.infra.hibernate;

import com.worktrack.entity.base.Status;
import com.worktrack.repo.hibernate.HibernateFilterNames;
import com.worktrack.repo.hibernate.HibernateFilterParameters;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class HibernateFilterManager {

    @PersistenceContext
    private EntityManager entityManager;

    public void enableNotDeletedFilter() {
        enableFilterWithParams(
                HibernateFilterNames.NOT_DELETED,
                Map.of(HibernateFilterParameters.DELETED_STATUS, Status.DELETED.name())
        );
    }

    public void enableFilterWithParams(String filterName, Map<String, Object> params) {
        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.enableFilter(filterName);
        params.forEach(filter::setParameter);
    }

}
