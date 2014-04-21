package com.atlassian.jira.configurator.db;

import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.configurator.config.ValidationException;
import com.atlassian.jira.exception.ParseException;
import com.atlassian.jira.local.ListeningTestCase;

public class TestPostgresDatabaseConfig extends ListeningTestCase
{
    @Test
    public void testGetUrl() throws Exception
    {
        PostgresDatabaseConfig postgresDatabaseConfig = new PostgresDatabaseConfig();
        //  http://doc.postgresintl.com/jdbc/ch03s03.html
        //  jdbc:postgresql:database
        //  jdbc:postgresql://host/database
        //  jdbc:postgresql://host:port/database

        // database only - we can make a simple URL
        assertEquals("jdbc:postgresql:JIRA", postgresDatabaseConfig.getUrl("", "", "JIRA"));
        // whitespace trimmed
        assertEquals("jdbc:postgresql:JIRA", postgresDatabaseConfig.getUrl("  \t \n \r", "    ", "   JIRA    "));
        // jdbc:postgresql://host/database
        assertEquals("jdbc:postgresql://192.168.3.201/JIRA", postgresDatabaseConfig.getUrl("192.168.3.201", "   ", "JIRA"));
        // jdbc:postgresql://host:port/database
        assertEquals("jdbc:postgresql://db.acme.com:4433/mydb", postgresDatabaseConfig.getUrl("db.acme.com", "4433", "mydb"));
        assertEquals("jdbc:postgresql://localhost:8543/JIRA", postgresDatabaseConfig.getUrl("    localhost    ", " 8543  ", "  JIRA    "));
        // Now the special case where the User has named a port, but leaves the DB blank
        // Postgres cannot handle this in the URL, so we expand the blank host into "localhost"
        assertEquals("jdbc:postgresql://localhost:1234/mydb", postgresDatabaseConfig.getUrl("", "1234", "mydb"));

        // Invalid: database is required
        try
        {
            postgresDatabaseConfig.getUrl("localhost", "4433", "");
            fail();
        }
        catch (ValidationException ex)
        {
            // cool
        }
    }
    
    @Test
    public void testWrongPrefix()
    {
        PostgresDatabaseConfig postgresDatabaseConfig = new PostgresDatabaseConfig();
        try
        {
            postgresDatabaseConfig.parseUrl("jdbc:postgresql#");
            fail();
        }
        catch (ParseException e)
        {
            // cool
        }
    }

    @Test
    public void testInvalid()
    {
        PostgresDatabaseConfig postgresDatabaseConfig = new PostgresDatabaseConfig();
        try
        {
            postgresDatabaseConfig.parseUrl("jdbc:postgresql://fred");
            fail();
        }
        catch (ParseException e)
        {
            // cool
        }
    }

    @Test
    public void testMin() throws ParseException
    {
        PostgresDatabaseConfig postgresDatabaseConfig = new PostgresDatabaseConfig();
        DatabaseInstance connectionProperties = postgresDatabaseConfig.parseUrl("jdbc:postgresql:fred");
        assertEquals("", connectionProperties.getHostname());
        assertEquals("", connectionProperties.getPort());
        assertEquals("fred", connectionProperties.getInstance());
    }

    @Test
    public void testFull() throws ParseException
    {
        //  jdbc:postgresql://host:port/database
        PostgresDatabaseConfig postgresDatabaseConfig = new PostgresDatabaseConfig();
        DatabaseInstance connectionProperties = postgresDatabaseConfig.parseUrl("jdbc:postgresql://beast:123/jira");
        assertEquals("beast", connectionProperties.getHostname());
        assertEquals("123", connectionProperties.getPort());
        assertEquals("jira", connectionProperties.getInstance());
    }

    @Test
    public void testDefaultPort() throws ParseException
    {
        PostgresDatabaseConfig postgresDatabaseConfig = new PostgresDatabaseConfig();
        DatabaseInstance connectionProperties = postgresDatabaseConfig.parseUrl("jdbc:postgresql://beast/jira");
        assertEquals("beast", connectionProperties.getHostname());
        assertEquals("", connectionProperties.getPort());
        assertEquals("jira", connectionProperties.getInstance());
    }
}
