package com.atlassian.jira.jql.context;

import com.atlassian.jira.local.MockControllerTestCase;
import com.atlassian.jira.mock.ofbiz.MockGenericValue;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import com.atlassian.jira.issue.security.IssueSecuritySchemeManager;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import static com.atlassian.jira.jql.operand.SimpleLiteralFactory.createLiteral;
import com.atlassian.jira.jql.resolver.IssueSecurityLevelResolver;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.clause.TerminalClauseImpl;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.Operand;
import com.atlassian.query.operand.SingleValueOperand;
import com.atlassian.query.operator.Operator;
import com.opensymphony.user.User;
import mock.user.MockOSUser;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @since v4.0
 */
public class TestIssueSecurityLevelClauseContextFactory extends MockControllerTestCase
{
    private User theUser;

    @Test
    public void testCreator() throws Exception
    {
        IssueSecurityLevelClauseContextFactory.Creator creator =
                mockController.instantiate(IssueSecurityLevelClauseContextFactory.Creator.class);
        assertNotNull(creator.create());
    }

    @Test
    public void testGetSecurityLevelsFromClause() throws Exception
    {
        final SingleValueOperand operand = new SingleValueOperand("test");
        final List<QueryLiteral> literals = new ArrayList<QueryLiteral>();
        final List<GenericValue> levels = new ArrayList<GenericValue>();
        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.EQUALS, operand);

        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        jqlOperandResolver.getValues(theUser, operand, terminalClause);
        mockController.setReturnValue(literals);

        final IssueSecurityLevelResolver resolver = mockController.getMock(IssueSecurityLevelResolver.class);
        resolver.getIssueSecurityLevels(theUser, literals);
        mockController.setReturnValue(levels);

        IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);

        assertEquals(levels, factory.getSecurityLevelsFromClause(theUser, terminalClause));
        
        mockController.verify();
    }

    @Test
    public void testGetSecurityLevelsFromClauseNullLiterals() throws Exception
    {
        final SingleValueOperand operand = new SingleValueOperand("test");
        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.EQUALS, operand);

        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        jqlOperandResolver.getValues(theUser, operand, terminalClause);
        mockController.setReturnValue(null);

        IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);

        final List<GenericValue> result = factory.getSecurityLevelsFromClause(theUser, terminalClause);
        assertTrue(result.isEmpty());

        mockController.verify();
    }

    @Test
    public void testGetSecurityLevelsFromClauseNegative() throws Exception
    {
        final SingleValueOperand operand = new SingleValueOperand("test");
        final List<QueryLiteral> literals = Collections.singletonList(createLiteral("qwerty"));
        final List<GenericValue> levels = CollectionBuilder.<GenericValue>newBuilder(createMockSecurityLevel(1, "one"), createMockSecurityLevel(2, "two")).asMutableList();
        final List<GenericValue> expectedResult = Collections.<GenericValue>singletonList(createMockSecurityLevel(1, "three"));
        final List<GenericValue> allLevels = CollectionBuilder.<GenericValue>newBuilder().addAll(levels).addAll(expectedResult).asMutableList();

        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.NOT_EQUALS, operand);

        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        expect(jqlOperandResolver.getValues(theUser, operand, terminalClause)).andReturn(literals);

        final IssueSecurityLevelResolver resolver = mockController.getMock(IssueSecurityLevelResolver.class);
        expect(resolver.getIssueSecurityLevels(theUser, literals)).andReturn(levels);
        expect(resolver.getAllSecurityLevels(theUser)).andReturn(allLevels);

        IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);

        assertEquals(expectedResult, factory.getSecurityLevelsFromClause(theUser, terminalClause));

        mockController.verify();
    }

    @Test
    public void testGetSecurityLevelsFromClauseNegativeWithEmpty() throws Exception
    {
        final GenericValue mockLevel1 = createMockSecurityLevel(1, "one");
        final GenericValue mockLevel2 = createMockSecurityLevel(2, "two");

        final List<GenericValue> allLevels = CollectionBuilder.<GenericValue>newBuilder(mockLevel1, mockLevel2).asMutableList();
        final List<GenericValue> levels = CollectionBuilder.<GenericValue>newBuilder(null, mockLevel1, null).asMutableList();

        final SingleValueOperand operand = new SingleValueOperand("test");
        final List<QueryLiteral> literals = new ArrayList<QueryLiteral>();
        final List<GenericValue> expectedResult = Arrays.asList(mockLevel2);

        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.IS_NOT, operand);

        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        expect(jqlOperandResolver.getValues(theUser, operand, terminalClause)).andReturn(literals);

        final IssueSecurityLevelResolver resolver = mockController.getMock(IssueSecurityLevelResolver.class);
        expect(resolver.getIssueSecurityLevels(theUser, literals)).andReturn(levels);
        expect(resolver.getAllSecurityLevels(theUser)).andReturn(allLevels);

        final IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);
        assertEquals(expectedResult, factory.getSecurityLevelsFromClause(theUser, terminalClause));

        mockController.verify();
    }

    @Test
    public void testGetSecurityLevelsFromClauseNegativeWithEmptyOnly() throws Exception
    {
        final SingleValueOperand operand = new SingleValueOperand("test");
        final List<QueryLiteral> literals = new ArrayList<QueryLiteral>();
        final List<GenericValue> levels = CollectionBuilder.<GenericValue>newBuilder(null, null, null).asMutableList();
        final List<GenericValue> expectedResult = Arrays.asList(null, null, null);

        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.NOT_IN, operand);

        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        expect(jqlOperandResolver.getValues(theUser, operand, terminalClause)).andReturn(literals);

        final IssueSecurityLevelResolver resolver = mockController.getMock(IssueSecurityLevelResolver.class);
        expect(resolver.getIssueSecurityLevels(theUser, literals)).andReturn(levels);

        final IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);
        assertEquals(expectedResult, factory.getSecurityLevelsFromClause(theUser, terminalClause));

        mockController.verify();
    }

    @Test
    public void testGetProjectsForSecurityLevel() throws Exception
    {
        final GenericValue securityLevel = createMockSecurityLevel(100l, 543L);
        final GenericValue scheme = createMockSecurityScheme(543L);
        final GenericValue projectGV = createMockProject(666L);
        final Project project = new MockProject(666L);

        final IssueSecuritySchemeManager schemeManager = mockController.getMock(IssueSecuritySchemeManager.class);
        schemeManager.getScheme(543L);
        mockController.setReturnValue(scheme);
        schemeManager.getProjects(scheme);
        mockController.setReturnValue(Collections.singletonList(projectGV));

        final ProjectManager projectManager = mockController.getMock(ProjectManager.class);
        projectManager.getProjectObj(666L);
        mockController.setReturnValue(project);

        IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);

        final Collection<Project> projects = factory.getProjectsForSecurityLevel(securityLevel);
        assertEquals(1, projects.size());
        assertEquals(project, projects.iterator().next());

        mockController.verify();
    }

    @Test
    public void testGetProjectsForSecurityLevelThrowsGenericEntityException() throws Exception
    {
        final GenericValue securityLevel = createMockSecurityLevel(100l, 543L);

        final IssueSecuritySchemeManager schemeManager = mockController.getMock(IssueSecuritySchemeManager.class);
        schemeManager.getScheme(543L);
        mockController.setThrowable(new GenericEntityException());

        IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);

        final Collection<Project> projects = factory.getProjectsForSecurityLevel(securityLevel);
        assertTrue(projects.isEmpty());

        mockController.verify();
    }

    @Test
    public void testGetAssociatedProjectsFromClause() throws Exception
    {
        final GenericValue level1 = createMockSecurityLevel(1L, 1L);
        final GenericValue level2 = createMockSecurityLevel(2L, 1L);
        final Project project1 = new MockProject(11L);
        final Project project2 = new MockProject(22L);
        final Project project3 = new MockProject(33L);

        final Map<GenericValue, Collection<Project>> levelToProjectMap = MapBuilder.<GenericValue, Collection<Project>>newBuilder()
                .add(level1, CollectionBuilder.newBuilder(project1, project3).asList())
                .add(level2, CollectionBuilder.newBuilder(project2, project3).asList())
                .toMap();

        final IssueSecurityLevelClauseContextFactory factory = createFactory(levelToProjectMap);
        mockController.replay();

        final Set<Project> associatedProjects = factory.getAssociatedProjectsFromClause(theUser, null);
        assertEquals(3, associatedProjects.size());
        assertTrue(associatedProjects.contains(project1));
        assertTrue(associatedProjects.contains(project2));
        assertTrue(associatedProjects.contains(project3));
        
        mockController.verify();
    }

    @Test
    public void testGetClauseContextBadOperators() throws Exception
    {
        IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);

        for (Operator operator : Operator.values())
        {
            if (operator != Operator.EQUALS && operator != Operator.NOT_EQUALS &&
                operator != Operator.IN && operator != Operator.NOT_IN &&
                operator != Operator.IS && operator != Operator.IS_NOT)
            {
                final TerminalClause terminalClause = new TerminalClauseImpl("level", operator, "test");
                final ClauseContext clauseContext = factory.getClauseContext(theUser, terminalClause);
                assertEquals(ClauseContextImpl.createGlobalClauseContext(), clauseContext);
            }
        }

        mockController.verify();
    }

    @Test
    public void testGetContextFromClauseIsGlobalWhenNoneWhereCalculated() throws Exception
    {
        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.EQUALS, "test");
        final Set<Project> associatedProjects = Collections.emptySet();
        final IssueSecurityLevelClauseContextFactory factory = createFactory(associatedProjects);
        mockController.replay();

        final ClauseContext context = factory.getContextFromClause(theUser, terminalClause);
        final ClauseContext expectedContext = ClauseContextImpl.createGlobalClauseContext();
        assertEquals(expectedContext, context);

        mockController.verify();
    }

    @Test
    public void testGetContextFromClauseEquals() throws Exception
    {
        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.EQUALS, "test");
        final Set<Project> associatedProjects = Collections.<Project>singleton(new MockProject(666L));
        final IssueSecurityLevelClauseContextFactory factory = createFactory(associatedProjects);
        mockController.replay();

        final ClauseContext context = factory.getContextFromClause(theUser, terminalClause);
        assertEquals(1, context.getContexts().size());
        ProjectIssueTypeContext expected = createExpectedContext(666L);
        assertTrue(context.getContexts().contains(expected));

        mockController.verify();
    }

    @Test
    public void testGetClauseContextEmpty() throws Exception
    {
        final Operand operand = EmptyOperand.EMPTY;
        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.IS, operand);

        final List<QueryLiteral> literals = CollectionBuilder.<QueryLiteral>newBuilder(new QueryLiteral()).asList();
        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        jqlOperandResolver.getValues(theUser, operand, terminalClause);
        mockController.setReturnValue(literals);

        final IssueSecurityLevelResolver securityLevelResolver = mockController.getMock(IssueSecurityLevelResolver.class);
        securityLevelResolver.getIssueSecurityLevels(theUser, literals);
        mockController.setReturnValue(Collections.<GenericValue>singletonList(null));

        IssueSecurityLevelClauseContextFactory factory = mockController.instantiate(IssueSecurityLevelClauseContextFactory.class);

        assertEquals(ClauseContextImpl.createGlobalClauseContext(), factory.getClauseContext(theUser, terminalClause));
        
        mockController.verify();
    }

    @Test
    public void testGetClauseContextNotEmpty() throws Exception
    {
        final SingleValueOperand operand = new SingleValueOperand("test");
        final TerminalClause terminalClause = new TerminalClauseImpl("level", Operator.EQUALS, operand);
        final ClauseContext contextFromClause = new ClauseContextImpl(Collections.<ProjectIssueTypeContext>singleton(createExpectedContext(555L)));

        final IssueSecurityLevelClauseContextFactory factory = createFactory(contextFromClause);
        mockController.replay();

        assertEquals(contextFromClause, factory.getClauseContext(theUser, terminalClause));
        
        mockController.verify();
    }

    private ProjectIssueTypeContextImpl createExpectedContext(final long projectId)
    {
        return new ProjectIssueTypeContextImpl(new ProjectContextImpl(projectId), AllIssueTypesContext.INSTANCE);
    }

    private IssueSecurityLevelClauseContextFactory createFactory(final Map<GenericValue, Collection<Project>> associatedProjects)
    {
        final IssueSecurityLevelResolver resolver = mockController.getMock(IssueSecurityLevelResolver.class);
        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        final IssueSecuritySchemeManager schemeManager = mockController.getMock(IssueSecuritySchemeManager.class);
        final ProjectManager projectManager = mockController.getMock(ProjectManager.class);

        return new IssueSecurityLevelClauseContextFactory(resolver, jqlOperandResolver, schemeManager, projectManager)
        {
            @Override
            List<GenericValue> getSecurityLevelsFromClause(final User searcher, final TerminalClause terminalClause)
            {
                return new ArrayList<GenericValue>(associatedProjects.keySet());
            }

            @Override
            Collection<Project> getProjectsForSecurityLevel(final GenericValue securityLevel)
            {
                return associatedProjects.get(securityLevel);
            }
        };
    }

    private IssueSecurityLevelClauseContextFactory createFactory(final Set<Project> associatedProjects)
    {
        final IssueSecurityLevelResolver resolver = mockController.getMock(IssueSecurityLevelResolver.class);
        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        final IssueSecuritySchemeManager schemeManager = mockController.getMock(IssueSecuritySchemeManager.class);
        final ProjectManager projectManager = mockController.getMock(ProjectManager.class);

        return new IssueSecurityLevelClauseContextFactory(resolver, jqlOperandResolver, schemeManager, projectManager)
        {
            @Override
            Set<Project> getAssociatedProjectsFromClause(final User searcher, final TerminalClause terminalClause)
            {
                return associatedProjects;
            }
        };
    }

    private IssueSecurityLevelClauseContextFactory createFactory(final ClauseContext contextFromClause)
    {
        final IssueSecurityLevelResolver resolver = mockController.getMock(IssueSecurityLevelResolver.class);
        final JqlOperandResolver jqlOperandResolver = mockController.getMock(JqlOperandResolver.class);
        final IssueSecuritySchemeManager schemeManager = mockController.getMock(IssueSecuritySchemeManager.class);
        final ProjectManager projectManager = mockController.getMock(ProjectManager.class);

        return new IssueSecurityLevelClauseContextFactory(resolver, jqlOperandResolver, schemeManager, projectManager)
        {
            @Override
            ClauseContext getContextFromClause(final User searcher, final TerminalClause terminalClause)
            {
                return contextFromClause;
            }
        };
    }

    private MockGenericValue createMockSecurityLevel(final Long id, final Long schemeId)
    {
        return new MockGenericValue("IssueSecurityLevel", MapBuilder.newBuilder().add("id", id).add("scheme", schemeId).toMap());
    }

    private MockGenericValue createMockSecurityScheme(final Long id)
    {
        return new MockGenericValue("SecurityScheme", MapBuilder.newBuilder().add("id", id).toMap());
    }

    private MockGenericValue createMockProject(final Long id)
    {
        return new MockGenericValue("Project", MapBuilder.newBuilder().add("id", id).toMap());
    }

    private MockGenericValue createMockSecurityLevel(final long id, final String name)
    {
        return new MockGenericValue("IssueSecurityLevel", MapBuilder.newBuilder().add("id", id).add("name", name).toMap());
    }

    @Before
    public void setUp() throws Exception
    {
        theUser = new MockOSUser("fred");
    }

    @After
    public void tearDown() throws Exception
    {
        theUser = null;
    }
}
