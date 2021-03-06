package com.atlassian.crowd.plugin.rest.entity;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collections;
import java.util.List;

/**
 * REST version of an AuthenticationContext.
 */
@XmlRootElement (name = "authentication-context")
@XmlAccessorType (XmlAccessType.FIELD)
public class AuthenticationContextEntity
{
    @XmlElement (name = "username")
    private final String username;

    @XmlElement (name = "password")
    private final String password;

    @XmlElement (name = "validation-factors")
    private final ValidationFactorEntityList validationFactors;

    private AuthenticationContextEntity()
    {
        username = null;
        password = null;
        validationFactors = null;
    }

    public AuthenticationContextEntity(final String name, final String password, final ValidationFactorEntityList validationFactors)
    {
        this.username = name;
        this.password = password;
        this.validationFactors = validationFactors;
    }

    public String getUserName()
    {
        return username;
    }

    public String getPassword()
    {
        return password;
    }

    public List<ValidationFactorEntity> getValidationFactors()
    {
        if (validationFactors != null)
        {
            return validationFactors.getValidationFactors();
        }
        else
        {
            return Collections.emptyList();
        }
    }
}
