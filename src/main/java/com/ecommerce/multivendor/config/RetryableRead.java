package com.ecommerce.multivendor.config;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.CannotCreateTransactionException;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Apply to a read-only service method to transparently retry it (up to 3 attempts,
 * short exponential backoff) if it fails due to a transient database connection
 * problem — e.g. a pooled connection that went dead mid-flight, or a momentary
 * "could not open connection" blip (both seen in production logs against a
 * serverless DB tier). Only ever use this on side-effect-free reads: retrying a
 * write could double it up if the first attempt actually succeeded server-side
 * before the connection dropped.
 */
@Retryable(
        retryFor = {
                DataAccessResourceFailureException.class,
                CannotCreateTransactionException.class,
                TransientDataAccessException.class,
        },
        maxAttempts = 3,
        backoff = @Backoff(delay = 300, multiplier = 2)
)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RetryableRead {
}
