package com.atlassian.jira.ofbiz;

import com.atlassian.jira.local.MockControllerTestCase;
import org.junit.Test;
import static org.junit.Assert.*;

import org.ofbiz.core.entity.jdbc.interceptors.SQLInterceptor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @since v4.0
 */
public class ChainedSQLInterceptorTest extends MockControllerTestCase
{
    @Test
    public void testChaining()
    {
        CountingSqlIntegceptor interceptor1 = new CountingSqlIntegceptor();
        CountingSqlIntegceptor interceptor2 = new CountingSqlIntegceptor();

        ChainedSQLInterceptor.Builder builder = new ChainedSQLInterceptor.Builder();

        final ChainedSQLInterceptor chainedInterceptor = builder.add(interceptor1).add(interceptor2).build();
        assertNotNull(chainedInterceptor);

        chainedInterceptor.beforeExecution(null, null, null);
        chainedInterceptor.beforeExecution(null, null, null);
        chainedInterceptor.beforeExecution(null, null, null);

        chainedInterceptor.afterSuccessfulExecution(null, null, null, null, 0);
        chainedInterceptor.afterSuccessfulExecution(null, null, null, null, 0);

        chainedInterceptor.onException(null, null, null, null);

        assertEquals(3, interceptor1.beforeExecutionCallCount.get());
        assertEquals(2, interceptor1.afterSuccessfulExecutionCallCount.get());
        assertEquals(1, interceptor1.onExceptionCallCount.get());

        assertEquals(3, interceptor2.beforeExecutionCallCount.get());
        assertEquals(2, interceptor2.afterSuccessfulExecutionCallCount.get());
        assertEquals(1, interceptor2.onExceptionCallCount.get());

    }

    @Test
    public void testCallOrdering()
    {
        SQLInterceptor i1 = mockController.getMock(SQLInterceptor.class);
        SQLInterceptor i2 = mockController.getMock(SQLInterceptor.class);
        SQLInterceptor i3 = mockController.getMock(SQLInterceptor.class);

        i1.beforeExecution(null,null,null);
        i2.beforeExecution(null,null,null);
        i3.beforeExecution(null,null,null);

        i3.afterSuccessfulExecution(null,null,null,null,0);
        i2.afterSuccessfulExecution(null,null,null,null,0);
        i1.afterSuccessfulExecution(null,null,null,null,0);

        i3.onException(null,null,null,null);
        i2.onException(null,null,null,null);
        i1.onException(null,null,null,null);

        mockController.replay();

        final ChainedSQLInterceptor chainedInterceptor = new ChainedSQLInterceptor.Builder().add(i1).add(i2).add(i3).build();

        chainedInterceptor.beforeExecution(null, null, null);
        chainedInterceptor.afterSuccessfulExecution(null, null, null,null,0);
        chainedInterceptor.onException(null, null, null,null);
    }


    private static class CountingSqlIntegceptor implements SQLInterceptor
    {
        final AtomicInteger beforeExecutionCallCount = new AtomicInteger();
        final AtomicInteger afterSuccessfulExecutionCallCount = new AtomicInteger();
        final AtomicInteger onExceptionCallCount = new AtomicInteger();

        public void beforeExecution(final String sqlString, final List<String> parameterValues, final Statement statement)
        {
            beforeExecutionCallCount.incrementAndGet();
        }

        public void afterSuccessfulExecution(final String sqlString, final List<String> parameterValues, final Statement statement, final ResultSet resultSet, final int rowsUpdated)
        {
            afterSuccessfulExecutionCallCount.incrementAndGet();
        }

        public void onException(final String sqlString, final List<String> parameterValues, final Statement statement, final SQLException sqlException)
        {
            onExceptionCallCount.incrementAndGet();
        }
    }
}
