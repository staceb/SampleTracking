package com.atlassian.jira.util;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A very simple interface to collect errors. This is typically used during form validation for collecting field
 * validation errors (use {@link #addError(String, String)}), and general errors
 * ({@link #addErrorMessage(String)}) that aren't field-specific (eg. permission problems).
 */
public interface ErrorCollection
{
    /**
     * Add a field-specific error message.
     * @param field Field name, eg. "assignee"
     * @param message Error message.
     */
    void addError(String field, String message);

    /**
     * Add a field-specific error message.
     * @param field Field name, eg. "assignee"
     * @param message Error message.
     * @param reason Reason for the error.
     */
    void addError(String field, String message, Reason reason);

    /**
     * Add error message relating to system state (not field-specific).
     * @param message Error message.
     */
    void addErrorMessage(String message);

    /**
     * Add error message relating to system state (not field-specific), and a reason.
     * @param message Error message.
     * @param reason Reason for the error.
     */
    void addErrorMessage(String message, Reason reason);

    /**
     * Get all non field-specific error messages.
     * @return Collection of error Strings.
     */
    Collection<String> getErrorMessages();

    /**
     * Populate this ErrorCollection with a new set of messages (existing errors are lost).
     * @param errorMessages List of error message {@link String}s.
     */
    void setErrorMessages(Collection<String> errorMessages);

    /**
     * Get error messages, then get rid of them.
     * @return The (now cleared) error messages.
     */
    Collection<String> getFlushedErrorMessages();

    /**
     * Get all field-specific errors.
     * @return Map of String: String pairs, eg. {"assignee": "Assignee is required"}
     */
    Map<String, String> getErrors();

    /**
     * Populate this ErrorCollection with general and field-specific errors.
     * @param errors ErrorCollection whose errors/messages we obtain.
     */
    void addErrorCollection(ErrorCollection errors);

    /**
     * Append new error messages to those already collected.
     * @param errorMessages Collection of error strings.
     */
    void addErrorMessages(Collection<String> errorMessages);

    /**
     * Append new field-specific errors to those already collected.
     * @param errors of String: String pairs, eg. {"assignee": "Assignee is required"}
     */
    void addErrors(Map<String, String> errors);

    /**
     * Whether any errors (of any type - field-specific or otherwise) have been collected.
     * @return true if any errors (of any type - field-specific or otherwise) have been collected.
     */
    boolean hasAnyErrors();

    /**
     * Add reasons why the function has not been performed.
     * The reasons may be used by callers of services to set return codes etc. for example in REST services.
     *
     * @param reasons a set of well known reasons why the function has not been performed.
     */
    void addReasons(Set<Reason>  reasons);

    /**
     * Add a reason why the function has not been performed.
     * The reasons may be used by callers of services to set return codes etc. for example in REST services.
     *
     * @param reason a well known reasons why the function has not been performed.
     */
    void addReason(Reason  reason);

    /**
     * Set reasons why the function has not been performed.
     * The reasons may be used by callers of services to set return codes etc. for example in REST services.
     *
     * @param reasons a set of well known reasons why the function has not been performed.
     */
    void setReasons(Set<Reason>  reasons);

    /**
     * A set of well known reasons why the function has not been performed.
     * The reasons may be used by callers of services to set return codes etc. for example in REST services.
     *
     * @return a set of well known reasons why the function has not been performed.
     */
    Set<Reason> getReasons();

    public static enum Reason
    {
        /**
         * That which you are seeking is not here.
         */
        NOT_FOUND,
        /**
         * Not allowed to perform function.
         */
        FORBIDDEN,
        /**
         * Data validation failed.
         */
        VALIDATION_FAILED,
        /**
         * We are all broken.
         */
        SERVER_ERROR,
    }
}