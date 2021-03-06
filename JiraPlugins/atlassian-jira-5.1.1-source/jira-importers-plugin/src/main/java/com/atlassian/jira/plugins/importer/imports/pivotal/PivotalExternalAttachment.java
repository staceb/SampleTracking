/*
 * Copyright (c) 2011. Atlassian
 * All rights reserved
 */

package com.atlassian.jira.plugins.importer.imports.pivotal;

import com.atlassian.jira.plugins.importer.external.beans.ExternalAttachment;

import java.util.Date;

public class PivotalExternalAttachment extends ExternalAttachment {
	private final String url;

	public PivotalExternalAttachment(String fileName, String url, Date attachedDate) {
		super(fileName, null, attachedDate);
		this.url = url;
	}

	public String getUrl() {
		return url;
	}
}
