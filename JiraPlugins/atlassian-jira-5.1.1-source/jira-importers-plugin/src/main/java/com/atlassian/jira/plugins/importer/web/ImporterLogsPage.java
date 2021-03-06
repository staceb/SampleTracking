/*
 * Copyright (C) 2002-2011 Atlassian
 * All rights reserved.
 */
package com.atlassian.jira.plugins.importer.web;

import com.atlassian.jira.plugins.importer.extensions.ImporterController;
import com.atlassian.jira.plugins.importer.imports.importer.ImportDataBean;
import com.atlassian.jira.plugins.importer.imports.importer.JiraDataImporter;
import com.atlassian.jira.plugins.importer.imports.importer.impl.ImporterCallable;
import com.atlassian.jira.plugins.importer.tracking.UsageTrackingService;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.task.TaskContext;
import com.atlassian.jira.task.TaskDescriptor;
import com.atlassian.jira.task.TaskManager;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.web.WebInterfaceManager;
import com.atlassian.sal.api.websudo.WebSudoNotRequired;

import javax.annotation.Nullable;

@WebSudoNotRequired
public class ImporterLogsPage extends ImporterProcessSupport {

	private final TaskManager taskManager;

	public ImporterLogsPage(UsageTrackingService usageTrackingService, TaskManager taskManager, WebInterfaceManager webInterfaceManager, PluginAccessor pluginAccessor) {
		super(usageTrackingService, webInterfaceManager, pluginAccessor);
		this.taskManager = taskManager;
    }

    @SuppressWarnings("unused")
	public String doViewLogs() throws Exception {
		return doExecute();
	}

	@Override
	public String doDefault() throws Exception {
		return doExecute();
	}

	@Override
	protected String doExecute() throws Exception {
		if (!isAdministrator()) {
			return "denied";
		}

		if (getImporter() != null && getImporter().getStats() != null) {
			if (getImporter().isRunning()) {
				return "logs";
			} else {
				return getRedirect(ImporterFinishedPage.class.getSimpleName()
						+ "!default.jspa?externalSystem=" + getExternalSystem()
						+ "&atl_token=" + getXsrfToken());
			}
		} else {
			return "restartimporterneeded";
		}
	}

	/**
	 * Do the actual import
	 *
	 * @return view name
	 */
	@RequiresXsrfCheck
    @SuppressWarnings("unused")
	public String doImport() {
		final JiraDataImporter importer = getImporter();
		final ImporterController controller = getController();
		final ImportProcessBean bean = controller != null ? controller.getImportProcessBeanFromSession() : null;

		if (importer == null || bean == null || controller == null) {
			return "restartimporterneeded";
		}

		if (!importer.isRunning()) {
			importer.setRunning();
			importer.initializeLog();

			try {
				final ImportDataBean dataBean = controller.createDataBean();
				importer.setDataBean(dataBean);

				TaskDescriptor<Void> task = taskManager.submitTask(new ImporterCallable(importer, getLoggedInUser()),
						"JIRA Importers Plugin Main Import Task", new TaskContext() {
					public String buildProgressURL(Long aLong) {
						return null;
					}
				});
			} catch(Exception e) {
				if(importer.getLog() != null) {
					importer.getLog().fail(e, "Failed to start import");
				}
			}
		}

		return getRedirect("ImporterLogsPage!viewLogs.jspa?externalSystem=" + getExternalSystem()
				+ "&atl_token=" + getXsrfToken());
	}

	@Nullable
	public JiraDataImporter getImporter() {
		ImporterController controller = getController();
		return controller != null ? controller.getImporter() : null;
	}

	@Override
	public String getFormTitle() {
		if (getImporter().isAborted()) {
		    return getText("jira-importer-plugin.ImporterLogsPage.stopping");
		} else {
			return getText("jira-importer-plugin.ImporterLogsPage.title",
					getImporter().getSelectedProjects().size());
		}
	}

	@Override
	public String getFormDescription() {
		return getText("jira-importer-plugin.ImporterLogsPage.projects.imported", getTitle());
	}
}
