@InterceptorChain ({RequestScopeInterceptor.class, ExceptionInterceptor.class, ExpandInterceptor.class})
package com.atlassian.jira.rest.v2.admin;

import com.atlassian.jira.rest.exception.ExceptionInterceptor;
import com.atlassian.jira.rest.v2.issue.scope.RequestScopeInterceptor;
import com.atlassian.plugins.rest.common.expand.interceptor.ExpandInterceptor;
import com.atlassian.plugins.rest.common.interceptor.InterceptorChain;
