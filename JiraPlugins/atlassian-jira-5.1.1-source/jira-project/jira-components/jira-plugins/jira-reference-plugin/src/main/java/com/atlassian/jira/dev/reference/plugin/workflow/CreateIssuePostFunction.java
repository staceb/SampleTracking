package com.atlassian.jira.dev.reference.plugin.workflow;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.index.IssueIndexManager;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.ImportUtils;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.workflow.FunctionProvider;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Creates an issue in a post-function to test https://jira.atlassian.com/browse/JRA-26172
 *
 * @since v4.4
 */
public class CreateIssuePostFunction implements FunctionProvider
{

    private static final Logger log = Logger.getLogger(CreateIssuePostFunction.class);

    private final IssueManager issueManager;
    private final IssueFactory issueFactory;
    private final JiraAuthenticationContext authenticationContext;
    private final OfBizDelegator ofBizDelegator;
    private final IssueIndexManager issueIndexManager;

    public CreateIssuePostFunction(IssueManager issueManager, IssueFactory issueFactory, JiraAuthenticationContext authenticationContext, OfBizDelegator ofBizDelegator, IssueIndexManager issueIndexManager)
    {
        this.issueManager = issueManager;
        this.issueFactory = issueFactory;
        this.authenticationContext = authenticationContext;
        this.ofBizDelegator = ofBizDelegator;
        this.issueIndexManager = issueIndexManager;
    }

    // As this is a post function, this will get executed in the scope of the transaction
    // started by the workflow manager.  I create one issue in this thread, and spawn a separate
    // thread to create an isssue.  This tests the key generation logic (get next id) - if the transaction logic is
    // faulty then the test will either deadlock or create duplicate keys.
    public void execute(Map transientVars, Map args, PropertySet ps)
    {
        final Issue originalIssue = (Issue)transientVars.get("originalissueobject");
        final User user = authenticationContext.getLoggedInUser();
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        try
        {
            GenericValue gv = new IssueCreator().create(issueFactory.cloneIssue(originalIssue), user);
            Future<GenericValue> future = executorService.submit(new ThreadedIssueCreator(issueFactory.cloneIssue(originalIssue), user));
            //enable indexes
            issueIndexManager.reIndex(gv);
            issueIndexManager.reIndex(future.get());
            executorService.shutdown();
        }
        catch (Exception e)
        {
            log.error("Problem creating issue: " + e.getMessage());
        }
    }

    class IssueCreator
    {
        public GenericValue create(Issue issue, User user) throws Exception
        {
            GenericValue gv = issueManager.createIssue(user, issue);
            issue.store();
            return gv;
        };
    }

    class ThreadedIssueCreator implements Callable<GenericValue>
    {
        final Issue issue;
        final User user;

        public ThreadedIssueCreator(Issue issue, User user)
        {
            this.issue = issue;
            this.user = user;
        }

        @Override
        public GenericValue call() throws Exception
        {
            return new IssueCreator().create(issue, user);
        };
    }
}
