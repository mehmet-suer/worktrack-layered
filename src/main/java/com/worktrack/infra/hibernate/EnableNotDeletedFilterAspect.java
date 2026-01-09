package com.worktrack.infra.hibernate;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Aspect
@Component
public class EnableNotDeletedFilterAspect {

    private final HibernateFilterManager filterManager;

    public EnableNotDeletedFilterAspect(HibernateFilterManager filterManager) {
        this.filterManager = filterManager;
    }

    @Before("(@annotation(tx) || @within(tx))")
    public void enableFilterOnlyForReadOnly(Transactional tx) {
        if (tx.readOnly()) {
            filterManager.enableNotDeletedFilter();
        }
    }
}
