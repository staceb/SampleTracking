<%@ page import="com.atlassian.jira.ComponentManager" %>
<%@ page import="com.atlassian.plugin.webresource.WebResourceManager" %>
<%@ taglib prefix="ww" uri="webwork" %>
<%@ taglib prefix="aui" uri="webwork" %>
<%@ taglib prefix="page" uri="sitemesh-page" %>
<html>
<head>
    <meta content="frontpage" name="decorator"/>
    <title><ww:text name="'signup.title'"/></title>
</head>
<%
    WebResourceManager webResourceManager = ComponentManager.getInstance().getWebResourceManager();
    webResourceManager.requireResource("jira.webresources:captcha");
%>
<body class="type-a">
    <div class="content intform">
        <page:applyDecorator id="signup" name="auiform">
            <page:param name="action">Signup.jspa</page:param>
            <page:param name="submitButtonName">Signup</page:param>
            <page:param name="submitButtonText"><ww:text name="'signup.heading'"/></page:param>
            <page:param name="cancelLinkURI"><ww:url value="'default.jsp'" atltoken="false"/></page:param>

            <aui:component template="formHeading.jsp" theme="'aui'">
                <aui:param name="'text'"><ww:text name="'signup.heading'"/></aui:param>
            </aui:component>

            <aui:component template="formDescriptionBlock.jsp" theme="'aui'">
                <aui:param name="'messageHtml'">
                    <p><ww:text name="'signup.desc'"/></p>
                </aui:param>
            </aui:component>

            <page:applyDecorator name="auifieldset">
                <page:param name="legend"><ww:text name="'signup.fieldset.legend.details'"/></page:param>

                <page:applyDecorator name="auifieldgroup">
                    <aui:textfield id="'username'" label="text('common.words.username')" mandatory="'true'" maxlength="'255'" name="'username'" theme="'aui'"/>
                </page:applyDecorator>
                <page:applyDecorator name="auifieldgroup">
                    <aui:password id="'password'" label="text('common.words.password')" mandatory="'true'" maxlength="'255'" name="'password'" theme="'aui'"/>
                </page:applyDecorator>
                <page:applyDecorator name="auifieldgroup">
                    <aui:password id="'confirm'" label="text('signup.confirmPassword')" mandatory="'true'" maxlength="'255'" name="'confirm'" theme="'aui'"/>
                </page:applyDecorator>
                <page:applyDecorator name="auifieldgroup">
                    <aui:textfield id="'fullname'" label="text('common.words.fullname')" mandatory="'true'" maxlength="'255'" name="'fullname'" theme="'aui'"/>
                </page:applyDecorator>
                <page:applyDecorator name="auifieldgroup">
                    <aui:textfield id="'email'" label="text('common.words.email')" mandatory="'true'" maxlength="'255'" name="'email'" theme="'aui'"/>
                </page:applyDecorator>

                <ww:if test="applicationProperties/option('jira.option.captcha.on.signup') == true">
                    <page:applyDecorator id="'captcha'" name="auifieldgroup">
                        <aui:component label="text('signup.captcha.text')" id="'os_captcha'" name="'captcha'" template="captcha.jsp" theme="'aui'">
                            <aui:param name="'captchaURI'"><%= request.getContextPath() %>/captcha</aui:param>
                            <aui:param name="'iconText'"><ww:text name="'admin.common.words.refresh'"/></aui:param>
                            <aui:param name="'iconCssClass'">icon-reload reload</aui:param>                            
                        </aui:component>
                    </page:applyDecorator>
                </ww:if>

            </page:applyDecorator>

        </page:applyDecorator>
    </div>
</body>
</html>