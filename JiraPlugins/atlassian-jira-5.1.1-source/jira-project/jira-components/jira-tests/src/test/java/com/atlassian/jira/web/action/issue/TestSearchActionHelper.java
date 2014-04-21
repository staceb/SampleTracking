package com.atlassian.jira.web.action.issue;

import com.atlassian.jira.local.MockControllerTestCase;
import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.user.preferences.PreferenceKeys;
import com.atlassian.jira.util.Supplier;
import com.atlassian.jira.web.SessionKeys;
import com.atlassian.jira.web.bean.PagerFilter;
import com.atlassian.jira.web.session.SessionPagerFilterManager;
import org.easymock.Capture;
import org.easymock.EasyMock;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple test for {@link com.atlassian.jira.web.action.issue.TestSearchActionHelper}. 
 *
 * @since v4.0
 */
public class TestSearchActionHelper extends MockControllerTestCase
{
    /**
     * Make sure that reseting the pager works.
     */
    @Test
    public void testResetPager()
    {
        final PagerFilter originalPagerFilter = new PagerFilter();

        final Preferences mockUserPreferences = getMock(Preferences.class);
        expect(mockUserPreferences.getLong(PreferenceKeys.USER_ISSUES_PER_PAGE))
                .andReturn(10L);

        final SessionPagerFilterManager mockSessionPagerFilterManager = getMock(SessionPagerFilterManager.class);
        mockSessionPagerFilterManager.setCurrentObject(not(same(originalPagerFilter)));
        expectLastCall();

        final SearchActionHelper actionHelper = mockController.instantiate(SearchActionHelper.class);
        actionHelper.resetPager();
    }

    /**
     * Make sure that reseting the pager works when there is an error with the user's settings.
     */
    @Test
    public void testResetPagerMaxInvalid()
    {
        final Preferences mockUserPreferences = getMock(Preferences.class);
        expect(mockUserPreferences.getLong(PreferenceKeys.USER_ISSUES_PER_PAGE))
                .andThrow(new NumberFormatException("Injected error from testResetPagerInvalid"));

        final Capture<PagerFilter> pagerCapture = new Capture<PagerFilter>();
        final SessionPagerFilterManager mockSessionPagerFilterManager = getMock(SessionPagerFilterManager.class);
        mockSessionPagerFilterManager.setCurrentObject(EasyMock.capture(pagerCapture));
        expectLastCall();

        final SearchActionHelper actionHelper = mockController.instantiate(SearchActionHelper.class);
        actionHelper.resetPager();

        assertEquals(20, pagerCapture.getValue().getMax());
    }

    /**
     * Make sure that we retrieve the pager from the session.
     */
    @Test
    public void testGetPagerFilterAlreadyInSession()
    {
        final int max = 4859;
        final PagerFilter originalPagerFilter = new PagerFilter(max);

        final SessionPagerFilterManager mockSessionPagerFilterManager = getMock(SessionPagerFilterManager.class);
        expect(mockSessionPagerFilterManager.getCurrentObject()).andReturn(originalPagerFilter);

        final SearchActionHelper actionHelper = mockController.instantiate(SearchActionHelper.class);
        final PagerFilter pagerFilter = actionHelper.getPagerFilter();
        assertNotNull(pagerFilter);
        assertSame(originalPagerFilter, pagerFilter);
        assertEquals(max, pagerFilter.getMax());
    }

    /**
     * Make sure that we add a new pager to the session it does not exist.
     */
    @Test
    public void testGetPagerFilterMissing()
    {
        final long expectedTempMax = 0x687fab;

        final Preferences mockUserPreferences = getMock(Preferences.class);
        expect(mockUserPreferences.getLong(PreferenceKeys.USER_ISSUES_PER_PAGE))
                .andReturn(expectedTempMax);

        final SessionPagerFilterManager mockSessionPagerFilterManager = getMock(SessionPagerFilterManager.class);
        expect(mockSessionPagerFilterManager.getCurrentObject())
                .andReturn(null);

        mockSessionPagerFilterManager.setCurrentObject(isA(PagerFilter.class));
        expectLastCall();

        final SearchActionHelper actionHelper = mockController.instantiate(SearchActionHelper.class);

        final PagerFilter pagerFilter = actionHelper.getPagerFilter();
        assertNotNull(pagerFilter);
        assertEquals(expectedTempMax, pagerFilter.getMax());
    }

    /**
     * Make sure that the pager is returned with the correct tempMax when it is configured. 
     */
    @Test
    public void testGetPagerFilterWithTempMax()
    {
        // max is not the same as temp max
        final PagerFilter originalPagerFilter = new PagerFilter(4859);

        final SessionPagerFilterManager mockSessionPagerFilterManager = getMock(SessionPagerFilterManager.class);
        expect(mockSessionPagerFilterManager.getCurrentObject())
                .andReturn(originalPagerFilter);

        final SearchActionHelper actionHelper = mockController.instantiate(SearchActionHelper.class);

        final int tempMax = 90;
        actionHelper.setTempMax(tempMax);
        final PagerFilter pagerFilter = actionHelper.getPagerFilter();
        assertEquals(tempMax, pagerFilter.getMax());
        assertSame(originalPagerFilter, pagerFilter);
    }

    /**
     * Does reset with tempMax not set become a no-op.
     */
    @Test
    public void testResetPagerTempMax()
    {
        final Map<String, Object> sessionMap = new HashMap<String, Object>();
        mockController.addObjectInstance(new TestSessionSupplier(sessionMap));

        // max is not the same as temp max
        final int origMax = 0xfdaad3;
        final PagerFilter originalPagerFilter = new PagerFilter(origMax);
        sessionMap.put(SessionKeys.SEARCH_PAGER, originalPagerFilter);

        final SearchActionHelper actionHelper = mockController.instantiate(SearchActionHelper.class);
        actionHelper.resetPagerTempMax();

        assertEquals(origMax, originalPagerFilter.getMax());
        mockController.verify();
    }

    /**
     * Does reset with tempMax set reset the pager correctly. 
     */
    @Test
    public void testResetPagerTempMaxChange()
    {
        // max is not the same as temp max
        final int origMax = 0xfdaad3;
        final PagerFilter originalPagerFilter = new PagerFilter(origMax);

        final SessionPagerFilterManager mockSessionPagerFilterManager = getMock(SessionPagerFilterManager.class);
        expect(mockSessionPagerFilterManager.getCurrentObject())
                .andReturn(originalPagerFilter);

        final SearchActionHelper actionHelper = mockController.instantiate(SearchActionHelper.class);
        final int expectedTempMax = 0x673a;
        actionHelper.setTempMax(expectedTempMax);
        actionHelper.resetPagerTempMax();

        assertEquals(expectedTempMax, originalPagerFilter.getMax());
    }

    private static class TestSessionSupplier implements Supplier<Map<String, Object>>
    {
        private final Map<String, Object> session;

        public TestSessionSupplier(final Map<String, Object> session)
        {
            this.session = session;
        }

        public Map<String, Object> get()
        {
            return session;
        }
    }
}
