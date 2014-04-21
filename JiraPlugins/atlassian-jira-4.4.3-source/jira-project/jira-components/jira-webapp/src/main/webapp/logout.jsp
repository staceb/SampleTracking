<%@ page import="com.atlassian.jira.web.exception.InvalidDirectJspCallException" %>
<%@ page import="webwork.action.ActionContext" %>
<%@ taglib uri="webwork" prefix="ww" %>
<%@ taglib uri="webwork" prefix="aui" %>
<%
    if (ActionContext.getValueStack().findValue("/") == null)
    {
        throw new InvalidDirectJspCallException("Calling logout.jsp directly. This is no longer supported.", "logout.jsp");
    }
%>
<html>
<head>
    <meta content="message" name="decorator" />
	<title><ww:text name="'logout.title'"/></title>
</head>
<body>
    <h1 id="logged-out"><ww:text name="'logout.title'"/></h1>
    <aui:component template="auimessage.jsp" theme="'aui'">
        <aui:param name="'messageType'">info</aui:param>
        <aui:param name="'titleText'"><ww:text name="'logout.desc.line1'" /></aui:param>
        <aui:param name="'messageHtml'">
            <p>
                <ww:text name="'logout.desc.line2'">
                    <ww:param name="'value0'"><a href="<%= request.getContextPath() %>/secure/"></ww:param>
                    <ww:param name="'value1'"></a></ww:param>
                </ww:text>
            </p>
        </aui:param>
    </aui:component>
</body>
</html>
