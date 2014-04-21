package com.atlassian.crowd.embedded.admin.directory;

import com.atlassian.crowd.directory.DelegatedAuthenticationDirectory;
import com.atlassian.crowd.directory.ldap.LDAPPropertiesMapper;
import com.atlassian.crowd.model.directory.DirectoryImpl;

import java.util.HashMap;
import java.util.Map;

/**
 * Convenience class for setting attributes for a "Delegated Authentication LDAP" Directory.
 * <p/>
 * This class is not thread safe.
 */
public class LdapDelegatingDirectoryAttributes
{
    private String ldapUrl;
    private String ldapBasedn;
    private String ldapUserdn;
    private String ldapPassword;
    private String ldapUserDn;
    private String ldapUserObjectclass;
    private String ldapUserFilter;
    private String ldapUserUsername;
    private String ldapUserUsernameRdn;
    private String ldapUserFirstname;
    private String ldapUserLastname;
    private String ldapUserDisplayname;
    private String ldapUserEmail;
    private boolean createUserOnAuth;
    private boolean updateUserOnAuth;
    private String delegatedToClass;
    private String ldapAutoAddGroups;

    private String ldapGroupObjectclass;
    private String ldapGroupFilter;
    private String ldapGroupName;
    private String ldapGroupDescription;

    private String ldapGroupUsernames;
    private String ldapUserGroup;
    private boolean ldapUsermembershipUseForGroups;
    private boolean ldapUsermembershipUse;

    private boolean synchroniseGroupMemberships;
    private String ldapGroupDn;
    private boolean ldapPagedresults;
    private String ldapPagedresultsSize;
    private boolean ldapReferral;


    //----------------------------------------------------------------------
    // Getters and Setters
    //----------------------------------------------------------------------

    public String getLdapUrl()
    {
        return ldapUrl;
    }

    public void setLdapUrl(final String ldapUrl)
    {
        this.ldapUrl = ldapUrl;
    }

    public String getLdapBasedn()
    {
        return ldapBasedn;
    }

    public void setLdapBasedn(final String ldapBasedn)
    {
        this.ldapBasedn = ldapBasedn;
    }

    public String getLdapUserdn()
    {
        return ldapUserdn;
    }

    public void setLdapUserdn(final String ldapUserdn)
    {
        this.ldapUserdn = ldapUserdn;
    }

    public String getLdapPassword()
    {
        return ldapPassword;
    }

    public void setLdapPassword(final String ldapPassword)
    {
        this.ldapPassword = ldapPassword;
    }

    public String getLdapUserDn()
    {
        return ldapUserDn;
    }

    public void setLdapUserDn(String ldapUserDn)
    {
        this.ldapUserDn = ldapUserDn;
    }

    public String getLdapUserObjectclass()
    {
        return ldapUserObjectclass;
    }

    public void setLdapUserObjectclass(String ldapUserObjectclass)
    {
        this.ldapUserObjectclass = ldapUserObjectclass;
    }

    public String getLdapUserFilter()
    {
        return ldapUserFilter;
    }

    public void setLdapUserFilter(String ldapUserFilter)
    {
        this.ldapUserFilter = ldapUserFilter;
    }

    public String getLdapUserUsername()
    {
        return ldapUserUsername;
    }

    public void setLdapUserUsername(final String ldapUserUsername)
    {
        this.ldapUserUsername = ldapUserUsername;
    }

    public String getLdapUserUsernameRdn()
    {
        return ldapUserUsernameRdn;
    }

    public void setLdapUserUsernameRdn(String ldapUserUsernameRdn)
    {
        this.ldapUserUsernameRdn = ldapUserUsernameRdn;
    }

    public String getLdapUserFirstname()
    {
        return ldapUserFirstname;
    }

    public void setLdapUserFirstname(String ldapUserFirstname)
    {
        this.ldapUserFirstname = ldapUserFirstname;
    }

    public String getLdapUserLastname()
    {
        return ldapUserLastname;
    }

    public void setLdapUserLastname(String ldapUserLastname)
    {
        this.ldapUserLastname = ldapUserLastname;
    }

    public String getLdapUserDisplayname()
    {
        return ldapUserDisplayname;
    }

    public void setLdapUserDisplayname(String ldapUserDisplayname)
    {
        this.ldapUserDisplayname = ldapUserDisplayname;
    }

    public String getLdapUserEmail()
    {
        return ldapUserEmail;
    }

    public void setLdapUserEmail(String ldapUserEmail)
    {
        this.ldapUserEmail = ldapUserEmail;
    }

    public boolean isCreateUserOnAuth()
    {
        return createUserOnAuth;
    }

    public void setCreateUserOnAuth(final boolean createUserOnAuth)
    {
        this.createUserOnAuth = createUserOnAuth;
    }

    public boolean isUpdateUserOnAuth()
    {
        return updateUserOnAuth;
    }

    public void setUpdateUserOnAuth(final boolean updateUserOnAuth)
    {
        this.updateUserOnAuth = updateUserOnAuth;
    }

    public String getDelegatedToClass()
    {
        return delegatedToClass;
    }

    public void setDelegatedToClass(final String delegatedToClass)
    {
        this.delegatedToClass = delegatedToClass;
    }

    public void setLdapAutoAddGroups(String groups)
    {
        this.ldapAutoAddGroups = groups;
    }

    public String getLdapAutoAddGroups()
    {
        return ldapAutoAddGroups;
    }

    public String getLdapGroupObjectclass()
    {
        return ldapGroupObjectclass;
    }

    public void setLdapGroupObjectclass(String ldapGroupObjectclass)
    {
        this.ldapGroupObjectclass = ldapGroupObjectclass;
    }

    public String getLdapGroupFilter()
    {
        return ldapGroupFilter;
    }

    public void setLdapGroupFilter(String ldapGroupFilter)
    {
        this.ldapGroupFilter = ldapGroupFilter;
    }

    public String getLdapGroupName()
    {
        return ldapGroupName;
    }

    public void setLdapGroupName(String ldapGroupName)
    {
        this.ldapGroupName = ldapGroupName;
    }

    public String getLdapGroupDescription()
    {
        return ldapGroupDescription;
    }

    public void setLdapGroupDescription(String ldapGroupDescription)
    {
        this.ldapGroupDescription = ldapGroupDescription;
    }

    public String getLdapGroupUsernames()
    {
        return ldapGroupUsernames;
    }

    public void setLdapGroupUsernames(String ldapGroupUsernames)
    {
        this.ldapGroupUsernames = ldapGroupUsernames;
    }

    public String getLdapUserGroup()
    {
        return ldapUserGroup;
    }

    public void setLdapUserGroup(String ldapUserGroup)
    {
        this.ldapUserGroup = ldapUserGroup;
    }

    public boolean isLdapUsermembershipUseForGroups()
    {
        return ldapUsermembershipUseForGroups;
    }

    public void setLdapUsermembershipUseForGroups(boolean ldapUsermembershipUseForGroups)
    {
        this.ldapUsermembershipUseForGroups = ldapUsermembershipUseForGroups;
    }

    public boolean isLdapUsermembershipUse()
    {
        return ldapUsermembershipUse;
    }

    public void setLdapUsermembershipUse(boolean ldapUsermembershipUse)
    {
        this.ldapUsermembershipUse = ldapUsermembershipUse;
    }

    public boolean isSynchroniseGroupMemberships()
    {
        return synchroniseGroupMemberships;
    }

    public void setSynchroniseGroupMemberships(boolean synchroniseGroupMemberships)
    {
        this.synchroniseGroupMemberships = synchroniseGroupMemberships;
    }

    public String getLdapGroupDn()
    {
        return ldapGroupDn;
    }

    public void setLdapGroupDn(String ldapGroupDn)
    {
        this.ldapGroupDn = ldapGroupDn;
    }

    public void setLdapPagedresults(boolean ldapPagedresults)
    {
        this.ldapPagedresults = ldapPagedresults;
    }

    public boolean isLdapPagedresults()
    {
        return ldapPagedresults;
    }

    public void setLdapPagedresultsSize(String ldapPagedresultsSize)
    {
        this.ldapPagedresultsSize = ldapPagedresultsSize;
    }

    public String getLdapPagedresultsSize()
    {
        return ldapPagedresultsSize;
    }

    public void setLdapReferral(boolean ldapReferral)
    {
        this.ldapReferral = ldapReferral;
    }

    public boolean isLdapReferral()
    {
        return ldapReferral;
    }


    //----------------------------------------------------------------------

    //----------------------------------------------------------------------
    // Converters

    public Map<String, String> toAttributesMap()
    {
        HashMap<String, String> map = new HashMap<String, String>(15);
        map.put(LDAPPropertiesMapper.LDAP_URL_KEY, ldapUrl);
        map.put(LDAPPropertiesMapper.LDAP_BASEDN_KEY, ldapBasedn);
        map.put(LDAPPropertiesMapper.LDAP_USERDN_KEY, ldapUserdn);
        map.put(LDAPPropertiesMapper.LDAP_PASSWORD_KEY, ldapPassword);
        map.put(LDAPPropertiesMapper.USER_DN_ADDITION, ldapUserDn);
        map.put(LDAPPropertiesMapper.USER_OBJECTCLASS_KEY, ldapUserObjectclass);
        map.put(LDAPPropertiesMapper.USER_OBJECTFILTER_KEY, ldapUserFilter);
        map.put(LDAPPropertiesMapper.USER_USERNAME_KEY, ldapUserUsername);
        map.put(LDAPPropertiesMapper.USER_USERNAME_RDN_KEY, ldapUserUsernameRdn);
        map.put(LDAPPropertiesMapper.USER_FIRSTNAME_KEY, ldapUserFirstname);
        map.put(LDAPPropertiesMapper.USER_LASTNAME_KEY, ldapUserLastname);
        map.put(LDAPPropertiesMapper.USER_DISPLAYNAME_KEY, ldapUserDisplayname);
        map.put(LDAPPropertiesMapper.USER_EMAIL_KEY, ldapUserEmail);
        map.put(DelegatedAuthenticationDirectory.ATTRIBUTE_LDAP_DIRECTORY_CLASS, delegatedToClass);
        map.put(DelegatedAuthenticationDirectory.ATTRIBUTE_CREATE_USER_ON_AUTH, String.valueOf(createUserOnAuth));
        map.put(DelegatedAuthenticationDirectory.ATTRIBUTE_UPDATE_USER_ON_AUTH, String.valueOf(updateUserOnAuth));
        map.put(DirectoryImpl.ATTRIBUTE_KEY_AUTO_ADD_GROUPS, ldapAutoAddGroups);
        map.put(LDAPPropertiesMapper.GROUP_OBJECTCLASS_KEY, ldapGroupObjectclass);
        map.put(LDAPPropertiesMapper.GROUP_OBJECTFILTER_KEY, ldapGroupFilter);
        map.put(LDAPPropertiesMapper.GROUP_NAME_KEY, ldapGroupName);
        map.put(LDAPPropertiesMapper.GROUP_DESCRIPTION_KEY, ldapGroupDescription);
        map.put(LDAPPropertiesMapper.GROUP_USERNAMES_KEY, ldapGroupUsernames);
        map.put(LDAPPropertiesMapper.USER_GROUP_KEY, ldapUserGroup);
        map.put(LDAPPropertiesMapper.LDAP_USING_USER_MEMBERSHIP_ATTRIBUTE_FOR_GROUP_MEMBERSHIP, String.valueOf(ldapUsermembershipUseForGroups));
        map.put(LDAPPropertiesMapper.LDAP_USING_USER_MEMBERSHIP_ATTRIBUTE, String.valueOf(ldapUsermembershipUse));
        map.put(DelegatedAuthenticationDirectory.ATTRIBUTE_KEY_IMPORT_GROUPS, String.valueOf(synchroniseGroupMemberships));
        map.put(LDAPPropertiesMapper.GROUP_DN_ADDITION, ldapGroupDn);
        map.put(LDAPPropertiesMapper.LDAP_PAGEDRESULTS_KEY, String.valueOf(ldapPagedresults));
        map.put(LDAPPropertiesMapper.LDAP_PAGEDRESULTS_SIZE, ldapPagedresultsSize);
        map.put(LDAPPropertiesMapper.LDAP_REFERRAL_KEY, String.valueOf(ldapReferral));
        return map;
    }

    public static LdapDelegatingDirectoryAttributes fromAttributesMap(Map<String, String> map)
    {
        final LdapDelegatingDirectoryAttributes attributes = new LdapDelegatingDirectoryAttributes();
        attributes.setLdapUrl(map.get(LDAPPropertiesMapper.LDAP_URL_KEY));
        attributes.setLdapBasedn(map.get(LDAPPropertiesMapper.LDAP_BASEDN_KEY));
        attributes.setLdapUserdn(map.get(LDAPPropertiesMapper.LDAP_USERDN_KEY));
        attributes.setLdapPassword(map.get(LDAPPropertiesMapper.LDAP_PASSWORD_KEY));
        attributes.setLdapUserDn(map.get(LDAPPropertiesMapper.USER_DN_ADDITION));
        attributes.setLdapUserObjectclass(map.get(LDAPPropertiesMapper.USER_OBJECTCLASS_KEY));
        attributes.setLdapUserFilter(map.get(LDAPPropertiesMapper.USER_OBJECTFILTER_KEY));
        attributes.setLdapUserUsername(map.get(LDAPPropertiesMapper.USER_USERNAME_KEY));
        attributes.setLdapUserUsernameRdn(map.get(LDAPPropertiesMapper.USER_USERNAME_RDN_KEY));
        attributes.setLdapUserFirstname(map.get(LDAPPropertiesMapper.USER_FIRSTNAME_KEY));
        attributes.setLdapUserLastname(map.get(LDAPPropertiesMapper.USER_LASTNAME_KEY));
        attributes.setLdapUserDisplayname(map.get(LDAPPropertiesMapper.USER_DISPLAYNAME_KEY));
        attributes.setLdapUserEmail(map.get(LDAPPropertiesMapper.USER_EMAIL_KEY));
        attributes.setCreateUserOnAuth(Boolean.valueOf(map.get(DelegatedAuthenticationDirectory.ATTRIBUTE_CREATE_USER_ON_AUTH)));
        attributes.setUpdateUserOnAuth(Boolean.valueOf(map.get(DelegatedAuthenticationDirectory.ATTRIBUTE_UPDATE_USER_ON_AUTH)));
        attributes.setDelegatedToClass(map.get(DelegatedAuthenticationDirectory.ATTRIBUTE_LDAP_DIRECTORY_CLASS));
        attributes.setLdapAutoAddGroups(map.get(DirectoryImpl.ATTRIBUTE_KEY_AUTO_ADD_GROUPS));
        attributes.setLdapGroupObjectclass(map.get(LDAPPropertiesMapper.GROUP_OBJECTCLASS_KEY));
        attributes.setLdapGroupFilter(map.get(LDAPPropertiesMapper.GROUP_OBJECTFILTER_KEY));
        attributes.setLdapGroupName(map.get(LDAPPropertiesMapper.GROUP_NAME_KEY));
        attributes.setLdapGroupDescription(map.get(LDAPPropertiesMapper.GROUP_DESCRIPTION_KEY));
        attributes.setLdapGroupUsernames(map.get(LDAPPropertiesMapper.GROUP_USERNAMES_KEY));
        attributes.setLdapUserGroup(map.get(LDAPPropertiesMapper.USER_GROUP_KEY));
        attributes.setLdapUsermembershipUseForGroups(Boolean.valueOf(map.get(LDAPPropertiesMapper.LDAP_USING_USER_MEMBERSHIP_ATTRIBUTE_FOR_GROUP_MEMBERSHIP)));
        attributes.setLdapUsermembershipUse(Boolean.valueOf(map.get(LDAPPropertiesMapper.LDAP_USING_USER_MEMBERSHIP_ATTRIBUTE)));
        attributes.setSynchroniseGroupMemberships(Boolean.valueOf(map.get(DelegatedAuthenticationDirectory.ATTRIBUTE_KEY_IMPORT_GROUPS)));
        attributes.setLdapGroupDn(map.get(LDAPPropertiesMapper.GROUP_DN_ADDITION));
        attributes.setLdapPagedresults(Boolean.valueOf(map.get(LDAPPropertiesMapper.LDAP_PAGEDRESULTS_KEY)));
        attributes.setLdapPagedresultsSize(map.get(LDAPPropertiesMapper.LDAP_PAGEDRESULTS_SIZE));
        attributes.setLdapReferral(Boolean.valueOf(map.get(LDAPPropertiesMapper.LDAP_REFERRAL_KEY)));
        return attributes;
    }
}
