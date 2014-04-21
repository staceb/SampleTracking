package com.atlassian.jira.ofbiz;

import org.ofbiz.core.entity.GenericDelegator;
import org.ofbiz.core.entity.GenericValue;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * A wrapper around {@link org.ofbiz.core.entity.EntityListIterator} that does not throw
 * {@link org.ofbiz.core.entity.GenericEntityException}.
 * <p>
 * Note: This does not follow the contract for {@link Iterator}, in that the {@link #next()} call does not throw
 * {@link NoSuchElementException} but returns a null value if the last element has been reached.
 */
public interface OfBizListIterator
{
    void setDelegator(GenericDelegator genericDelegator);

    void afterLast();

    void beforeFirst();

    boolean last();

    boolean first();

    void close();

    GenericValue currentGenericValue();

    int currentIndex();

    boolean absolute(int i);

    GenericValue next();

    int nextIndex();

    GenericValue previous();

    int previousIndex();

    void setFetchSize(int i);

    List<GenericValue> getCompleteList();

    List<GenericValue> getPartialList(int i, int i1);

    void add(GenericValue o);

    void remove();

    void set(GenericValue o);

    /**
     * Returns true if the specified field name in the database is case sensitive.
     * @param fieldName the name of the entity field
     * @return true if the field is case sensitive in the database
     * @throws com.atlassian.jira.exception.DataAccessException if an underlying problem occurs.
     */
    boolean isCaseSensitive(String fieldName);
}
