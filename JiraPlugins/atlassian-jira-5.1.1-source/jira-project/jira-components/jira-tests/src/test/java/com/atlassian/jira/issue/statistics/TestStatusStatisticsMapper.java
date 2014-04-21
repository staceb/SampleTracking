/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package com.atlassian.jira.issue.statistics;

import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.local.ListeningTestCase;
import com.mockobjects.dynamic.Mock;
import com.atlassian.jira.config.ConstantsManager;

public class TestStatusStatisticsMapper extends ListeningTestCase
{
    @Test
    public void testEquals()
    {
        StatusStatisticsMapper mapper = new StatusStatisticsMapper(null);
        assertEquals(mapper, mapper);
        assertEquals(mapper.hashCode(), mapper.hashCode());

        StatusStatisticsMapper mapper2 = new StatusStatisticsMapper(null);
        assertEquals(mapper, mapper2);

        Mock mockConstantsManager = new Mock(ConstantsManager.class);
        mockConstantsManager.setStrict(true);

        assertTrue(mapper.equals(new StatusStatisticsMapper((ConstantsManager) mockConstantsManager.proxy())));
        assertEquals(mapper.hashCode(), new StatusStatisticsMapper((ConstantsManager) mockConstantsManager.proxy()).hashCode());
        mockConstantsManager.verify();

        assertFalse(mapper.equals(null));
        assertFalse(mapper.equals(new Object()));
        assertFalse(mapper.equals(new IssueKeyStatisticsMapper()));
        // Mappers of different issue "constants" are never equal to each other
        assertFalse(mapper.equals(new IssueTypeStatisticsMapper(null)));
        assertFalse(mapper.equals(new ResolutionStatisticsMapper(null)));
        assertFalse(mapper.equals(new PriorityStatisticsMapper(null)));
    }
}
