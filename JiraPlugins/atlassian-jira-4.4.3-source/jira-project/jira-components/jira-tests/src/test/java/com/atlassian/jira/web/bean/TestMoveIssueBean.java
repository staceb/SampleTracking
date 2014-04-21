/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package com.atlassian.jira.web.bean;

import org.junit.Test;
import static org.junit.Assert.*;

import com.atlassian.jira.local.ListeningTestCase;

import java.util.HashMap;
import java.util.Map;

public class TestMoveIssueBean extends ListeningTestCase
{
    @Test
    public void testReset()
    {
        MoveIssueBean bean = new MoveIssueBean(null, null);
        String[] testArray = new String[]{"1", "2", "3"};
        Map testMap = new HashMap();
        testMap.put("key", "value");

        bean.setTargetStatusId("1");
//        bean.setAssignee("admin");
//        bean.setAffectsVersionsIds(testArray);
//        bean.setComponentsIds(testArray);
//        bean.setFixVersionsIds(testArray);
//        bean.setCustomFieldValuesHolder(testMap);
        bean.setTaskTargetStatusHolder(testMap);

        bean.reset();

        assertNull(bean.getTargetStatusId());
//        assertNull(bean.getAssignee());
//        assertNull(bean.getAffectsVersionsIds());
//        assertNull(bean.getComponentsIds());
//        assertNull(bean.getFixVersionsIds());
        assertEquals(new HashMap(), bean.getFieldValuesHolder());
        assertNull(bean.getTaskTargetStatusHolder());

    }
}
