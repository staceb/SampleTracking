package com.atlassian.jira.imports.project;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.external.ExternalException;
import com.atlassian.jira.external.beans.ExternalComponent;
import com.atlassian.jira.external.beans.ExternalCustomField;
import com.atlassian.jira.external.beans.ExternalProject;
import com.atlassian.jira.external.beans.ExternalProjectRoleActor;
import com.atlassian.jira.external.beans.ExternalUser;
import com.atlassian.jira.external.beans.ExternalVersion;
import com.atlassian.jira.imports.project.core.BackupOverview;
import com.atlassian.jira.imports.project.core.BackupProject;
import com.atlassian.jira.imports.project.core.BackupProjectImpl;
import com.atlassian.jira.imports.project.core.BackupSystemInformation;
import com.atlassian.jira.imports.project.core.BackupSystemInformationImpl;
import com.atlassian.jira.imports.project.core.EntityRepresentation;
import com.atlassian.jira.imports.project.core.EntityRepresentationImpl;
import com.atlassian.jira.imports.project.core.MappingResult;
import com.atlassian.jira.imports.project.core.ProjectImportData;
import com.atlassian.jira.imports.project.core.ProjectImportDataImpl;
import com.atlassian.jira.imports.project.core.ProjectImportOptions;
import com.atlassian.jira.imports.project.core.ProjectImportOptionsImpl;
import com.atlassian.jira.imports.project.core.ProjectImportResults;
import com.atlassian.jira.imports.project.core.ProjectImportResultsImpl;
import com.atlassian.jira.imports.project.customfield.ExternalCustomFieldConfiguration;
import com.atlassian.jira.imports.project.handler.AbortImportException;
import com.atlassian.jira.imports.project.handler.AttachmentFileValidatorHandler;
import com.atlassian.jira.imports.project.handler.ChainedSaxHandler;
import com.atlassian.jira.imports.project.handler.CustomFieldMapperHandler;
import com.atlassian.jira.imports.project.handler.CustomFieldOptionsMapperHandler;
import com.atlassian.jira.imports.project.handler.CustomFieldValueValidatorHandler;
import com.atlassian.jira.imports.project.handler.GroupMapperHandler;
import com.atlassian.jira.imports.project.handler.IssueComponentMapperHandler;
import com.atlassian.jira.imports.project.handler.IssueLinkMapperHandler;
import com.atlassian.jira.imports.project.handler.IssueMapperHandler;
import com.atlassian.jira.imports.project.handler.IssuePartitonHandler;
import com.atlassian.jira.imports.project.handler.IssueRelatedEntitiesPartionHandler;
import com.atlassian.jira.imports.project.handler.IssueTypeMapperHandler;
import com.atlassian.jira.imports.project.handler.IssueVersionMapperHandler;
import com.atlassian.jira.imports.project.handler.ProjectIssueSecurityLevelMapperHandler;
import com.atlassian.jira.imports.project.handler.ProjectMapperHandler;
import com.atlassian.jira.imports.project.handler.ProjectRoleActorMapperHandler;
import com.atlassian.jira.imports.project.handler.RegisterUserMapperHandler;
import com.atlassian.jira.imports.project.handler.RequiredProjectRolesMapperHandler;
import com.atlassian.jira.imports.project.handler.SimpleEntityMapperHandler;
import com.atlassian.jira.imports.project.handler.UserMapperHandler;
import com.atlassian.jira.imports.project.mapper.AutomaticDataMapper;
import com.atlassian.jira.imports.project.mapper.CustomFieldMapper;
import com.atlassian.jira.imports.project.mapper.ProjectImportMapper;
import com.atlassian.jira.imports.project.mapper.ProjectImportMapperImpl;
import com.atlassian.jira.imports.project.mapper.SimpleProjectImportIdMapper;
import com.atlassian.jira.imports.project.mapper.SimpleProjectImportIdMapperImpl;
import com.atlassian.jira.imports.project.mapper.UserMapper;
import com.atlassian.jira.imports.project.parser.AttachmentParser;
import com.atlassian.jira.imports.project.parser.ChangeGroupParser;
import com.atlassian.jira.imports.project.parser.ChangeItemParser;
import com.atlassian.jira.imports.project.parser.CommentParser;
import com.atlassian.jira.imports.project.parser.CustomFieldValueParser;
import com.atlassian.jira.imports.project.parser.CustomFieldValueParserImpl;
import com.atlassian.jira.imports.project.parser.IssueLinkParser;
import com.atlassian.jira.imports.project.parser.IssueParser;
import com.atlassian.jira.imports.project.parser.NodeAssociationParser;
import com.atlassian.jira.imports.project.parser.ProjectRoleActorParser;
import com.atlassian.jira.imports.project.parser.TrackbackParser;
import com.atlassian.jira.imports.project.parser.UserAssociationParser;
import com.atlassian.jira.imports.project.taskprogress.TaskProgressInterval;
import com.atlassian.jira.imports.project.taskprogress.TaskProgressProcessor;
import com.atlassian.jira.imports.project.util.MockProjectImportTemporaryFiles;
import com.atlassian.jira.imports.project.util.ProjectImportTemporaryFiles;
import com.atlassian.jira.imports.project.validation.CustomFieldMapperValidator;
import com.atlassian.jira.imports.project.validation.CustomFieldOptionMapperValidator;
import com.atlassian.jira.imports.project.validation.GroupMapperValidator;
import com.atlassian.jira.imports.project.validation.IssueLinkTypeMapperValidator;
import com.atlassian.jira.imports.project.validation.IssueSecurityLevelValidator;
import com.atlassian.jira.imports.project.validation.IssueTypeMapperValidator;
import com.atlassian.jira.imports.project.validation.PriorityMapperValidator;
import com.atlassian.jira.imports.project.validation.ProjectImportValidators;
import com.atlassian.jira.imports.project.validation.ProjectRoleActorMapperValidator;
import com.atlassian.jira.imports.project.validation.ProjectRoleMapperValidator;
import com.atlassian.jira.imports.project.validation.ResolutionMapperValidator;
import com.atlassian.jira.imports.project.validation.StatusMapperValidator;
import com.atlassian.jira.imports.project.validation.UserMapperValidator;
import com.atlassian.jira.imports.xml.BackupXmlParser;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.issue.label.OfBizLabelStore;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.worklog.OfBizWorklogStore;
import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.roles.ProjectRoleManager;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.jira.util.concurrent.BoundedExecutor;
import com.atlassian.jira.web.bean.MockI18nBean;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.P;
import org.easymock.EasyMock;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;
import org.junit.Before;
import org.junit.Test;
import org.ofbiz.core.entity.model.MockModelEntity;
import org.ofbiz.core.entity.model.ModelEntity;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @since v3.13
 */
public class TestDefaultProjectImportManager extends ListeningTestCase
{
    private MapBuilder<String, CustomFieldValueParser> entities = MapBuilder.newBuilder();

    @Before
    public void setUp() throws Exception
    {
        entities.add(CustomFieldValueParser.CUSTOM_FIELD_VALUE_ENTITY_NAME, new CustomFieldValueParserImpl());
    }

    @Test
    public void testGetBackupOverview() throws IOException, SAXException
    {
        final String filename = "/somerubbish";
        final Mock mockBackupXmlParser = new Mock(BackupXmlParser.class);
        mockBackupXmlParser.setStrict(true);
        mockBackupXmlParser.expectVoid("parseBackupXml", P.args(P.eq(filename), P.IS_ANYTHING));
        final BackupXmlParser backupXmlParser = (BackupXmlParser) mockBackupXmlParser.proxy();
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(backupXmlParser, null, null, null, null,
            null, null, null, null, null, null, null, null, null)
        {
            @Override
            int getTotalEntitiesCount()
            {
                return 1;
            }
        };
        final BackupOverview backupOverview = defaultProjectImportManager.getBackupOverview(filename, null, new MockI18nBean());
        assertNotNull(backupOverview);

        mockBackupXmlParser.verify();
    }

    @Test
    public void testGetBackupOverviewIllegalArgument() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            defaultProjectImportManager.getBackupOverview(null, null, new MockI18nBean());
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testXmlPartitioningHappyPath() throws IOException, SAXException
    {
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        final String fileName = "/test/path";
        final ProjectImportOptionsImpl projectImportOptions = new ProjectImportOptionsImpl(fileName, fileName);
        final BackupSystemInformation backupSystemInformation = new BackupSystemInformationImpl("123", "Prof", Collections.EMPTY_LIST, true,
            Collections.EMPTY_MAP, 0);

        final IssuePartitonHandler issuePartitonHandler = new IssuePartitonHandler(null, null, new MockModelEntity(IssueParser.ISSUE_ENTITY_NAME),
            null);
        final IssueRelatedEntitiesPartionHandler issueRelatedEntitiesPartionHandler = new IssueRelatedEntitiesPartionHandler(null, null, null,
            Collections.EMPTY_LIST, null);
        final IssueRelatedEntitiesPartionHandler customFieldValuesPartitionHandler = new IssueRelatedEntitiesPartionHandler(null, null, null,
            Collections.EMPTY_LIST, null);
        final IssueRelatedEntitiesPartionHandler fileAttachmentPartitionHandler = new IssueRelatedEntitiesPartionHandler(null, null, null,
            Collections.EMPTY_LIST, null);

        final MockControl mockChainedSaxHandlerControl = MockClassControl.createControl(ChainedSaxHandler.class);
        final ChainedSaxHandler mockChainedSaxHandler = (ChainedSaxHandler) mockChainedSaxHandlerControl.getMock();
        mockChainedSaxHandler.registerHandler(issuePartitonHandler);
        mockChainedSaxHandler.registerHandler(customFieldValuesPartitionHandler);
        mockChainedSaxHandler.registerHandler(issueRelatedEntitiesPartionHandler);
        mockChainedSaxHandler.registerHandler(fileAttachmentPartitionHandler);

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final UserMapperHandler userMapperHandler = new UserMapperHandler(projectImportOptions, backupProject, projectImportMapper.getUserMapper());
        final GroupMapperHandler groupMapperHandler = new GroupMapperHandler(backupProject, projectImportMapper.getGroupMapper());
        final IssueMapperHandler issueMapperHandler = new IssueMapperHandler(backupProject, projectImportMapper);
        final ProjectIssueSecurityLevelMapperHandler securityLevelMapperHandler = new ProjectIssueSecurityLevelMapperHandler(backupProject,
            projectImportMapper.getIssueSecurityLevelMapper());
        final IssueTypeMapperHandler issueTypeHandler = new IssueTypeMapperHandler(projectImportMapper.getIssueTypeMapper());
        final SimpleEntityMapperHandler priorityHandler = new SimpleEntityMapperHandler(SimpleEntityMapperHandler.PRIORITY_ENTITY_NAME,
            projectImportMapper.getPriorityMapper());
        final SimpleEntityMapperHandler resolutionHandler = new SimpleEntityMapperHandler(SimpleEntityMapperHandler.RESOLUTION_ENTITY_NAME,
            projectImportMapper.getResolutionMapper());
        final SimpleEntityMapperHandler statusHandler = new SimpleEntityMapperHandler(SimpleEntityMapperHandler.STATUS_ENTITY_NAME,
            projectImportMapper.getStatusMapper());
        final CustomFieldMapperHandler customFieldHandler = new CustomFieldMapperHandler(backupProject, projectImportMapper.getCustomFieldMapper(), entities.toMap());
        final ProjectMapperHandler projectMapperHandler = new ProjectMapperHandler(projectImportMapper.getProjectMapper());
        final CustomFieldOptionsMapperHandler customFieldOptionMapperHandler = new CustomFieldOptionsMapperHandler(
            projectImportMapper.getCustomFieldOptionMapper());
        final SimpleEntityMapperHandler projectRoleRegistrationMapperHandler = new SimpleEntityMapperHandler(
            SimpleEntityMapperHandler.PROJECT_ROLE_ENTITY_NAME, projectImportMapper.getProjectRoleMapper());
        final RequiredProjectRolesMapperHandler requiredProjectRolesMapperHandler = new RequiredProjectRolesMapperHandler(backupProject,
            projectImportMapper.getProjectMapper());
        final IssueVersionMapperHandler issueVersionMapperHandler = new IssueVersionMapperHandler(backupProject,
            projectImportMapper.getVersionMapper());
        final IssueComponentMapperHandler issueComponentMapperHandler = new IssueComponentMapperHandler(backupProject,
            projectImportMapper.getComponentMapper());
        final IssueLinkMapperHandler issueLinkMapperHandler = new IssueLinkMapperHandler(backupProject, backupSystemInformation, null,
            projectImportMapper.getIssueLinkTypeMapper());
        final RegisterUserMapperHandler registerUserMapperHandler = new RegisterUserMapperHandler(projectImportMapper.getUserMapper());
        final ProjectRoleActorMapperHandler projectRoleActorMapperHandler = new ProjectRoleActorMapperHandler(backupProject,
            projectImportMapper.getProjectRoleActorMapper());
        mockChainedSaxHandler.registerHandler(userMapperHandler);
        mockChainedSaxHandler.registerHandler(groupMapperHandler);
        mockChainedSaxHandler.registerHandler(issueMapperHandler);
        mockChainedSaxHandler.registerHandler(securityLevelMapperHandler);
        mockChainedSaxHandler.registerHandler(issueTypeHandler);
        mockChainedSaxHandler.registerHandler(priorityHandler);
        mockChainedSaxHandler.registerHandler(resolutionHandler);
        mockChainedSaxHandler.registerHandler(statusHandler);
        mockChainedSaxHandler.registerHandler(customFieldHandler);
        mockChainedSaxHandler.registerHandler(projectMapperHandler);
        mockChainedSaxHandler.registerHandler(customFieldOptionMapperHandler);
        mockChainedSaxHandler.registerHandler(projectRoleRegistrationMapperHandler);
        mockChainedSaxHandler.registerHandler(requiredProjectRolesMapperHandler);
        mockChainedSaxHandler.registerHandler(issueVersionMapperHandler);
        mockChainedSaxHandler.registerHandler(issueComponentMapperHandler);
        mockChainedSaxHandler.registerHandler(issueLinkMapperHandler);
        mockChainedSaxHandler.registerHandler(registerUserMapperHandler);
        mockChainedSaxHandler.registerHandler(projectRoleActorMapperHandler);

        mockChainedSaxHandlerControl.replay();

        final Mock mockBackupXmlParser = new Mock(BackupXmlParser.class);
        mockBackupXmlParser.setStrict(true);
        mockBackupXmlParser.expectVoid("parseBackupXml", P.args(P.eq(fileName), P.IS_ANYTHING));

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(
            (BackupXmlParser) mockBackupXmlParser.proxy(), null, null, null, null, null, null, null, null, null, null, null, null, null)
        {
            @Override
            String getApplicationEncoding()
            {
                return "UTF-8";
            }

            @Override
            IssuePartitonHandler getIssuePartitioner(final PrintWriter issueFileWriter, final BackupProject backupProject, final String encoding)
            {
                return issuePartitonHandler;
            }

            @Override
            IssueRelatedEntitiesPartionHandler getIssueRelatedEntitesHandler(final PrintWriter issueRelatedEntitiesWriter, final PrintWriter changeItemEntitiesWriter, final BackupProject backupProject, final String encoding)
            {
                return issueRelatedEntitiesPartionHandler;
            }

            @Override
            IssueRelatedEntitiesPartionHandler getCustomFieldValuesHandler(final PrintWriter customFieldValuesWriter, final BackupProject backupProject, final String encoding)
            {
                return customFieldValuesPartitionHandler;
            }

            @Override
            IssueRelatedEntitiesPartionHandler getFileAttachmentHandler(final PrintWriter customFieldValuesWriter, final BackupProject backupProject, final String encoding)
            {
                return fileAttachmentPartitionHandler;
            }

            @Override
            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return mockChainedSaxHandler;
            }

            @Override
            IssueMapperHandler getIssueMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return issueMapperHandler;
            }

            @Override
            IssueTypeMapperHandler getIssueTypeMapperHandler(final ProjectImportMapper projectImportMapper)
            {
                return issueTypeHandler;
            }

            @Override
            SimpleEntityMapperHandler getPriorityMapperHandler(final ProjectImportMapper projectImportMapper)
            {
                return priorityHandler;
            }

            @Override
            ProjectIssueSecurityLevelMapperHandler getProjectIssueSecurityLevelMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return securityLevelMapperHandler;
            }

            @Override
            SimpleEntityMapperHandler getResolutionMapperHandler(final ProjectImportMapper projectImportMapper)
            {
                return resolutionHandler;
            }

            @Override
            SimpleEntityMapperHandler getStatusMapperHandler(final ProjectImportMapper projectImportMapper)
            {
                return statusHandler;
            }

            @Override
            UserMapperHandler getUserMapperHandler(final ProjectImportOptions projectImportOptions, final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return userMapperHandler;
            }

            @Override
            GroupMapperHandler getGroupMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return groupMapperHandler;
            }

            @Override
            CustomFieldMapperHandler getCustomFieldMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return customFieldHandler;
            }

            @Override
            ProjectMapperHandler getProjectMapperHandler(final ProjectImportMapper projectImportMapper)
            {
                return projectMapperHandler;
            }

            @Override
            CustomFieldOptionsMapperHandler getCustomFieldOptionMapperHandler(final ProjectImportMapper projectImportMapper)
            {
                return customFieldOptionMapperHandler;
            }

            @Override
            SimpleEntityMapperHandler getProjectRoleRegistrationHandler(final ProjectImportMapper projectImportMapper)
            {
                return projectRoleRegistrationMapperHandler;
            }

            @Override
            RequiredProjectRolesMapperHandler getRequiredProjectRolesMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return requiredProjectRolesMapperHandler;
            }

            @Override
            IssueVersionMapperHandler getIssueVersionMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return issueVersionMapperHandler;
            }

            @Override
            IssueComponentMapperHandler getIssueComponentMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return issueComponentMapperHandler;
            }

            @Override
            IssueLinkMapperHandler getIssueLinkMapperHandler(final BackupProject backupProject, final BackupSystemInformation backupSystemInformation, final ProjectImportMapper projectImportMapper)
            {
                return issueLinkMapperHandler;
            }

            @Override
            RegisterUserMapperHandler getRegisterUserMapperHandler(final ProjectImportMapper projectImportMapper)
            {
                return registerUserMapperHandler;
            }

            @Override
            void populateCustomFieldMapperOldValues(final BackupProject backupProject, final CustomFieldMapper customFieldMapper)
            {
            // do nothing
            }

            @Override
            void populateVersionMapper(final SimpleProjectImportIdMapper versionMapper, final Map newVersions)
            {
            // do nothing
            }

            @Override
            void populateComponentMapper(final SimpleProjectImportIdMapper componentMapper, final Map newComponents)
            {
            // do nothing
            }

            @Override
            ProjectRoleActorMapperHandler getProjectRoleActorMapperHandler(final BackupProject backupProject, final ProjectImportMapper projectImportMapper)
            {
                return projectRoleActorMapperHandler;
            }
        };
        final ProjectImportData backupParsingResult = defaultProjectImportManager.getProjectImportData(projectImportOptions, backupProject,
            backupSystemInformation, null);

        // Test that the files exist
        assertTrue(new File(backupParsingResult.getPathToCustomFieldValuesXml()).exists());
        assertTrue(new File(backupParsingResult.getPathToIssueRelatedEntitiesXml()).exists());
        assertTrue(new File(backupParsingResult.getPathToIssuesXml()).exists());
        assertTrue(new File(backupParsingResult.getPathToFileAttachmentXml()).exists());

        mockChainedSaxHandlerControl.verify();
    }

    @Test
    public void testGetCustomFieldValuesHandler()
    {
        final AtomicInteger callCount = new AtomicInteger(0);
        final CustomFieldManager mockCustomFieldManager = createMock(CustomFieldManager.class);
        expect(mockCustomFieldManager.getCustomFieldTypes()).andReturn(Collections.emptyList());
        replay(mockCustomFieldManager);

        final DefaultProjectImportManager manager = new DefaultProjectImportManager(null, null, null, null, null, mockCustomFieldManager, null, null, null, null, null,
            null, null, null)
        {
            @Override
            ModelEntity getModelEntity(final String entityName)
            {
                callCount.incrementAndGet();
                assertEquals(CustomFieldValueParser.CUSTOM_FIELD_VALUE_ENTITY_NAME, entityName);
                return new MockModelEntity(CustomFieldValueParser.CUSTOM_FIELD_VALUE_ENTITY_NAME);
            }
        };

        final IssueRelatedEntitiesPartionHandler handler = manager.getCustomFieldValuesHandler(null, null, "UTF-16");
        assertNotNull(handler);
        assertEquals(1, handler.getRegisteredHandlers().size());
        assertNotNull(handler.getRegisteredHandlers().get(CustomFieldValueParser.CUSTOM_FIELD_VALUE_ENTITY_NAME));
        assertEquals(1, callCount.get());
        verify(mockCustomFieldManager);
    }

    @Test
    public void testGetIssueRelatedEntitesHandler()
    {
        final DefaultProjectImportManager manager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null, null, null, null,
            null, null, null)
        {
            @Override
            ModelEntity getModelEntity(final String entityName)
            {
                return new MockModelEntity(entityName);
            }
        };

        final IssueRelatedEntitiesPartionHandler handler = manager.getIssueRelatedEntitesHandler(null, null, null, "UTF-16");
        assertNotNull(handler);
        final Map registeredHandlers = handler.getRegisteredHandlers();
        assertEquals(9, registeredHandlers.size());
        assertNotNull(handler.getRegisteredHandlers().get(NodeAssociationParser.NODE_ASSOCIATION_ENTITY_NAME));
        assertNotNull(handler.getRegisteredHandlers().get(IssueLinkParser.ISSUE_LINK_ENTITY_NAME));
        assertNotNull(handler.getRegisteredHandlers().get(CommentParser.COMMENT_ENTITY_NAME));
        assertNotNull(handler.getRegisteredHandlers().get(ChangeGroupParser.CHANGE_GROUP_ENTITY_NAME));
        assertNotNull(handler.getRegisteredHandlers().get(ChangeItemParser.CHANGE_ITEM_ENTITY_NAME));
        assertNotNull(handler.getRegisteredHandlers().get(TrackbackParser.TRACKBACK_ENTITY_NAME));
        assertNotNull(handler.getRegisteredHandlers().get(OfBizWorklogStore.WORKLOG_ENTITY));
        assertNotNull(handler.getRegisteredHandlers().get(UserAssociationParser.USER_ASSOCIATION_ENTITY_NAME));
        assertNotNull(handler.getRegisteredHandlers().get(OfBizLabelStore.TABLE));
    }

    @Test
    public void testGetFileAttachmentEntitesHandler()
    {
        final DefaultProjectImportManager manager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null, null, null, null,
            null, null, null)
        {
            @Override
            ModelEntity getModelEntity(final String entityName)
            {
                return new MockModelEntity(entityName);
            }
        };

        final IssueRelatedEntitiesPartionHandler handler = manager.getFileAttachmentHandler(null, null, "UTF-16");
        assertNotNull(handler);
        final Map registeredHandlers = handler.getRegisteredHandlers();
        assertEquals(1, registeredHandlers.size());
        assertNotNull(handler.getRegisteredHandlers().get(AttachmentParser.ATTACHMENT_ENTITY_NAME));
    }

    @Test
    public void testGetIssuePartitioner()
    {
        final AtomicInteger callCount = new AtomicInteger(0);
        final DefaultProjectImportManager manager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null, null, null, null,
            null, null, null)
        {
            @Override
            ModelEntity getModelEntity(final String entityName)
            {
                callCount.incrementAndGet();
                assertEquals(IssueParser.ISSUE_ENTITY_NAME, entityName);
                return new MockModelEntity(IssueParser.ISSUE_ENTITY_NAME);
            }
        };

        final IssuePartitonHandler handler = manager.getIssuePartitioner(null, null, "UTF-16");
        assertEquals(1, callCount.get());
        assertNotNull(handler);
    }

    @Test
    public void testGetStatusMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final SimpleEntityMapperHandler handler = new SimpleEntityMapperHandler(SimpleEntityMapperHandler.STATUS_ENTITY_NAME,
            projectImportMapper.getStatusMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final SimpleEntityMapperHandler simpleEntityMapperHandler = projectImportManager.getStatusMapperHandler(projectImportMapper);
        assertEquals(handler, simpleEntityMapperHandler);
    }

    @Test
    public void testGetRegisterUserMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final RegisterUserMapperHandler handler = new RegisterUserMapperHandler(projectImportMapper.getUserMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final RegisterUserMapperHandler registerUserMapperHandler = projectImportManager.getRegisterUserMapperHandler(projectImportMapper);
        assertEquals(handler, registerUserMapperHandler);
    }

    @Test
    public void testGetResolutionMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final SimpleEntityMapperHandler handler = new SimpleEntityMapperHandler(SimpleEntityMapperHandler.RESOLUTION_ENTITY_NAME,
            projectImportMapper.getResolutionMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final SimpleEntityMapperHandler simpleEntityMapperHandler = projectImportManager.getResolutionMapperHandler(projectImportMapper);
        assertEquals(handler, simpleEntityMapperHandler);
    }

    @Test
    public void testGetIssueVersionMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final IssueVersionMapperHandler handler = new IssueVersionMapperHandler(null, projectImportMapper.getVersionMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final IssueVersionMapperHandler versionMapperHandler = projectImportManager.getIssueVersionMapperHandler(null, projectImportMapper);
        assertEquals(handler, versionMapperHandler);
    }

    @Test
    public void testGetIssueComponentMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final IssueComponentMapperHandler handler = new IssueComponentMapperHandler(null, projectImportMapper.getComponentMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final IssueComponentMapperHandler componentMapperHandler = projectImportManager.getIssueComponentMapperHandler(null, projectImportMapper);
        assertEquals(handler, componentMapperHandler);
    }

    @Test
    public void testGetProjectRoleRegistrationHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final SimpleEntityMapperHandler handler = new SimpleEntityMapperHandler(SimpleEntityMapperHandler.PROJECT_ROLE_ENTITY_NAME,
            projectImportMapper.getProjectRoleMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final SimpleEntityMapperHandler simpleEntityMapperHandler = projectImportManager.getProjectRoleRegistrationHandler(projectImportMapper);
        assertEquals(handler, simpleEntityMapperHandler);
    }

    @Test
    public void testGetRequiredProjectRolesMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final RequiredProjectRolesMapperHandler expectedMapperHandler = new RequiredProjectRolesMapperHandler(null,
            projectImportMapper.getProjectRoleMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final RequiredProjectRolesMapperHandler mapperHandler = projectImportManager.getRequiredProjectRolesMapperHandler(null, projectImportMapper);
        assertEquals(expectedMapperHandler, mapperHandler);
    }

    @Test
    public void testGetPriorityMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final SimpleEntityMapperHandler handler = new SimpleEntityMapperHandler(SimpleEntityMapperHandler.PRIORITY_ENTITY_NAME,
            projectImportMapper.getPriorityMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final SimpleEntityMapperHandler simpleEntityMapperHandler = projectImportManager.getPriorityMapperHandler(projectImportMapper);
        assertEquals(handler, simpleEntityMapperHandler);
    }

    @Test
    public void testGetIssueTypeMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final IssueTypeMapperHandler handler = new IssueTypeMapperHandler(projectImportMapper.getIssueTypeMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final IssueTypeMapperHandler issueTypeMapperHandler = projectImportManager.getIssueTypeMapperHandler(projectImportMapper);
        assertEquals(handler, issueTypeMapperHandler);
    }

    @Test
    public void testGetProjectIssueSecurityLevelMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final ProjectIssueSecurityLevelMapperHandler expectedMapperHandler = new ProjectIssueSecurityLevelMapperHandler(null,
            projectImportMapper.getIssueSecurityLevelMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final ProjectIssueSecurityLevelMapperHandler mapperHandler = projectImportManager.getProjectIssueSecurityLevelMapperHandler(null,
            projectImportMapper);
        assertEquals(expectedMapperHandler, mapperHandler);
    }

    @Test
    public void testGetIssueMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final IssueMapperHandler expectedMapperHandler = new IssueMapperHandler(null, projectImportMapper);
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final IssueMapperHandler mapperHandler = projectImportManager.getIssueMapperHandler(null, projectImportMapper);
        assertEquals(expectedMapperHandler, mapperHandler);
    }

    @Test
    public void testGetUserMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final UserMapperHandler expectedMapperHandler = new UserMapperHandler(null, null, projectImportMapper.getUserMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final UserMapperHandler mapperHandler = projectImportManager.getUserMapperHandler(null, null, projectImportMapper);
        assertEquals(expectedMapperHandler, mapperHandler);
    }

    @Test
    public void testGetCustomFieldMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final CustomFieldMapperHandler expectedMapperHandler = new CustomFieldMapperHandler(null, projectImportMapper.getCustomFieldMapper(), entities.toMap());

        final CustomFieldManager mockCustomFieldManager = createMock(CustomFieldManager.class);
        expect(mockCustomFieldManager.getCustomFieldTypes()).andReturn(Collections.emptyList());
        replay(mockCustomFieldManager);

        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, mockCustomFieldManager, null, null,
            null, null, null, null, null, null);
        final CustomFieldMapperHandler mapperHandler = projectImportManager.getCustomFieldMapperHandler(null, projectImportMapper);
        assertEquals(expectedMapperHandler, mapperHandler);
        verify(mockCustomFieldManager);
    }

    @Test
    public void testGetProjectMapperHandler()
    {
        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final ProjectMapperHandler expectedMapperHandler = new ProjectMapperHandler(projectImportMapper.getProjectMapper());
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        final ProjectMapperHandler mapperHandler = projectImportManager.getProjectMapperHandler(projectImportMapper);
        assertEquals(expectedMapperHandler, mapperHandler);
    }

    @Test
    public void testGetCustomFieldValueValidatorHandler()
    {
        // Set up the BackupProject
        final ExternalProject externalProject = new ExternalProject();
        externalProject.setId("10");
        final BackupProject backupProject = new BackupProjectImpl(externalProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final CustomFieldManager mockCustomFieldManager = createMock(CustomFieldManager.class);
        expect(mockCustomFieldManager.getCustomFieldTypes()).andReturn(Collections.emptyList());
        replay(mockCustomFieldManager);

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final CustomFieldValueValidatorHandler expectedMapperHandler = new CustomFieldValueValidatorHandler(backupProject, projectImportMapper, mockCustomFieldManager, null);

        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, mockCustomFieldManager, null, null,
            null, null, null, null, null, null);
        final CustomFieldValueValidatorHandler mapperHandler = projectImportManager.getCustomFieldValueValidatorHandler(backupProject,
            projectImportMapper);
        assertEquals(expectedMapperHandler, mapperHandler);
        verify(mockCustomFieldManager);
    }

    @Test
    public void testPopulateCustomFieldMapperOldValues()
    {
        final ExternalCustomFieldConfiguration configuration1 = new ExternalCustomFieldConfiguration(null, null, new ExternalCustomField("11111",
            "CustomField1", "df.df.df:eee"), "321");
        final ExternalCustomFieldConfiguration configuration2 = new ExternalCustomFieldConfiguration(null, null, new ExternalCustomField("22222",
            "CustomField2", "df.df.df:fff"), "321");
        final List customFIeldConfigs = EasyList.build(configuration1, configuration2);
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            customFIeldConfigs, Collections.EMPTY_LIST);
        final CustomFieldMapper customFieldMapper = new CustomFieldMapper();

        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        projectImportManager.populateCustomFieldMapperOldValues(backupProject, customFieldMapper);

        assertEquals(2, customFieldMapper.getRegisteredOldIds().size());
        assertTrue(customFieldMapper.getRegisteredOldIds().contains("11111"));
        assertTrue(customFieldMapper.getRegisteredOldIds().contains("22222"));
        assertEquals("CustomField1", customFieldMapper.getKey("11111"));
        assertEquals("CustomField2", customFieldMapper.getKey("22222"));
    }

    @Test
    public void testPopulateVersionMapperOldValues()
    {
        final ExternalVersion version1 = new ExternalVersion("Version1");
        version1.setId("11111");
        final ExternalVersion version2 = new ExternalVersion("Version2");
        version2.setId("22222");
        final List versions = EasyList.build(version1, version2);
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), versions, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);
        final SimpleProjectImportIdMapperImpl versionMapper = new SimpleProjectImportIdMapperImpl();

        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        projectImportManager.populateVersionMapperOldValues(backupProject, versionMapper);

        assertEquals(2, versionMapper.getRegisteredOldIds().size());
        assertTrue(versionMapper.getRegisteredOldIds().contains("11111"));
        assertTrue(versionMapper.getRegisteredOldIds().contains("22222"));
        assertEquals("Version1", versionMapper.getKey("11111"));
        assertEquals("Version2", versionMapper.getKey("22222"));
    }

    @Test
    public void testPopulateVersionMapper() throws Exception
    {
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);

        final SimpleProjectImportIdMapperImpl versionMapper = new SimpleProjectImportIdMapperImpl();
        final MockControl mockVersionControl = MockControl.createStrictControl(Version.class);
        final Version mockVersion = (Version) mockVersionControl.getMock();
        mockVersion.getId();
        mockVersionControl.setReturnValue(new Long(1));
        mockVersionControl.replay();

        final MockControl mockVersionControl2 = MockControl.createStrictControl(Version.class);
        final Version mockVersion2 = (Version) mockVersionControl2.getMock();
        mockVersion2.getId();
        mockVersionControl2.setReturnValue(new Long(2));
        mockVersionControl2.replay();

        projectImportManager.populateVersionMapper(versionMapper, EasyMap.build("12", mockVersion, "14", mockVersion2));

        assertEquals("1", versionMapper.getMappedId("12"));
        assertEquals("2", versionMapper.getMappedId("14"));
        mockVersionControl.verify();
        mockVersionControl2.verify();
    }

    @Test
    public void testPopulateComponentMapperOldValues()
    {
        final ExternalComponent component1 = new ExternalComponent("Component1");
        component1.setId("11111");
        final ExternalComponent component2 = new ExternalComponent("Component2");
        component2.setId("22222");
        final List components = EasyList.build(component1, component2);
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, components, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);
        final SimpleProjectImportIdMapperImpl componentMapper = new SimpleProjectImportIdMapperImpl();

        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);
        projectImportManager.populateComponentMapperOldValues(backupProject, componentMapper);

        assertEquals(2, componentMapper.getRegisteredOldIds().size());
        assertTrue(componentMapper.getRegisteredOldIds().contains("11111"));
        assertTrue(componentMapper.getRegisteredOldIds().contains("22222"));
        assertEquals("Component1", componentMapper.getKey("11111"));
        assertEquals("Component2", componentMapper.getKey("22222"));
    }

    @Test
    public void testPopulateComponentMapper() throws Exception
    {
        final DefaultProjectImportManager projectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null, null,
            null, null, null, null, null, null);

        final SimpleProjectImportIdMapperImpl componentMapper = new SimpleProjectImportIdMapperImpl();

        final MockControl mockProjectComponentControl = MockControl.createStrictControl(ProjectComponent.class);
        final ProjectComponent mockProjectComponent = (ProjectComponent) mockProjectComponentControl.getMock();
        mockProjectComponent.getId();
        mockProjectComponentControl.setReturnValue(new Long(1));
        mockProjectComponentControl.replay();

        final MockControl mockProjectComponentControl2 = MockControl.createStrictControl(ProjectComponent.class);
        final ProjectComponent mockProjectComponent2 = (ProjectComponent) mockProjectComponentControl2.getMock();
        mockProjectComponent2.getId();
        mockProjectComponentControl2.setReturnValue(new Long(2));
        mockProjectComponentControl2.replay();

        projectImportManager.populateComponentMapper(componentMapper, EasyMap.build("12", mockProjectComponent, "14", mockProjectComponent2));

        assertEquals("1", componentMapper.getMappedId("12"));
        assertEquals("2", componentMapper.getMappedId("14"));
        mockProjectComponentControl.verify();
        mockProjectComponentControl2.verify();
    }

    @Test
    public void testValidateCustomFieldValuesNullBackupProject() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(null, null, 0, 0, 0, 0, 0);
            defaultProjectImportManager.validateCustomFieldValues(projectImportData, new MappingResult(), null, null, null);
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testValidateCustomFieldValuesNullMappingResult() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(null, null, 0, 0, 0, 0, 0);
            defaultProjectImportManager.validateCustomFieldValues(projectImportData, null, new BackupProjectImpl(new ExternalProject(),
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST), null, null);
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testValidateCustomFieldValuesNullProjectImporter() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(null, null, 0, 0, 0, 0, 0);
            defaultProjectImportManager.validateCustomFieldValues(projectImportData, new MappingResult(), new BackupProjectImpl(
                new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST), null, null);
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testValidateCustomFieldValuesNullCustFieldXmlPath() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(null, null, 0, 0, 0, 0, 0);
            defaultProjectImportManager.validateCustomFieldValues(projectImportData, new MappingResult(), new BackupProjectImpl(
                new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST), null, null);
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testValidateCustomFieldValuesHappyPath() throws IOException, SAXException
    {
        // Set up the BackupProject
        final ExternalProject externalProject = new ExternalProject();
        externalProject.setId("10");
        final BackupProject backupProject = new BackupProjectImpl(externalProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final MockI18nBean i18nBean = new MockI18nBean();

        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final MockControl mockChainedSaxHandlerControl = MockClassControl.createControl(ChainedSaxHandler.class);
        final ChainedSaxHandler mockChainedSaxHandler = (ChainedSaxHandler) mockChainedSaxHandlerControl.getMock();

        final CustomFieldManager mockCustomFieldManager = createMock(CustomFieldManager.class);
        expect(mockCustomFieldManager.getCustomFieldTypes()).andReturn(Collections.emptyList());
        replay(mockCustomFieldManager);

        final CustomFieldValueValidatorHandler valueValidatorHandler = new CustomFieldValueValidatorHandler(backupProject, projectImportMapper, mockCustomFieldManager, null);
        mockChainedSaxHandler.registerHandler(valueValidatorHandler);
        mockChainedSaxHandlerControl.replay();

        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");

        final MockControl mockBackupXmlParserControl = MockControl.createStrictControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getCustomFieldValuesXmlFile().getAbsolutePath(), mockChainedSaxHandler);
        mockBackupXmlParserControl.replay();

        final MockControl mockCustomFieldOptionMapperValidatorControl = MockControl.createStrictControl(CustomFieldOptionMapperValidator.class);
        final CustomFieldOptionMapperValidator mockCustomFieldOptionMapperValidator = (CustomFieldOptionMapperValidator) mockCustomFieldOptionMapperValidatorControl.getMock();
        mockCustomFieldOptionMapperValidator.validateMappings(i18nBean, backupProject, projectImportMapper.getCustomFieldOptionMapper(),
            projectImportMapper.getCustomFieldMapper(), valueValidatorHandler.getValidationResults());
        mockCustomFieldOptionMapperValidatorControl.replay();

        final MockControl mockProjectImportValidatorsControl = MockControl.createStrictControl(ProjectImportValidators.class);
        final ProjectImportValidators mockProjectImportValidators = (ProjectImportValidators) mockProjectImportValidatorsControl.getMock();
        mockProjectImportValidators.getCustomFieldOptionMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockCustomFieldOptionMapperValidator);
        mockProjectImportValidatorsControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null, null,
            mockCustomFieldManager, null, mockProjectImportValidators, null, null, null, null, null, null)
        {
            @Override
            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return mockChainedSaxHandler;
            }
        };

        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0);
        final MappingResult backupParsingResult = new MappingResult();

        defaultProjectImportManager.validateCustomFieldValues(projectImportData, backupParsingResult, backupProject, null, i18nBean);

        mockChainedSaxHandlerControl.verify();
        mockBackupXmlParserControl.verify();
        mockCustomFieldOptionMapperValidatorControl.verify();
        mockProjectImportValidatorsControl.verify();
        verify(mockCustomFieldManager);
    }

    private BackupSystemInformationImpl getBackupSystemInformationImpl()
    {
        return new BackupSystemInformationImpl("123", "Pro", Collections.EMPTY_LIST, true, Collections.EMPTY_MAP, 0);
    }

    @Test
    public void testValidateFileAttachmentsNullBackupProject() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(null, null, 0, 0, 0, 0, 0);
            defaultProjectImportManager.validateFileAttachments(new ProjectImportOptionsImpl("", "/attach/path"), projectImportData,
                new MappingResult(), null, getBackupSystemInformationImpl(), null, null);
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testValidateFileAttachmentsNullMappingResult() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(null, null, 0, 0, 0, 0, 0);
            defaultProjectImportManager.validateFileAttachments(new ProjectImportOptionsImpl("", "/attach/path"), projectImportData, null,
                new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                    Collections.EMPTY_LIST), getBackupSystemInformationImpl(), null, null);
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testValidateFileAttachmentsNullBackupSystemInfo() throws IOException, SAXException
    {
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, null, null, null, null, null, null);
        try
        {
            final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(null, null, 0, 0, 0, 0, 0);
            defaultProjectImportManager.validateFileAttachments(new ProjectImportOptionsImpl("", "/attach/path"), projectImportData,
                new MappingResult(), new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
                    Collections.EMPTY_LIST, Collections.EMPTY_LIST), null, null, null);
            fail("IllegalArgumentException expected.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected - null path
        }
    }

    @Test
    public void testValidateFileAttachmentsHappyPath() throws IOException, SAXException
    {
        // Set up the BackupProject
        final ExternalProject externalProject = new ExternalProject();
        externalProject.setId("10");
        final BackupProject backupProject = new BackupProjectImpl(externalProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final MockControl mockChainedSaxHandlerControl = MockClassControl.createControl(ChainedSaxHandler.class);
        final ChainedSaxHandler mockChainedSaxHandler = (ChainedSaxHandler) mockChainedSaxHandlerControl.getMock();

        final ProjectImportOptionsImpl projectImportOptions = new ProjectImportOptionsImpl("/", "/");
        final BackupSystemInformationImpl backupSysInfo = getBackupSystemInformationImpl();
        final AttachmentFileValidatorHandler valueValidatorHandler = new AttachmentFileValidatorHandler(backupProject, projectImportOptions,
            backupSysInfo, null);
        mockChainedSaxHandler.registerHandler(valueValidatorHandler);
        mockChainedSaxHandlerControl.replay();

        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");

        final MockControl mockBackupXmlParserControl = MockControl.createStrictControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getFileAttachmentEntitiesXmlFile().getAbsolutePath(), mockChainedSaxHandler);
        mockBackupXmlParserControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null, null,
            null, null, null, null, null, null, null, null, null)
        {
            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return mockChainedSaxHandler;
            }

            AttachmentFileValidatorHandler getAttachmentFileValidatorHandler(final BackupProject backupProject, final ProjectImportOptions projectImportOptions, final BackupSystemInformation backupSystemInformation, final I18nHelper i18nHelper)
            {
                return valueValidatorHandler;
            }
        };

        final MappingResult backupParsingResult = new MappingResult();

        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0);
        defaultProjectImportManager.validateFileAttachments(projectImportOptions, projectImportData, backupParsingResult, backupProject,
            backupSysInfo, null, new MockI18nBean());

        mockChainedSaxHandlerControl.verify();
        mockBackupXmlParserControl.verify();
    }

    @Test
    public void testAutoMapAndValidateIssueTypes() throws IOException, SAXException
    {
        // Set up the BackupProject
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final I18nHelper i18n = new MockI18nBean();

        final MockControl mockIssueTypeMapperValidatorControl = MockControl.createStrictControl(IssueTypeMapperValidator.class);
        final IssueTypeMapperValidator mockIssueTypeMapperValidator = (IssueTypeMapperValidator) mockIssueTypeMapperValidatorControl.getMock();
        mockIssueTypeMapperValidator.validateMappings(i18n, backupProject, projectImportMapper.getIssueTypeMapper());
        final MessageSetImpl messageSet = new MessageSetImpl();
        messageSet.addErrorMessage("I am an error message");
        mockIssueTypeMapperValidatorControl.setReturnValue(messageSet);
        mockIssueTypeMapperValidatorControl.replay();

        final MockControl mockProjectImportValidatorsControl = MockControl.createStrictControl(ProjectImportValidators.class);
        final ProjectImportValidators mockProjectImportValidators = (ProjectImportValidators) mockProjectImportValidatorsControl.getMock();
        mockProjectImportValidators.getIssueTypeMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockIssueTypeMapperValidator);
        mockProjectImportValidatorsControl.replay();

        final MockControl mockAutomaticDataMapperControl = MockControl.createStrictControl(AutomaticDataMapper.class);
        final AutomaticDataMapper mockAutomaticDataMapper = (AutomaticDataMapper) mockAutomaticDataMapperControl.getMock();
        mockAutomaticDataMapper.mapIssueTypes(backupProject, projectImportMapper.getIssueTypeMapper());
        mockAutomaticDataMapperControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null,
            mockAutomaticDataMapper, mockProjectImportValidators, null, null, null, null, null, null);

        final MappingResult mappingResult = new MappingResult();

        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");
        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0);
        defaultProjectImportManager.autoMapAndValidateIssueTypes(projectImportData, mappingResult, backupProject, i18n);

        assertTrue(mappingResult.getIssueTypeMessageSet().hasAnyErrors());
        mockIssueTypeMapperValidatorControl.verify();
        mockAutomaticDataMapperControl.verify();
        mockProjectImportValidatorsControl.verify();
    }

    @Test
    public void testAutoMapAndValidateCustomFields() throws IOException, SAXException
    {
        // Set up the BackupProject
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final I18nHelper i18n = new MockI18nBean();

        final MockControl mockCustomFieldMapperValidatorControl = MockControl.createStrictControl(CustomFieldMapperValidator.class);
        final CustomFieldMapperValidator mockCustomFieldMapperValidator = (CustomFieldMapperValidator) mockCustomFieldMapperValidatorControl.getMock();
        mockCustomFieldMapperValidator.validateMappings(i18n, backupProject, projectImportMapper.getIssueTypeMapper(),
            projectImportMapper.getCustomFieldMapper());
        final MessageSetImpl messageSet = new MessageSetImpl();
        messageSet.addErrorMessage("I am an error message");
        mockCustomFieldMapperValidatorControl.setReturnValue(messageSet);
        mockCustomFieldMapperValidatorControl.replay();

        final MockControl mockProjectImportValidatorsControl = MockControl.createStrictControl(ProjectImportValidators.class);
        final ProjectImportValidators mockProjectImportValidators = (ProjectImportValidators) mockProjectImportValidatorsControl.getMock();
        mockProjectImportValidators.getCustomFieldMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockCustomFieldMapperValidator);
        mockProjectImportValidatorsControl.replay();

        final MockControl mockAutomaticDataMapperControl = MockControl.createStrictControl(AutomaticDataMapper.class);
        final AutomaticDataMapper mockAutomaticDataMapper = (AutomaticDataMapper) mockAutomaticDataMapperControl.getMock();
        mockAutomaticDataMapper.mapCustomFields(backupProject, projectImportMapper.getCustomFieldMapper(), projectImportMapper.getIssueTypeMapper());
        mockAutomaticDataMapperControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null,
            mockAutomaticDataMapper, mockProjectImportValidators, null, null, null, null, null, null);

        final MappingResult mappingResult = new MappingResult();

        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");
        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0);
        defaultProjectImportManager.autoMapAndValidateCustomFields(projectImportData, mappingResult, backupProject, i18n);

        assertTrue(mappingResult.getCustomFieldMessageSet().hasAnyErrors());
        mockCustomFieldMapperValidatorControl.verify();
        mockAutomaticDataMapperControl.verify();
        mockProjectImportValidatorsControl.verify();
    }

    @Test
    public void testAutoMapSystemFields() throws IOException, SAXException
    {
        // Set up the BackupProject
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final MockControl mockAutomaticDataMapperControl = MockControl.createStrictControl(AutomaticDataMapper.class);
        final AutomaticDataMapper mockAutomaticDataMapper = (AutomaticDataMapper) mockAutomaticDataMapperControl.getMock();
        mockAutomaticDataMapper.mapPriorities(projectImportMapper.getPriorityMapper());
        mockAutomaticDataMapper.mapProjects(projectImportMapper.getProjectMapper());
        mockAutomaticDataMapper.mapResolutions(projectImportMapper.getResolutionMapper());
        mockAutomaticDataMapper.mapStatuses(backupProject, projectImportMapper.getStatusMapper(), projectImportMapper.getIssueTypeMapper());
        mockAutomaticDataMapper.mapIssueLinkTypes(projectImportMapper.getIssueLinkTypeMapper());
        mockAutomaticDataMapper.mapIssueSecurityLevels(backupProject.getProject().getKey(), projectImportMapper.getIssueSecurityLevelMapper());
        mockAutomaticDataMapperControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null,
            mockAutomaticDataMapper, null, null, null, null, null, null, null);

        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, null, 0, 0, 0, 0, 0);
        defaultProjectImportManager.autoMapSystemFields(projectImportData, backupProject);

        mockAutomaticDataMapperControl.verify();
    }

    @Test
    public void testAutoMapCustomFieldOptions() throws IOException, SAXException
    {
        // Set up the BackupProject
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final MockControl mockAutomaticDataMapperControl = MockControl.createStrictControl(AutomaticDataMapper.class);
        final AutomaticDataMapper mockAutomaticDataMapper = (AutomaticDataMapper) mockAutomaticDataMapperControl.getMock();
        mockAutomaticDataMapper.mapCustomFieldOptions(backupProject, projectImportMapper.getCustomFieldOptionMapper(),
            projectImportMapper.getCustomFieldMapper(), projectImportMapper.getIssueTypeMapper());
        mockAutomaticDataMapperControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null,
            mockAutomaticDataMapper, null, null, null, null, null, null, null);

        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, null, 0, 0, 0, 0, 0);
        defaultProjectImportManager.autoMapCustomFieldOptions(projectImportData, backupProject);

        mockAutomaticDataMapperControl.verify();
    }

    @Test
    public void testAutoMapProjectRoles() throws IOException, SAXException
    {
        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final MockControl mockAutomaticDataMapperControl = MockControl.createStrictControl(AutomaticDataMapper.class);
        final AutomaticDataMapper mockAutomaticDataMapper = (AutomaticDataMapper) mockAutomaticDataMapperControl.getMock();
        mockAutomaticDataMapper.mapProjectRoles(projectImportMapper.getProjectRoleMapper());
        mockAutomaticDataMapperControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null,
            mockAutomaticDataMapper, null, null, null, null, null, null, null);

        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, null, 0, 0, 0, 0, 0);
        defaultProjectImportManager.autoMapProjectRoles(projectImportData);

        mockAutomaticDataMapperControl.verify();
    }

    @Test
    public void testValidateSystemFields() throws IOException, SAXException
    {
        // Set up the BackupProject
        final BackupProject backupProject = new BackupProjectImpl(new ExternalProject(), Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        final ProjectImportMapperImpl projectImportMapper = new ProjectImportMapperImpl(null, null);

        final I18nHelper i18n = new MockI18nBean();

        final MockControl mockPriorityMapperValidatorControl = MockClassControl.createStrictControl(PriorityMapperValidator.class);
        final PriorityMapperValidator mockPriorityMapperValidator = (PriorityMapperValidator) mockPriorityMapperValidatorControl.getMock();
        mockPriorityMapperValidator.validateMappings(i18n, projectImportMapper.getPriorityMapper());
        final MessageSetImpl priorityMessageSet = new MessageSetImpl();
        priorityMessageSet.addErrorMessage("I am Priority error message");
        mockPriorityMapperValidatorControl.setReturnValue(priorityMessageSet);
        mockPriorityMapperValidatorControl.replay();

        final MockControl mockResolutionMapperValidatorControl = MockClassControl.createStrictControl(ResolutionMapperValidator.class);
        final ResolutionMapperValidator mockResolutionMapperValidator = (ResolutionMapperValidator) mockResolutionMapperValidatorControl.getMock();
        mockResolutionMapperValidator.validateMappings(i18n, projectImportMapper.getResolutionMapper());
        final MessageSetImpl resolutionMessageSet = new MessageSetImpl();
        resolutionMessageSet.addErrorMessage("I am Resolution error message");
        mockResolutionMapperValidatorControl.setReturnValue(resolutionMessageSet);
        mockResolutionMapperValidatorControl.replay();

        final MockControl mockStatusMapperValidatorControl = MockClassControl.createStrictControl(StatusMapperValidator.class);
        final StatusMapperValidator mockStatusMapperValidator = (StatusMapperValidator) mockStatusMapperValidatorControl.getMock();
        mockStatusMapperValidator.validateMappings(i18n, backupProject, projectImportMapper.getIssueTypeMapper(),
            projectImportMapper.getStatusMapper());
        final MessageSetImpl statusMessageSet = new MessageSetImpl();
        statusMessageSet.addErrorMessage("I am Status error message");
        mockStatusMapperValidatorControl.setReturnValue(statusMessageSet);
        mockStatusMapperValidatorControl.replay();

        final MockControl mockUserMapperValidatorControl = MockControl.createStrictControl(UserMapperValidator.class);
        final UserMapperValidator mockUserMapperValidator = (UserMapperValidator) mockUserMapperValidatorControl.getMock();
        mockUserMapperValidator.validateMappings(i18n, projectImportMapper.getUserMapper());
        final MessageSetImpl userMessageSet = new MessageSetImpl();
        userMessageSet.addErrorMessage("I am user error message");
        mockUserMapperValidatorControl.setReturnValue(userMessageSet);
        mockUserMapperValidatorControl.replay();

        final MockControl mockProjectRoleMapperValidatorControl = MockClassControl.createStrictControl(ProjectRoleMapperValidator.class);
        final ProjectRoleMapperValidator mockProjectRoleMapperValidator = (ProjectRoleMapperValidator) mockProjectRoleMapperValidatorControl.getMock();
        mockProjectRoleMapperValidator.validateMappings(i18n, projectImportMapper.getProjectRoleMapper());
        final MessageSetImpl projectRoleMessageSet = new MessageSetImpl();
        projectRoleMessageSet.addErrorMessage("Invalid project role mappings");
        mockProjectRoleMapperValidatorControl.setReturnValue(projectRoleMessageSet);
        mockProjectRoleMapperValidatorControl.replay();

        final MockControl mockProjectRoleActorMapperValidatorControl = MockClassControl.createStrictControl(ProjectRoleActorMapperValidator.class);
        final ProjectRoleActorMapperValidator mockProjectRoleActorMapperValidator = (ProjectRoleActorMapperValidator) mockProjectRoleActorMapperValidatorControl.getMock();
        mockProjectRoleActorMapperValidator.validateProjectRoleActors(i18n, projectImportMapper, null);
        final MessageSetImpl projectRoleActorMessageSet = new MessageSetImpl();
        projectRoleActorMessageSet.addErrorMessage("Invalid project role mappings");
        mockProjectRoleActorMapperValidatorControl.setReturnValue(projectRoleActorMessageSet);
        mockProjectRoleActorMapperValidatorControl.replay();

        final MockControl mockGroupMapperValidatorControl = MockClassControl.createStrictControl(GroupMapperValidator.class);
        final GroupMapperValidator mockGroupMapperValidator = (GroupMapperValidator) mockGroupMapperValidatorControl.getMock();
        mockGroupMapperValidator.validateMappings(i18n, projectImportMapper.getGroupMapper());
        final MessageSetImpl groupMessageSet = new MessageSetImpl();
        groupMessageSet.addErrorMessage("Invalid project role mappings");
        mockGroupMapperValidatorControl.setReturnValue(groupMessageSet);
        mockGroupMapperValidatorControl.replay();

        final MockControl mockIssueLinkTypeMapperValidatorControl = MockClassControl.createStrictControl(IssueLinkTypeMapperValidator.class);
        final IssueLinkTypeMapperValidator mockIssueLinkTypeMapperValidator = (IssueLinkTypeMapperValidator) mockIssueLinkTypeMapperValidatorControl.getMock();
        mockIssueLinkTypeMapperValidator.validateMappings(i18n, backupProject, projectImportMapper.getIssueLinkTypeMapper());
        final MessageSetImpl issueLinkTypeMessageSet = new MessageSetImpl();
        issueLinkTypeMessageSet.addErrorMessage("Invalid issue link type mappings");
        mockIssueLinkTypeMapperValidatorControl.setReturnValue(issueLinkTypeMessageSet);
        mockIssueLinkTypeMapperValidatorControl.replay();

        final MockControl mockIssueSecurityLevelValidatorControl = MockClassControl.createStrictControl(IssueSecurityLevelValidator.class);
        final IssueSecurityLevelValidator mockIssueSecurityLevelValidator = (IssueSecurityLevelValidator) mockIssueSecurityLevelValidatorControl.getMock();
        mockIssueSecurityLevelValidator.validateMappings(projectImportMapper.getIssueSecurityLevelMapper(), backupProject, i18n);
        final MessageSetImpl issueSecurityLevelMessageSet = new MessageSetImpl();
        issueSecurityLevelMessageSet.addErrorMessage("Invalid issue security level mappings");
        mockIssueSecurityLevelValidatorControl.setReturnValue(issueSecurityLevelMessageSet);
        mockIssueSecurityLevelValidatorControl.replay();

        final MockControl mockProjectImportValidatorsControl = MockControl.createStrictControl(ProjectImportValidators.class);
        final ProjectImportValidators mockProjectImportValidators = (ProjectImportValidators) mockProjectImportValidatorsControl.getMock();
        mockProjectImportValidators.getPriorityMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockPriorityMapperValidator);
        mockProjectImportValidators.getResolutionMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockResolutionMapperValidator);
        mockProjectImportValidators.getStatusMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockStatusMapperValidator);
        mockProjectImportValidators.getProjectRoleMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockProjectRoleMapperValidator);
        mockProjectImportValidators.getProjectRoleActorMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockProjectRoleActorMapperValidator);
        mockProjectImportValidators.getUserMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockUserMapperValidator);
        mockProjectImportValidators.getGroupMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockGroupMapperValidator);
        mockProjectImportValidators.getIssueLinkTypeMapperValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockIssueLinkTypeMapperValidator);
        mockProjectImportValidators.getIssueSecurityLevelValidator();
        mockProjectImportValidatorsControl.setReturnValue(mockIssueSecurityLevelValidator);
        mockProjectImportValidatorsControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            mockProjectImportValidators, null, null, null, null, null, null);

        final MappingResult mappingResult = new MappingResult();

        final ProjectImportDataImpl projectImportData = new ProjectImportDataImpl(projectImportMapper, null, 0, 0, 0, 0, 0);
        defaultProjectImportManager.validateSystemFields(projectImportData, mappingResult, null, backupProject, null, i18n);

        assertTrue(mappingResult.getPriorityMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getStatusMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getResolutionMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getProjectRoleMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getUserMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getProjectRoleMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getProjectRoleActorMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getGroupMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getIssueLinkTypeMessageSet().hasAnyErrors());
        assertTrue(mappingResult.getIssueSecurityLevelMessageSet().hasAnyErrors());
        mockPriorityMapperValidatorControl.verify();
        mockResolutionMapperValidatorControl.verify();
        mockStatusMapperValidatorControl.verify();
        mockProjectRoleMapperValidatorControl.verify();
        mockProjectImportValidatorsControl.verify();
        mockUserMapperValidatorControl.verify();
        mockIssueSecurityLevelValidatorControl.verify();
    }

    @Test
    public void testCreateUsers() throws AbortImportException
    {
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        final ExternalUser externalUser = new ExternalUser("fred", "Fred Flinstone", "fred@bedrock.com", "dead");

        final MockControl mockProjectImportPersisterControl = MockControl.createControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createUser(externalUser);
        mockProjectImportPersisterControl.setReturnValue(true);
        mockProjectImportPersisterControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null);

        // Mock UserUtil
        final MockControl mockUserUtilControl = MockControl.createStrictControl(UserUtil.class);
        final UserUtil mockUserUtil = (UserUtil) mockUserUtilControl.getMock();
        mockUserUtil.userExists("fred");
        mockUserUtilControl.setReturnValue(false);
        mockUserUtil.userExists("barney");
        mockUserUtilControl.setReturnValue(false);
        mockUserUtilControl.replay();

        final UserMapper userMapper = new UserMapper(mockUserUtil);
        userMapper.flagUserAsInUse("fred");
        userMapper.flagUserAsInUse("barney");
        userMapper.registerOldValue(externalUser);

        defaultProjectImportManager.createMissingUsers(userMapper, projectImportResults, null);

        assertEquals(1, projectImportResults.getUsersCreatedCount());
        // Verify Mock UserUtil
        mockUserUtilControl.verify();
        mockProjectImportPersisterControl.verify();
    }

    @Test
    public void testCreateUsersErrorCreatingUser() throws AbortImportException
    {
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        final ExternalUser externalUser = new ExternalUser("fred", "Fred Flinstone", "fred@bedrock.com", "dead");

        final MockControl mockProjectImportPersisterControl = MockControl.createControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createUser(externalUser);
        mockProjectImportPersisterControl.setReturnValue(false);
        mockProjectImportPersisterControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null);

        // Mock UserUtil
        final MockControl mockUserUtilControl = MockControl.createStrictControl(UserUtil.class);
        final UserUtil mockUserUtil = (UserUtil) mockUserUtilControl.getMock();
        mockUserUtil.userExists("fred");
        mockUserUtilControl.setReturnValue(false);
        mockUserUtil.userExists("barney");
        mockUserUtilControl.setReturnValue(false);
        mockUserUtilControl.replay();

        final UserMapper userMapper = new UserMapper(mockUserUtil);
        userMapper.flagUserAsInUse("fred");
        userMapper.flagUserAsInUse("barney");
        userMapper.registerOldValue(externalUser);

        defaultProjectImportManager.createMissingUsers(userMapper, projectImportResults, null);

        assertEquals(0, projectImportResults.getUsersCreatedCount());
        assertEquals(1, projectImportResults.getErrors().size());
        assertEquals("Could not create user 'fred'.", projectImportResults.getErrors().iterator().next());
        // Verify Mock UserUtil
        mockUserUtilControl.verify();
        mockProjectImportPersisterControl.verify();
    }

    @Test
    public void testCreateUsersErrorCreatingUserAndAbort() throws AbortImportException
    {
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        // seed with a bunch of errors
        for (int i = 0; i < 10; i++)
        {
            projectImportResults.addError("error" + i);
        }

        final ExternalUser externalUser = new ExternalUser("fred", "Fred Flinstone", "fred@bedrock.com", "dead");

        final MockControl mockProjectImportPersisterControl = MockControl.createControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersisterControl.replay();

        final MockControl mockBoundedExecutorControl = MockClassControl.createControl(BoundedExecutor.class);
        final BoundedExecutor mockBoundedExecutor = (BoundedExecutor) mockBoundedExecutorControl.getMock();
        mockBoundedExecutor.shutdownAndIgnoreQueue();
        mockBoundedExecutorControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null)
        {
            BoundedExecutor createExecutor(final String threadName)
            {
                return mockBoundedExecutor;
            }
        };

        // Mock UserUtil
        final MockControl mockUserUtilControl = MockControl.createStrictControl(UserUtil.class);
        final UserUtil mockUserUtil = (UserUtil) mockUserUtilControl.getMock();
        mockUserUtil.userExists("fred");
        mockUserUtilControl.setReturnValue(false);
        mockUserUtil.userExists("barney");
        mockUserUtilControl.setReturnValue(false);
        mockUserUtilControl.replay();

        final UserMapper userMapper = new UserMapper(mockUserUtil);
        userMapper.flagUserAsInUse("fred");
        userMapper.flagUserAsInUse("barney");
        userMapper.registerOldValue(externalUser);

        try
        {
            defaultProjectImportManager.createMissingUsers(userMapper, projectImportResults, null);
            fail("should abort");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        assertEquals(0, projectImportResults.getUsersCreatedCount());
        mockBoundedExecutorControl.verify();
        // Verify Mock UserUtil
        mockUserUtilControl.verify();
        mockProjectImportPersisterControl.verify();
    }

    @Test
    public void testImportProject_DontUpdateProject() throws ExternalException, AbortImportException
    {
        // user doesn't "update Proj details", so ProjectImportPersister.updateProjectDetails() should NOT be called.

        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("12");
        oldProject.setCounter("23");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        // Mock ProjectImportOptions
        final MockControl mockProjectImportOptionsControl = MockControl.createStrictControl(ProjectImportOptions.class);
        final ProjectImportOptions mockProjectImportOptions = (ProjectImportOptions) mockProjectImportOptionsControl.getMock();
        mockProjectImportOptions.overwriteProjectDetails();
        mockProjectImportOptionsControl.setReturnValue(false);
        mockProjectImportOptions.overwriteProjectDetails();
        mockProjectImportOptionsControl.setReturnValue(false);
        mockProjectImportOptionsControl.replay();

        final MockControl mockProjectManagerControl = MockControl.createStrictControl(ProjectManager.class);
        final ProjectManager mockProjectManager = (ProjectManager) mockProjectManagerControl.getMock();
        mockProjectManager.getProjectObjByKey(oldProject.getKey());
        mockProjectManagerControl.setReturnValue(new MockProject());
        mockProjectManagerControl.replay();

        // Mock ProjectImportPersister
        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.updateProjectIssueCounter(backupProject, 23);
        mockProjectImportPersister.createVersions(backupProject);
        mockProjectImportPersisterControl.setReturnValue(Collections.EMPTY_MAP);
        mockProjectImportPersister.createComponents(backupProject);
        mockProjectImportPersisterControl.setReturnValue(Collections.EMPTY_MAP);
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        // Make the Monkey Project already exist.
        projectImportMapper.getProjectMapper().mapValue("12", "112");
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, mockProjectManager, null);

        defaultProjectImportManager.importProject(mockProjectImportOptions, projectImportMapper, backupProject, new ProjectImportResultsImpl(0, 0, 0,
            0, null), null);

        // Verify Mock ProjectImportPersister
        mockProjectImportPersisterControl.verify();
        // Verify Mock ProjectImportOptions
        mockProjectImportOptionsControl.verify();
        mockProjectManagerControl.verify();
    }

    @Test
    public void testImportProject_UpdateProject() throws ExternalException, AbortImportException
    {
        // user chooses "update Proj details", so ProjectImportPersister.updateProjectDetails() should be called.

        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("12");
        oldProject.setCounter("23");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        // Mock ProjectImportOptions
        final MockControl mockProjectImportOptionsControl = MockControl.createStrictControl(ProjectImportOptions.class);
        final ProjectImportOptions mockProjectImportOptions = (ProjectImportOptions) mockProjectImportOptionsControl.getMock();
        mockProjectImportOptions.overwriteProjectDetails();
        mockProjectImportOptionsControl.setReturnValue(true);
        mockProjectImportOptions.overwriteProjectDetails();
        mockProjectImportOptionsControl.setReturnValue(true);
        mockProjectImportOptionsControl.replay();

        // Mock ProjectImportPersister
        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.updateProjectDetails(oldProject);
        mockProjectImportPersisterControl.setReturnValue(new MockProject());
        mockProjectImportPersister.updateProjectIssueCounter(backupProject, 23);
        mockProjectImportPersister.createVersions(backupProject);
        mockProjectImportPersisterControl.setReturnValue(Collections.EMPTY_MAP);
        mockProjectImportPersister.createComponents(backupProject);
        mockProjectImportPersisterControl.setReturnValue(Collections.EMPTY_MAP);
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        // Make the Monkey Project already exist.
        projectImportMapper.getProjectMapper().mapValue("12", "112");
        final AtomicBoolean importRoleCalled = new AtomicBoolean(false);
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null)
        {
            public void importProjectRoleMembers(final Project project, final ProjectImportMapper projectImportMapper, final ProjectImportResults projectImportResults, final TaskProgressInterval taskProgressInterval)
            {
                // do nothing
                importRoleCalled.set(true);
            }
        };

        defaultProjectImportManager.importProject(mockProjectImportOptions, projectImportMapper, backupProject, new ProjectImportResultsImpl(0, 0, 0,
            0, null), null);

        // Verify Mock ProjectImportPersister
        mockProjectImportPersisterControl.verify();
        // Verify Mock ProjectImportOptions
        mockProjectImportOptionsControl.verify();
        assertTrue(importRoleCalled.get());
    }

    @Test
    public void testImportProject_CreateProject() throws ExternalException, AbortImportException
    {
        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("4");
        oldProject.setCounter("23");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        final MockControl mockProjectImportPersisterControl = MockControl.createControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createProject(backupProject);
        // new Project has ID 12
        mockProjectImportPersisterControl.setReturnValue(new MockProject(12));
        mockProjectImportPersister.updateProjectIssueCounter(backupProject, 23);
        mockProjectImportPersister.createVersions(backupProject);
        mockProjectImportPersisterControl.setReturnValue(Collections.EMPTY_MAP);
        mockProjectImportPersister.createComponents(backupProject);
        mockProjectImportPersisterControl.setReturnValue(Collections.EMPTY_MAP);
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null);

        defaultProjectImportManager.importProject(new ProjectImportOptionsImpl("", "", false), projectImportMapper, backupProject,
            new ProjectImportResultsImpl(0, 0, 0, 0, null), null);
        // Verify that we mapped old ID 4 to new ID 12
        assertEquals("12", projectImportMapper.getProjectMapper().getMappedId("4"));

        // Verify the persister was called as expected.
        mockProjectImportPersisterControl.verify();
    }

    @Test
    public void testImportProject_VersionsAbort() throws ExternalException, AbortImportException
    {
        // user chooses "update Proj details", so ProjectImportPersister.updateProjectDetails() should be called.

        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("12");
        oldProject.setCounter("23");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        // Mock ProjectImportOptions
        final MockControl mockProjectImportOptionsControl = MockControl.createStrictControl(ProjectImportOptions.class);
        final ProjectImportOptions mockProjectImportOptions = (ProjectImportOptions) mockProjectImportOptionsControl.getMock();
        mockProjectImportOptions.overwriteProjectDetails();
        mockProjectImportOptionsControl.setReturnValue(true);
        mockProjectImportOptionsControl.replay();

        // Mock ProjectImportPersister
        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.updateProjectDetails(oldProject);
        mockProjectImportPersisterControl.setReturnValue(new MockProject());
        mockProjectImportPersister.updateProjectIssueCounter(backupProject, 23);
        mockProjectImportPersister.createVersions(backupProject);
        mockProjectImportPersisterControl.setThrowable(new DataAccessException("blah"));
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        // Make the Monkey Project already exist.
        projectImportMapper.getProjectMapper().mapValue("12", "112");
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null);

        try
        {
            defaultProjectImportManager.importProject(mockProjectImportOptions, projectImportMapper, backupProject, new ProjectImportResultsImpl(0,
                0, 0, 0, null), null);
            fail("should throw Abort Import exception");
        }
        catch (final AbortImportException e)
        {
            //expected
        }

        // Verify Mock ProjectImportPersister
        mockProjectImportPersisterControl.verify();
        // Verify Mock ProjectImportOptions
        mockProjectImportOptionsControl.verify();
    }

    @Test
    public void testImportProject_ComponentsAbort() throws ExternalException, AbortImportException
    {
        // user chooses "update Proj details", so ProjectImportPersister.updateProjectDetails() should be called.

        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("12");
        oldProject.setCounter("23");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        // Mock ProjectImportOptions
        final MockControl mockProjectImportOptionsControl = MockControl.createStrictControl(ProjectImportOptions.class);
        final ProjectImportOptions mockProjectImportOptions = (ProjectImportOptions) mockProjectImportOptionsControl.getMock();
        mockProjectImportOptions.overwriteProjectDetails();
        mockProjectImportOptionsControl.setReturnValue(true);
        mockProjectImportOptionsControl.replay();

        // Mock ProjectImportPersister
        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.updateProjectDetails(oldProject);
        mockProjectImportPersisterControl.setReturnValue(new MockProject());
        mockProjectImportPersister.updateProjectIssueCounter(backupProject, 23);
        mockProjectImportPersister.createVersions(backupProject);
        mockProjectImportPersisterControl.setReturnValue(Collections.EMPTY_MAP);
        mockProjectImportPersister.createComponents(backupProject);
        mockProjectImportPersisterControl.setThrowable(new DataAccessException("blah"));
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        // Make the Monkey Project already exist.
        projectImportMapper.getProjectMapper().mapValue("12", "112");
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null);

        try
        {
            defaultProjectImportManager.importProject(mockProjectImportOptions, projectImportMapper, backupProject, new ProjectImportResultsImpl(0,
                0, 0, 0, null), null);
            fail("should throw Abort Import exception");
        }
        catch (final AbortImportException e)
        {
            //expected
        }

        // Verify Mock ProjectImportPersister
        mockProjectImportPersisterControl.verify();
        // Verify Mock ProjectImportOptions
        mockProjectImportOptionsControl.verify();
    }

    @Test
    public void testImportProject_CreateProjectAbortImportException() throws ExternalException, AbortImportException
    {
        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("4");
        oldProject.setCounter("23");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        final MockControl mockProjectImportPersisterControl = MockControl.createControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createProject(backupProject);
        mockProjectImportPersisterControl.setThrowable(new ExternalException());
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, null, null, null, null);

        try
        {
            defaultProjectImportManager.importProject(new ProjectImportOptionsImpl("", "", false), projectImportMapper, backupProject,
                new ProjectImportResultsImpl(0, 0, 0, 0, null), null);
            fail("Should have thrown AbortImportEx");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        // Verify the persister was called as expected.
        mockProjectImportPersisterControl.verify();
    }

    @Test
    public void testDoImport() throws IndexException, IOException, SAXException
    {
        final MockI18nBean i18n = new MockI18nBean();
        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");
        final ProjectImportOptions projectImportOptions = new ProjectImportOptionsImpl("/path", "/attach/path");

        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("4");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        final ChainedSaxHandler chainedSaxHandler = new ChainedSaxHandler();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        projectImportMapper.getProjectMapper().mapValue("4", "5");

        final MockControl mockBackupXmlParserControl = MockControl.createControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getIssuesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getIssueRelatedEntitiesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getChangeItemEntitiesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getCustomFieldValuesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getFileAttachmentEntitiesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParserControl.replay();

        final MockControl mockProjectImportPersisterControl = MockControl.createControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.updateProjectIssueCounter(backupProject, 0);
        mockProjectImportPersister.reIndexProject(projectImportMapper, null, i18n);
        mockProjectImportPersisterControl.replay();

        // Mock IssueLinkManager
        final MockControl mockIssueLinkManagerControl = MockControl.createStrictControl(IssueLinkManager.class);
        final IssueLinkManager mockIssueLinkManager = (IssueLinkManager) mockIssueLinkManagerControl.getMock();
        mockIssueLinkManager.clearCache();
        mockIssueLinkManagerControl.replay();

        final CustomFieldManager mockCustomFieldManager = createMock(CustomFieldManager.class);
        expect(mockCustomFieldManager.getCustomFieldTypes()).andReturn(Collections.emptyList());
        replay(mockCustomFieldManager);
        
        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null,
            mockIssueLinkManager, mockCustomFieldManager, null, null, mockProjectImportPersister, null, null, null, null, null)
        {
            int getTotalEntitiesCount()
            {
                return 1;
            }

            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return chainedSaxHandler;
            }
        };

        final ProjectImportResults projectImportResults = new ProjectImportResultsImpl(System.currentTimeMillis(), 2, 1, 0, null);
        defaultProjectImportManager.doImport(projectImportOptions, new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0),
            backupProject, getBackupSystemInformationImpl(), projectImportResults, null, i18n, null);

        // Verify Mock IssueLinkManager
        mockIssueLinkManagerControl.verify();
        mockBackupXmlParserControl.verify();
        verify(mockCustomFieldManager);
    }

    @Test
    public void testImportIssues_Error() throws IndexException, IOException, SAXException
    {
        final MockI18nBean i18n = new MockI18nBean();
        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");

        final ChainedSaxHandler chainedSaxHandler = new ChainedSaxHandler();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        projectImportMapper.getProjectMapper().mapValue("4", "5");

        final MockControl mockBackupXmlParserControl = MockControl.createControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getIssuesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParserControl.setThrowable(new AbortImportException());
        mockBackupXmlParserControl.replay();

        final MockControl mockBoundedExecutorControl = MockClassControl.createControl(BoundedExecutor.class);
        final BoundedExecutor mockBoundedExecutor = (BoundedExecutor) mockBoundedExecutorControl.getMock();
        mockBoundedExecutor.shutdownAndIgnoreQueue();
        mockBoundedExecutorControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null, null,
            null, null, null, null, null, null, null, null, null)
        {
            int getTotalEntitiesCount()
            {
                return 1;
            }

            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return chainedSaxHandler;
            }

            BoundedExecutor createExecutor(final String threadName)
            {
                return mockBoundedExecutor;
            }
        };

        final ProjectImportResults projectImportResults = new ProjectImportResultsImpl(System.currentTimeMillis(), 2, 1, 0, null);
        // Make the results contain an error
        for (int i = 0; i < 10; i++)
        {
            projectImportResults.addError("error" + i);
        }

        try
        {
            defaultProjectImportManager.importIssues(new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0),
                projectImportResults, i18n, null, projectImportMapper, null);
            fail("should abort");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        mockBoundedExecutorControl.verify();
        mockBackupXmlParserControl.verify();
    }

    @Test
    public void testImportIssueRelatedEntities_Error() throws IndexException, IOException, SAXException
    {
        final MockI18nBean i18n = new MockI18nBean();
        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");

        final ChainedSaxHandler chainedSaxHandler = new ChainedSaxHandler();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        projectImportMapper.getProjectMapper().mapValue("4", "5");

        final MockControl mockBackupXmlParserControl = MockControl.createControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getIssueRelatedEntitiesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParserControl.setThrowable(new AbortImportException());
        mockBackupXmlParserControl.replay();

        final MockControl mockBoundedExecutorControl = MockClassControl.createControl(BoundedExecutor.class);
        final BoundedExecutor mockBoundedExecutor = (BoundedExecutor) mockBoundedExecutorControl.getMock();
        mockBoundedExecutor.shutdownAndIgnoreQueue();
        mockBoundedExecutorControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null, null,
            null, null, null, null, null, null, null, null, null)
        {
            int getTotalEntitiesCount()
            {
                return 1;
            }

            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return chainedSaxHandler;
            }

            BoundedExecutor createExecutor(final String threadName)
            {
                return mockBoundedExecutor;
            }
        };

        final ProjectImportResults projectImportResults = new ProjectImportResultsImpl(System.currentTimeMillis(), 2, 1, 0, null);
        // Make the results contain an error
        for (int i = 0; i < 10; i++)
        {
            projectImportResults.addError("error" + i);
        }

        try
        {
            defaultProjectImportManager.importIssueRelatedData(new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0), null,
                projectImportResults, i18n, null, projectImportMapper, null);
            fail("should abort");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        mockBoundedExecutorControl.verify();
        mockBackupXmlParserControl.verify();
    }

    @Test
    public void testImportChangeItemEntities_Error() throws IndexException, IOException, SAXException
    {
        final MockI18nBean i18n = new MockI18nBean();
        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");

        final ChainedSaxHandler chainedSaxHandler = new ChainedSaxHandler();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        projectImportMapper.getProjectMapper().mapValue("4", "5");

        final MockControl mockBackupXmlParserControl = MockControl.createControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getChangeItemEntitiesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParserControl.setThrowable(new AbortImportException());
        mockBackupXmlParserControl.replay();

        final MockControl mockBoundedExecutorControl = MockClassControl.createControl(BoundedExecutor.class);
        final BoundedExecutor mockBoundedExecutor = (BoundedExecutor) mockBoundedExecutorControl.getMock();
        mockBoundedExecutor.shutdownAndIgnoreQueue();
        mockBoundedExecutorControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null, null,
            null, null, null, null, null, null, null, null, null)
        {
            int getTotalEntitiesCount()
            {
                return 1;
            }

            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return chainedSaxHandler;
            }

            BoundedExecutor createExecutor(final String threadName)
            {
                return mockBoundedExecutor;
            }
        };

        final ProjectImportResults projectImportResults = new ProjectImportResultsImpl(System.currentTimeMillis(), 2, 1, 0, null);
        // Make the results contain an error
        for (int i = 0; i < 10; i++)
        {
            projectImportResults.addError("error" + i);
        }

        try
        {
            defaultProjectImportManager.importChangeItemData(new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0),
                projectImportResults, i18n, projectImportMapper, null);
            fail("should abort");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        mockBoundedExecutorControl.verify();
        mockBackupXmlParserControl.verify();
    }

    @Test
    public void testImportCustomFieldValues_Error() throws IndexException, IOException, SAXException
    {
        final MockI18nBean i18n = new MockI18nBean();
        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");

        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("4");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        final ChainedSaxHandler chainedSaxHandler = new ChainedSaxHandler();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        projectImportMapper.getProjectMapper().mapValue("4", "5");

        final MockControl mockBackupXmlParserControl = MockControl.createControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getCustomFieldValuesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParserControl.setThrowable(new AbortImportException());
        mockBackupXmlParserControl.replay();

        final MockControl mockBoundedExecutorControl = MockClassControl.createControl(BoundedExecutor.class);
        final BoundedExecutor mockBoundedExecutor = (BoundedExecutor) mockBoundedExecutorControl.getMock();
        mockBoundedExecutor.shutdownAndIgnoreQueue();
        mockBoundedExecutorControl.replay();

        final CustomFieldManager mockCustomFieldManager = createMock(CustomFieldManager.class);
        expect(mockCustomFieldManager.getCustomFieldTypes()).andReturn(Collections.emptyList());
        replay(mockCustomFieldManager);

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null, null,
            mockCustomFieldManager, null, null, null, null, null, null, null, null)
        {
            int getTotalEntitiesCount()
            {
                return 1;
            }

            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return chainedSaxHandler;
            }

            BoundedExecutor createExecutor(final String threadName)
            {
                return mockBoundedExecutor;
            }
        };

        final ProjectImportResults projectImportResults = new ProjectImportResultsImpl(System.currentTimeMillis(), 2, 1, 0, null);
        // Make the results contain an error
        for (int i = 0; i < 10; i++)
        {
            projectImportResults.addError("error" + i);
        }

        try
        {
            defaultProjectImportManager.importCustomFieldValues(new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0, 0, 0, 0),
                backupProject, null, projectImportResults, i18n, projectImportMapper, null);
            fail("should abort");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        mockBoundedExecutorControl.verify();
        mockBackupXmlParserControl.verify();
        verify(mockCustomFieldManager);
    }

    @Test
    public void testImportAttachments_Error() throws IndexException, IOException, SAXException
    {
        final MockI18nBean i18n = new MockI18nBean();
        final ProjectImportTemporaryFiles temporaryFiles = new MockProjectImportTemporaryFiles("TST");

        final ProjectImportOptions projectImportOptions = new ProjectImportOptionsImpl("/path", "/attach/path");

        final ExternalProject oldProject = new ExternalProject();
        oldProject.setId("4");
        final BackupProject backupProject = new BackupProjectImpl(oldProject, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST,
            Collections.EMPTY_LIST);

        final ChainedSaxHandler chainedSaxHandler = new ChainedSaxHandler();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, null);
        projectImportMapper.getProjectMapper().mapValue("4", "5");

        final MockControl mockBackupXmlParserControl = MockControl.createControl(BackupXmlParser.class);
        final BackupXmlParser mockBackupXmlParser = (BackupXmlParser) mockBackupXmlParserControl.getMock();
        mockBackupXmlParser.parseBackupXml(temporaryFiles.getFileAttachmentEntitiesXmlFile().getAbsolutePath(), chainedSaxHandler);
        mockBackupXmlParserControl.setThrowable(new AbortImportException());
        mockBackupXmlParserControl.replay();

        final MockControl mockBoundedExecutorControl = MockClassControl.createControl(BoundedExecutor.class);
        final BoundedExecutor mockBoundedExecutor = (BoundedExecutor) mockBoundedExecutorControl.getMock();
        mockBoundedExecutor.shutdownAndIgnoreQueue();
        mockBoundedExecutorControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(mockBackupXmlParser, null, null, null, null,
            null, null, null, null, null, null, null, null, null)
        {
            int getTotalEntitiesCount()
            {
                return 1;
            }

            ChainedSaxHandler getChainedHandler(final TaskProgressProcessor taskProgressProcessor)
            {
                return chainedSaxHandler;
            }

            BoundedExecutor createExecutor(final String threadName)
            {
                return mockBoundedExecutor;
            }
        };

        final ProjectImportResults projectImportResults = new ProjectImportResultsImpl(System.currentTimeMillis(), 2, 1, 0, null);
        // Make the results contain an error
        for (int i = 0; i < 10; i++)
        {
            projectImportResults.addError("error" + i);
        }

        try
        {
            defaultProjectImportManager.importAttachments(projectImportOptions, new ProjectImportDataImpl(projectImportMapper, temporaryFiles, 0, 0,
                0, 0, 0), backupProject, null, projectImportResults, i18n, null);
            fail("should abort");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        mockBoundedExecutorControl.verify();
        mockBackupXmlParserControl.verify();
    }

    @Test
    public void testImportProjectRoleMembersGroups() throws Exception
    {
        final MockControl mockProjectRoleManagerControl = MockControl.createStrictControl(ProjectRoleManager.class);
        final ProjectRoleManager mockProjectRoleManager = (ProjectRoleManager) mockProjectRoleManagerControl.getMock();
        mockProjectRoleManager.removeAllRoleActorsByProject(null);
        mockProjectRoleManagerControl.replay();

        final MockControl mockGroupManagerControl = MockControl.createControl(GroupManager.class);
        final GroupManager mockGroupManager = (GroupManager) mockGroupManagerControl.getMock();
        EasyMock.makeThreadSafe(mockGroupManager, true);

        mockGroupManager.groupExists("dudes");
        mockGroupManagerControl.setReturnValue(true);
        mockGroupManager.groupExists("dudettes");
        mockGroupManagerControl.setReturnValue(false);
        mockGroupManagerControl.replay();

        final EntityRepresentation dudeEntityRepresentation = new EntityRepresentationImpl("X", new HashMap());

        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createEntity(dudeEntityRepresentation);
        mockProjectImportPersisterControl.setReturnValue(new Long(34));
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, mockGroupManager);
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-group-role-actor", "dudes"));
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-group-role-actor", "dudettes"));
        // Set up mapped IDs
        projectImportMapper.getProjectRoleMapper().registerOldValue("3", "My Role");
        projectImportMapper.getProjectMapper().mapValue("2", "12");
        projectImportMapper.getProjectRoleMapper().mapValue("3", "13");

        final MockControl mockProjectRoleActorParserControl = MockControl.createStrictControl(ProjectRoleActorParser.class);
        final ProjectRoleActorParser mockProjectRoleActorParser = (ProjectRoleActorParser) mockProjectRoleActorParserControl.getMock();
        mockProjectRoleActorParser.getEntityRepresentation(new ExternalProjectRoleActor(null, "12", "13", "atlassian-group-role-actor", "dudes"));
        mockProjectRoleActorParserControl.setReturnValue(dudeEntityRepresentation);
        mockProjectRoleActorParserControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, mockGroupManager, mockProjectRoleManager, null, null)
        {
            ProjectRoleActorParser getProjectRoleActorParser()
            {
                return mockProjectRoleActorParser;
            }
        };
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        defaultProjectImportManager.importProjectRoleMembers(null, projectImportMapper, projectImportResults, null);

        assertEquals(1, projectImportResults.getRoles().size());
        assertTrue(projectImportResults.getRoles().contains("My Role"));
        assertEquals(1, projectImportResults.getGroupsCreatedCountForRole("My Role"));
        mockProjectRoleManagerControl.verify();
        mockProjectImportPersisterControl.verify();
        mockGroupManagerControl.verify();
        mockProjectRoleActorParserControl.verify();
    }

    @Test
    public void testImportProjectRoleMembersGroupsReportError() throws Exception
    {
        final MockControl mockProjectRoleManagerControl = MockControl.createStrictControl(ProjectRoleManager.class);
        final ProjectRoleManager mockProjectRoleManager = (ProjectRoleManager) mockProjectRoleManagerControl.getMock();
        mockProjectRoleManager.removeAllRoleActorsByProject(null);
        mockProjectRoleManagerControl.replay();

        final MockControl mockGroupManagerControl = MockControl.createControl(GroupManager.class);
        final GroupManager mockGroupManager = (GroupManager) mockGroupManagerControl.getMock();
        EasyMock.makeThreadSafe(mockGroupManager, true);
        mockGroupManager.groupExists("dudes");
        mockGroupManagerControl.setReturnValue(true);
        mockGroupManager.groupExists("dudettes");
        mockGroupManagerControl.setReturnValue(false);
        mockGroupManagerControl.replay();

        final EntityRepresentation dudeEntityRepresentation = new EntityRepresentationImpl("X", new HashMap());

        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createEntity(dudeEntityRepresentation);
        mockProjectImportPersisterControl.setReturnValue(null);
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, mockGroupManager);
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-group-role-actor", "dudes"));
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-group-role-actor", "dudettes"));
        // Set up mapped IDs
        projectImportMapper.getProjectRoleMapper().registerOldValue("3", "My Role");
        projectImportMapper.getProjectMapper().mapValue("2", "12");
        projectImportMapper.getProjectRoleMapper().mapValue("3", "13");

        final MockControl mockProjectRoleActorParserControl = MockControl.createStrictControl(ProjectRoleActorParser.class);
        final ProjectRoleActorParser mockProjectRoleActorParser = (ProjectRoleActorParser) mockProjectRoleActorParserControl.getMock();
        mockProjectRoleActorParser.getEntityRepresentation(new ExternalProjectRoleActor(null, "12", "13", "atlassian-group-role-actor", "dudes"));
        mockProjectRoleActorParserControl.setReturnValue(dudeEntityRepresentation);
        mockProjectRoleActorParserControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, mockGroupManager, mockProjectRoleManager, null, null)
        {
            ProjectRoleActorParser getProjectRoleActorParser()
            {
                return mockProjectRoleActorParser;
            }
        };
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        defaultProjectImportManager.importProjectRoleMembers(null, projectImportMapper, projectImportResults, null);

        assertEquals(0, projectImportResults.getRoles().size());
        assertFalse(projectImportResults.getRoles().contains("My Role"));
        assertEquals(0, projectImportResults.getGroupsCreatedCountForRole("My Role"));
        assertEquals(1, projectImportResults.getErrors().size());
        assertEquals("There was an error adding group 'dudes' to the Project Role 'My Role'.", projectImportResults.getErrors().iterator().next());
        mockProjectRoleManagerControl.verify();
        mockProjectImportPersisterControl.verify();
        mockGroupManagerControl.verify();
        mockProjectRoleActorParserControl.verify();
    }

    @Test
    public void testImportProjectRoleMembersGroupsReportErrorAndAbort() throws Exception
    {
        final MockControl mockProjectRoleManagerControl = MockControl.createStrictControl(ProjectRoleManager.class);
        final ProjectRoleManager mockProjectRoleManager = (ProjectRoleManager) mockProjectRoleManagerControl.getMock();
        mockProjectRoleManager.removeAllRoleActorsByProject(null);
        mockProjectRoleManagerControl.replay();

        final MockControl mockGroupManagerControl = MockControl.createControl(GroupManager.class);
        final GroupManager mockGroupManager = (GroupManager) mockGroupManagerControl.getMock();
        mockGroupManagerControl.replay();

        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(null, mockGroupManager);
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-group-role-actor", "dudes"));
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-group-role-actor", "dudettes"));
        // Set up mapped IDs
        projectImportMapper.getProjectRoleMapper().registerOldValue("3", "My Role");
        projectImportMapper.getProjectMapper().mapValue("2", "12");
        projectImportMapper.getProjectRoleMapper().mapValue("3", "13");

        final MockControl mockProjectRoleActorParserControl = MockControl.createStrictControl(ProjectRoleActorParser.class);
        final ProjectRoleActorParser mockProjectRoleActorParser = (ProjectRoleActorParser) mockProjectRoleActorParserControl.getMock();
        mockProjectRoleActorParserControl.replay();

        final MockControl mockBoundedExecutorControl = MockClassControl.createControl(BoundedExecutor.class);
        final BoundedExecutor mockBoundedExecutor = (BoundedExecutor) mockBoundedExecutorControl.getMock();
        mockBoundedExecutor.shutdownAndWait();
        mockBoundedExecutorControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, null, mockGroupManager, mockProjectRoleManager, null, null)
        {
            ProjectRoleActorParser getProjectRoleActorParser()
            {
                return mockProjectRoleActorParser;
            }

            BoundedExecutor createExecutor(final String threadName)
            {
                return mockBoundedExecutor;
            }
        };
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        // Add 9 errors ahead of time
        for (int i = 0; i < 10; i++)
        {
            projectImportResults.addError("error" + i);
        }

        try
        {
            defaultProjectImportManager.importProjectRoleMembers(null, projectImportMapper, projectImportResults, null);
            fail("should have aborted");
        }
        catch (final AbortImportException e)
        {
            // expected
        }

        assertEquals(0, projectImportResults.getRoles().size());
        assertFalse(projectImportResults.getRoles().contains("My Role"));
        assertEquals(0, projectImportResults.getGroupsCreatedCountForRole("My Role"));
        mockProjectRoleManagerControl.verify();
        mockProjectImportPersisterControl.verify();
        mockGroupManagerControl.verify();
        mockProjectRoleActorParserControl.verify();
        mockBoundedExecutorControl.verify();
    }

    @Test
    public void testImportProjectRoleMembersUsers() throws Exception
    {
        final MockControl mockProjectRoleManagerControl = MockControl.createControl(ProjectRoleManager.class);
        final ProjectRoleManager mockProjectRoleManager = (ProjectRoleManager) mockProjectRoleManagerControl.getMock();
        mockProjectRoleManager.removeAllRoleActorsByProject(null);
        mockProjectRoleManagerControl.replay();

        final MockControl mockUserManagerControl = MockControl.createControl(UserUtil.class);
        final UserUtil mockUserManager = (UserUtil) mockUserManagerControl.getMock();
        EasyMock.makeThreadSafe(mockUserManager, true);
        mockUserManager.userExists("peter");
        mockUserManagerControl.setReturnValue(true);
        mockUserManager.userExists("paul");
        mockUserManagerControl.setReturnValue(false);
        mockUserManagerControl.replay();

        final EntityRepresentation paulEntityRepresentation = new EntityRepresentationImpl("X", new HashMap());

        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createEntity(paulEntityRepresentation);
        mockProjectImportPersisterControl.setReturnValue(new Long(34));
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(mockUserManager, null);
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-user-role-actor", "peter"));
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-user-role-actor", "paul"));
        // random one should be ignored with a log message.
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(new ExternalProjectRoleActor("1", "2", "3", "weird", "Joe"));
        // Set up mapped IDs
        projectImportMapper.getProjectRoleMapper().registerOldValue("3", "My Role");
        projectImportMapper.getProjectMapper().mapValue("2", "12");
        projectImportMapper.getProjectRoleMapper().mapValue("3", "13");

        final MockControl mockProjectRoleActorParserControl = MockControl.createStrictControl(ProjectRoleActorParser.class);
        final ProjectRoleActorParser mockProjectRoleActorParser = (ProjectRoleActorParser) mockProjectRoleActorParserControl.getMock();
        mockProjectRoleActorParser.getEntityRepresentation(new ExternalProjectRoleActor(null, "12", "13", "atlassian-user-role-actor", "peter"));
        mockProjectRoleActorParserControl.setReturnValue(paulEntityRepresentation);
        mockProjectRoleActorParserControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, mockUserManager, null, mockProjectRoleManager, null, null)
        {
            ProjectRoleActorParser getProjectRoleActorParser()
            {
                return mockProjectRoleActorParser;
            }
        };
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        defaultProjectImportManager.importProjectRoleMembers(null, projectImportMapper, projectImportResults, null);

        assertEquals(1, projectImportResults.getRoles().size());
        assertTrue(projectImportResults.getRoles().contains("My Role"));
        assertEquals(1, projectImportResults.getUsersCreatedCountForRole("My Role"));
        mockProjectRoleManagerControl.verify();
        mockProjectImportPersisterControl.verify();
        mockUserManagerControl.verify();
        mockProjectRoleActorParserControl.verify();
    }

    @Test
    public void testImportProjectRoleMembersUsersReportError() throws Exception
    {
        final MockControl mockProjectRoleManagerControl = MockControl.createControl(ProjectRoleManager.class);
        final ProjectRoleManager mockProjectRoleManager = (ProjectRoleManager) mockProjectRoleManagerControl.getMock();
        mockProjectRoleManager.removeAllRoleActorsByProject(null);
        mockProjectRoleManagerControl.replay();

        final MockControl mockUserManagerControl = MockControl.createControl(UserUtil.class);
        final UserUtil mockUserManager = (UserUtil) mockUserManagerControl.getMock();
        EasyMock.makeThreadSafe(mockUserManager, true);
        mockUserManager.userExists("peter");
        mockUserManagerControl.setReturnValue(true);
        mockUserManager.userExists("paul");
        mockUserManagerControl.setReturnValue(false);
        mockUserManagerControl.replay();

        final EntityRepresentation paulEntityRepresentation = new EntityRepresentationImpl("X", new HashMap());

        final MockControl mockProjectImportPersisterControl = MockControl.createStrictControl(ProjectImportPersister.class);
        final ProjectImportPersister mockProjectImportPersister = (ProjectImportPersister) mockProjectImportPersisterControl.getMock();
        mockProjectImportPersister.createEntity(paulEntityRepresentation);
        mockProjectImportPersisterControl.setReturnValue(null);
        mockProjectImportPersisterControl.replay();

        final ProjectImportMapper projectImportMapper = new ProjectImportMapperImpl(mockUserManager, null);
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-user-role-actor", "peter"));
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(
            new ExternalProjectRoleActor("1", "2", "3", "atlassian-user-role-actor", "paul"));
        // random one should be ignored with a log message.
        projectImportMapper.getProjectRoleActorMapper().flagValueActorAsInUse(new ExternalProjectRoleActor("1", "2", "3", "weird", "Joe"));
        // Set up mapped IDs
        projectImportMapper.getProjectRoleMapper().registerOldValue("3", "My Role");
        projectImportMapper.getProjectMapper().mapValue("2", "12");
        projectImportMapper.getProjectRoleMapper().mapValue("3", "13");

        final MockControl mockProjectRoleActorParserControl = MockControl.createStrictControl(ProjectRoleActorParser.class);
        final ProjectRoleActorParser mockProjectRoleActorParser = (ProjectRoleActorParser) mockProjectRoleActorParserControl.getMock();
        mockProjectRoleActorParser.getEntityRepresentation(new ExternalProjectRoleActor(null, "12", "13", "atlassian-user-role-actor", "peter"));
        mockProjectRoleActorParserControl.setReturnValue(paulEntityRepresentation);
        mockProjectRoleActorParserControl.replay();

        final DefaultProjectImportManager defaultProjectImportManager = new DefaultProjectImportManager(null, null, null, null, null, null, null,
            null, mockProjectImportPersister, mockUserManager, null, mockProjectRoleManager, null, null)
        {
            ProjectRoleActorParser getProjectRoleActorParser()
            {
                return mockProjectRoleActorParser;
            }
        };
        final ProjectImportResultsImpl projectImportResults = new ProjectImportResultsImpl(0, 0, 0, 0, new MockI18nBean());
        defaultProjectImportManager.importProjectRoleMembers(null, projectImportMapper, projectImportResults, null);

        assertEquals(0, projectImportResults.getRoles().size());
        assertFalse(projectImportResults.getRoles().contains("My Role"));
        assertEquals(0, projectImportResults.getUsersCreatedCountForRole("My Role"));
        assertEquals(1, projectImportResults.getErrors().size());
        assertEquals("There was an error adding user 'peter' to the Project Role 'My Role'.", projectImportResults.getErrors().iterator().next());
        mockProjectRoleManagerControl.verify();
        mockProjectImportPersisterControl.verify();
        mockUserManagerControl.verify();
        mockProjectRoleActorParserControl.verify();
    }

}
