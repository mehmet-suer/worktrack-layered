package com.worktrack.infra.retry;

import org.springframework.core.annotation.AliasFor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.*;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Transactional
@Retryable(
        retryFor = {CannotAcquireLockException.class, QueryTimeoutException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 2, random = true)
)
public @interface DefaultRetry {
    @AliasFor(annotation = Retryable.class, attribute = "maxAttempts")
    int maxAttempts() default 3;

    @AliasFor(annotation = Retryable.class, attribute = "backoff")
    Backoff backoff() default @Backoff(delay = 200, multiplier = 2, random = true);
}