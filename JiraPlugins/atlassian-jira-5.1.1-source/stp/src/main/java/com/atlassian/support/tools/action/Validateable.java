package com.atlassian.support.tools.action;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.support.tools.ValidationLog;
import com.atlassian.support.tools.servlet.SafeHttpServletRequest;

public interface Validateable
{
	void validate(Map<String, Object> context, SafeHttpServletRequest req, ValidationLog validationLog);
}
