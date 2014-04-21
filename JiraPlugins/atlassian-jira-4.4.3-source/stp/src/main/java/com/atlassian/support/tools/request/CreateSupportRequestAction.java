package com.atlassian.support.tools.request;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.KeyException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.EmailValidator;

import com.atlassian.support.tools.ValidationLog;
import com.atlassian.support.tools.action.SupportToolsAction;
import com.atlassian.support.tools.salext.SupportApplicationInfo;
import com.atlassian.support.tools.salext.mail.MailUtility;
import com.atlassian.support.tools.salext.mail.SupportRequest;
import com.atlassian.support.tools.salext.mail.SupportRequestAttachment;
import com.atlassian.support.tools.zip.FileOptionsValidator;
import com.atlassian.support.tools.zip.ZipUtility;
import com.atlassian.templaterenderer.RenderingException;
import com.atlassian.templaterenderer.TemplateRenderer;

public class CreateSupportRequestAction implements SupportToolsAction
{
	private final SupportApplicationInfo info;
	private final MailUtility mailUtility;
	private SupportRequest supportRequest = new SupportRequest();
	
	public CreateSupportRequestAction(SupportApplicationInfo info, MailUtility mailUtility)
	{
		this.info = info;
		this.mailUtility = mailUtility;
	}

	@Override
	public void prepare(Map<String, Object> context, HttpServletRequest request, ValidationLog validationLog)
	{
		supportRequest = new SupportRequest();
		supportRequest.setDescription(getParameter(request, "description"));
		supportRequest.setFromAddress(getParameter(request, "contactEmail"));
		supportRequest.setSubject(getParameter(request, "subject"));
		supportRequest.setTimeZone(getParameter(request, "timeZone"));
		supportRequest.setPriority(toInt(request.getParameter("priority"), SupportRequest.DEFAULT_PRIORITY));
		supportRequest.setToAddress(info.getCreateSupportRequestEmail());

		context.put("description", supportRequest.getDescription());
		context.put("contactEmail", supportRequest.getFromAddress());
		context.put("subject", supportRequest.getSubject());
		context.put("timeZone", supportRequest.getTimeZone());
		context.put("priority", String.valueOf(supportRequest.getPriority()));

		context.put("mailQueueURL", this.info.getMailQueueURL(request));
		context.put("mailUtility", this.mailUtility);
		this.info.flagSelectedApplicationFileBundles(request);

		// this is the only check that we run before processing the form
		validateMailServerConfiguration(request, validationLog);
	}

	private String generateMailBody() throws RenderingException, IOException
	{
		Map<String, Object> context = new HashMap<String, Object>();
		context.put("action", this);
		context.put("appInfo", this.info);
		context.put("description", supportRequest.getDescription());
		context.put("contactEmail", supportRequest.getFromAddress());
		context.put("priority", String.valueOf(supportRequest.getPriority()));

		StringWriter writer = new StringWriter();
		TemplateRenderer templateRenderer = this.info.getTemplateRenderer();
		templateRenderer.render(getTemplateFile(), context, writer);

		return writer.toString();
	}

	protected File generateZipFile() throws IOException, KeyException, GeneralSecurityException
	{
		return ZipUtility.createSupportZip(this.info.getApplicationFileBundles(), this.info);
	}

	@Override
	public void validate(Map<String, Object> context, HttpServletRequest req, ValidationLog validationLog)
	{
		if(StringUtils.isBlank(req.getParameter("subject")))
		{
			validationLog.addFieldError("subject", "stp.create.support.request.subject.empty");
		}
		
		if(StringUtils.isBlank(req.getParameter("description")))
		{
			validationLog.addFieldError("description", "stp.create.support.request.description.empty");
		}
		
		// Check the email address
		String contactEmail = req.getParameter("contactEmail");
		if(StringUtils.isBlank(contactEmail))
		{
			validationLog.addFieldError("contactEmail", "stp.create.support.request.from.empty");
		}
		else if(!isValidEmail(contactEmail))
		{
			validationLog.addFieldError("contactEmail", "stp.create.support.request.from.invalid", StringEscapeUtils.escapeHtml(contactEmail));
		}
		
		// Run the general check for Zips to see if any files were selected
		new FileOptionsValidator(this.info, false).validate(context, req, validationLog);
	}

	public static boolean isValidEmail(String emailAddress)
	{
		EmailValidator validator = EmailValidator.getInstance();
		return validator.isValid(emailAddress);
	}
	
	protected void validateMailServerConfiguration(HttpServletRequest request, ValidationLog validationLog)
	{
		if( ! this.mailUtility.isMailServerConfigured())
		{
			StringBuffer warningText = new StringBuffer();
			warningText.append(this.info.getText("stp.create.support.request.mail.configuration.warning"));
			warningText.append(" <a href=\"");
			warningText.append(this.info.getMailServerConfigurationURL(request));
			warningText.append("\">");
			warningText.append(this.info.getText("stp.create.support.request.mail.configuration.warning.link.text"));
			warningText.append("</a>.");
			validationLog.addLocalizedWarning(warningText.toString());
		}
	}

	@Override
	public void execute(Map<String, Object> context, HttpServletRequest req, ValidationLog validationLog) throws RenderingException, IOException, Exception
	{
		try
		{
			supportRequest.setBody(generateMailBody());
			
			File zipFile = generateZipFile();
			if(zipFile != null && zipFile.length() > 0)
			{	
				byte[] data = IOUtils.toByteArray(new FileInputStream(zipFile));
				supportRequest.addAttachment(new SupportRequestAttachment(zipFile.getName(), "application/zip", data));
			}
			
			this.mailUtility.sendMail(supportRequest, this.info);
		}
		catch(RenderingException e)
		{
			validationLog.addError("stp.mail.rendering.error", e);
			throw e;
		}
		catch(IOException e)
		{
			validationLog.addError("stp.mail.io.error", e);
			throw e;
		}
	}

	public String getTemplateFile()
	{
		return "/templates/email-create-request-body.vm";
	}

	@Override
	public String getCategory()
	{
		return "stp.contact.category.title";
	}

	@Override
	public String getTitle()
	{
		return "stp.create.support.request.title";
	}

	@Override
	public String getName()
	{
		return "create-support-request";
	}

	@Override
	public String getSuccessTemplatePath()
	{
		return "templates/create-support-request-execute.vm";
	}

	@Override
	public String getErrorTemplatePath()
	{
		return "templates/create-support-request-start.vm";
	}

	@Override
	public String getStartTemplatePath()
	{
		return "templates/create-support-request-start.vm";
	}

	@Override
	public SupportToolsAction newInstance()
	{
		return new CreateSupportRequestAction(this.info, this.mailUtility);
	}


    private static String getParameter(HttpServletRequest request, String paramName)
	{
    	String value = request.getParameter(paramName);
    	if(value == null)
    		return "";
    	else
    		return value;
	}

	private static int toInt(String str, int defaultValue) 
    {
        if(str == null) 
        {
            return defaultValue;
        }
        try 
        {
            return Integer.parseInt(str);
        } 
        catch (NumberFormatException nfe) 
        {
            return defaultValue;
        }
    }
}
