package com.atlassian.jira.imports.project.validation;

import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.external.beans.ExternalUser;
import com.atlassian.jira.imports.project.mapper.UserMapper;
import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetAssert;
import com.atlassian.jira.web.bean.MockI18nBean;
import org.easymock.MockControl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @since v3.13
 */
public class TestUserMapperValidatorImpl extends ListeningTestCase
{

    @Test
    public void testValidateMappingsExternalManagement() throws Exception
    {
        UserMapperValidatorImpl userMapperValidator = new UserMapperValidatorImpl(null)
        {
            boolean isExternalUserManagementEnabled()
            {
                return true;
            }
        };

        ExternalUser externalUser1 = new ExternalUser("dude", "Dude McMan");
        final UserMapper userMapper = new UserMapper(null)
        {
            public boolean userExists(final String userName)
            {
                return "dude".equals(userName) || "fred".equals(userName);
            }
        };
        userMapper.flagUserAsMandatory("dude");
        userMapper.flagUserAsMandatory("barney");
        userMapper.flagUserAsInUse("fred");
        userMapper.flagUserAsInUse("dudette");
        userMapper.registerOldValue(externalUser1);

        final MessageSet messageSet = userMapperValidator.validateMappings(new MockI18nBean(), userMapper);
        assertEquals(1, messageSet.getErrorMessages().size());
        final String errorMsg = "There are 1 required user(s) that are missing from the current system. External user management is enabled so the import is unable to create the user(s). You must add the user(s) to the system before the import can proceed. Click the 'View Details' link to see a full list of user(s) that are required.";
        assertEquals(errorMsg, messageSet.getErrorMessages().iterator().next());
        MessageSet.MessageLink link = messageSet.getLinkForError(errorMsg);
        assertEquals("View Details", link.getLinkText());
        assertEquals("/secure/admin/ProjectImportMissingMandatoryUsersExtMgmt.jspa", link.getLinkUrl());

        assertEquals(1, messageSet.getWarningMessages().size());
        final String warningMsg = "There are '1' user(s) referenced that are in use in the project and missing from the current system. External user management is enabled so the import is unable to create the user(s). You may want to add the user(s) to the system before performing the import but the import can proceed without them. Click the 'View Details' link to see a full list of user(s) that are in use.";
        assertEquals(warningMsg, messageSet.getWarningMessages().iterator().next());
        link = messageSet.getLinkForWarning(warningMsg);
        assertEquals("View Details", link.getLinkText());
        assertEquals("/secure/admin/ProjectImportMissingOptionalUsersExtMgmt.jspa", link.getLinkUrl());
    }

    @Test
    public void testValidateMappingsExternalManagementHappy() throws Exception
    {
        UserMapperValidatorImpl userMapperValidator = new UserMapperValidatorImpl(null)
        {
            boolean isExternalUserManagementEnabled()
            {
                return true;
            }
        };

        ExternalUser externalUser1 = new ExternalUser("dude", "Dude McMan");
        final UserMapper userMapper = new UserMapper(null)
        {
            public boolean userExists(final String userName)
            {
                return "dude".equals(userName) || "fred".equals(userName);
            }
        };

        userMapper.flagUserAsMandatory("dude");
        userMapper.flagUserAsInUse("fred");
        userMapper.registerOldValue(externalUser1);

        final MessageSet messageSet = userMapperValidator.validateMappings(new MockI18nBean(), userMapper);
        MessageSetAssert.assertNoMessages(messageSet);
    }

    @Test
    public void testValidateMappingsMissingMandatoryCantAutoCreate() throws Exception
    {
        UserMapperValidatorImpl userMapperValidator = new UserMapperValidatorImpl(null)
        {
            boolean isExternalUserManagementEnabled()
            {
                return false;
            }
        };

        ExternalUser externalUser1 = new ExternalUser("dude", "Dude McMan");
        final UserMapper userMapper = new UserMapper(null)
        {
            public boolean userExists(final String userName)
            {
                return false;
            }
        };

        userMapper.flagUserAsMandatory("dude");
        userMapper.flagUserAsMandatory("dudette");
        userMapper.registerOldValue(externalUser1);

        final MessageSet messageSet = userMapperValidator.validateMappings(new MockI18nBean(), userMapper);
        MessageSetAssert.assert1Error(messageSet, "There are '1' required user(s) that JIRA can not automatically create.");
        MessageSetAssert.assert1Warning(messageSet, "There are '1' users that will be automatically created if the import continues.");
    }

    @Test
    public void testValidateMappingsMissingOptionalCantAutoCreate() throws Exception
    {
        UserMapperValidatorImpl userMapperValidator = new UserMapperValidatorImpl(null)
        {
            boolean isExternalUserManagementEnabled()
            {
                return false;
            }
        };

        ExternalUser externalUser1 = new ExternalUser("dude", "Dude McMan");
        final UserMapper userMapper = new UserMapper(null)
        {
            public boolean userExists(final String userName)
            {
                return false;
            }
        };

        userMapper.flagUserAsInUse("dude");
        userMapper.flagUserAsInUse("dudette");
        userMapper.registerOldValue(externalUser1);

        final MessageSet messageSet = userMapperValidator.validateMappings(new MockI18nBean(), userMapper);
        assertEquals(2, messageSet.getWarningMessages().size());
        assertTrue(messageSet.getWarningMessages().contains("There are '1' users that will be automatically created if the import continues."));
        assertTrue(messageSet.getWarningMessages().contains("There are '1' user(s) referenced that JIRA can not automatically create. You may want to create these users before performing the import."));
    }

    @Test
    public void testValidateMappingsHappyPath() throws Exception
    {
        UserMapperValidatorImpl userMapperValidator = new UserMapperValidatorImpl(null)
        {
            boolean isExternalUserManagementEnabled()
            {
                return false;
            }
        };

        ExternalUser externalUser1 = new ExternalUser("dude", "Dude McMan");
        final UserMapper userMapper = new UserMapper(null)
        {
            public boolean userExists(final String userName)
            {
                return "dude".equals(userName) || "dudette".equals(userName);
            }
        };
        userMapper.flagUserAsMandatory("dude");
        userMapper.flagUserAsMandatory("dudette");
        userMapper.registerOldValue(externalUser1);

        final MessageSet messageSet = userMapperValidator.validateMappings(new MockI18nBean(), userMapper);
        assertFalse(messageSet.hasAnyMessages());
    }

    @Test
    public void testIsExternalUserManagementEnabled() throws Exception
    {
        final MockControl mockApplicationPropertiesControl = MockControl.createStrictControl(ApplicationProperties.class);
        final ApplicationProperties mockApplicationProperties = (ApplicationProperties) mockApplicationPropertiesControl.getMock();
        mockApplicationProperties.getOption(APKeys.JIRA_OPTION_USER_EXTERNALMGT);
        mockApplicationPropertiesControl.setReturnValue(true);
        mockApplicationPropertiesControl.replay();

        UserMapperValidatorImpl userMapperValidator = new UserMapperValidatorImpl(mockApplicationProperties);
        userMapperValidator.isExternalUserManagementEnabled();

        mockApplicationPropertiesControl.verify();
    }
}
