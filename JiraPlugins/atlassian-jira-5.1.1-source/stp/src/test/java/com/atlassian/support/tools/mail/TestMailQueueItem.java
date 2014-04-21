package com.atlassian.support.tools.mail;

import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;

import junit.framework.TestCase;

import org.apache.kahadb.util.ByteArrayInputStream;

import com.atlassian.support.tools.mock.MockApplicationInfo;
import com.atlassian.support.tools.salext.mail.SupportRequest;
import com.atlassian.support.tools.salext.mail.SupportRequestAttachment;
import com.atlassian.support.tools.salext.mail.SupportRequestMailQueueItem;


public class TestMailQueueItem extends TestCase
{
	/**
	 * Test for JAC:CONFDEV-3464
	 */
	public void testSerialization() throws Exception
	{
		MockApplicationInfo info = new MockApplicationInfo();
		
		SupportRequest requestInfo = new SupportRequest();
		requestInfo.setBody("A body of the request for support email");
		requestInfo.addHeader("key", "value");
		requestInfo.addAttachment(new SupportRequestAttachment("properties", "text", requestInfo.saveForMail(new MockApplicationInfo())));
		
		SupportRequestMailQueueItem item = new SupportRequestMailQueueItem(requestInfo, info);
		
		Thread.sleep(100);
		assertTrue(item.getDateQueued().before(new Date()));

		final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(item);
		out.flush();
		out.close();
		
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
		SupportRequestMailQueueItem item2 = (SupportRequestMailQueueItem) in.readObject();
		
		assertEquals("Support Mail Queue Item is not serializable: date", item.getDateQueued(), item2.getDateQueued());
		assertEquals("Support Mail Queue Item is not serializable: body", item.getSupportRequest().getBody(), item2.getSupportRequest().getBody());
	}
}
