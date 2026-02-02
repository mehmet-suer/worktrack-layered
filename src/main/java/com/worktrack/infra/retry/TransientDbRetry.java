package com.worktrack.infra.retry;

import org.springframework.core.annotation.AliasFor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Retryable(
        retryFor = {
                TransientDataAccessException.class,
                CannotAcquireLockException.class,
                QueryTimeoutException.class,
                CannotGetJdbcConnectionException.class
        },
        maxAttempts = 3,
        backoff = @Backoff(delay = 200, multiplier = 2, random = true)
)
public @interface TransientDbRetry {
    @AliasFor(annotation = Retryable.class, attribute = "maxAttempts")
    int maxAttempts() default 3;

    @AliasFor(annotation = Retryable.class, attribute = "backoff")
    Backoff backoff() default @Backoff(delay = 200, multiplier = 2, random = true);
}
