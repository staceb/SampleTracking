package com.atlassian.templaterenderer.velocity.one.six.internal;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Map;

import com.atlassian.templaterenderer.RenderingException;
import com.atlassian.templaterenderer.TemplateContextFactory;
import com.atlassian.templaterenderer.velocity.CompositeClassLoader;
import com.atlassian.templaterenderer.velocity.TemplateRendererAnnotationBoxingUberspect;
import com.atlassian.templaterenderer.velocity.TemplateRendererHtmlAnnotationEscaper;
import com.atlassian.templaterenderer.velocity.one.six.VelocityTemplateRenderer;
import com.atlassian.velocity.htmlsafe.HtmlSafeDirective;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.CommonsLogLogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

/**
 * A velocity template renderer
 */
public class VelocityTemplateRendererImpl implements VelocityTemplateRenderer
{
    private final ClassLoader classLoader;
    private final String pluginKey;
    private final TemplateContextFactory templateContextFactory;

    private final VelocityEngine velocity;

    public VelocityTemplateRendererImpl(ClassLoader classLoader,
            String pluginKey,
            Map<String, String> properties,
            TemplateContextFactory templateContextFactory)
    {
        this.classLoader = classLoader;
        this.pluginKey = pluginKey;
        this.templateContextFactory = templateContextFactory;

        velocity = new VelocityEngine();
        overrideProperty(Velocity.RUNTIME_LOG_LOGSYSTEM_CLASS, CommonsLogLogChute.class.getName());
        overrideProperty(Velocity.RESOURCE_LOADER, "classpath");
        overrideProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        overrideProperty("classpath.resource.loader.cache", Boolean.toString(!Boolean.getBoolean("atlassian.dev.mode")));
        overrideProperty("runtime.introspector.uberspect", TemplateRendererAnnotationBoxingUberspect.class.getName());
        overrideProperty("userdirective", HtmlSafeDirective.class.getName());
        overrideProperty("eventhandler.referenceinsertion.class", TemplateRendererHtmlAnnotationEscaper.class.getName());
        for (Map.Entry<String, String> prop : properties.entrySet())
        {
        	overrideProperty(prop.getKey(), prop.getValue());
        }

        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        // Don't use the context class loader here since it is a OSGIBundleDelegatingClassLoader that actually uses the originating
        // bundle. Essentially the same as the classLoader passed in.  Using this.getClass.getClassLoader() uses *this* bundles
        // classloader meaning the right version (the version this bundle depends on) of velocity will be loaded.
        final CompositeClassLoader compositeClassLoader =
            new CompositeClassLoader(this.getClass().getClassLoader(), classLoader);
        Thread.currentThread().setContextClassLoader(compositeClassLoader);
        try
        {
            velocity.init();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public void render(String templateName, Writer writer) throws RenderingException, IOException
    {
        render(templateName, Collections.<String, Object>emptyMap(), writer);
    }

    public void render(String templateName, Map<String, Object> context, Writer writer)
        throws RenderingException, IOException
    {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(classLoader);
        try
        {
            Template template = velocity.getTemplate(templateName);
            template.merge(createContext(context), writer);
            writer.flush();
        }
        catch (IOException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new RenderingException(e);
        }
        finally
        {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    public String renderFragment(String fragment, Map<String, Object> context)
    {
        try
        {
            StringWriter tempWriter = new StringWriter(fragment.length());
            velocity.evaluate(createContext(context), tempWriter, "renderFragment", fragment);
            return tempWriter.toString();
        }
        catch (Exception e)
        {
            throw new RenderingException(e);
        }

    }

    private VelocityContext createContext(Map<String, Object> contextParams)
    {
        return new VelocityContext(templateContextFactory.createContext(pluginKey, contextParams));
    }

    private void overrideProperty(String key, Object value)
    {
    	// "userdirective" property can have multiple values; all other overrideable
    	// properties that we set at init time should have single values
    	if (key.equals("userdirective"))
    	{
    		velocity.addProperty(key, value);
    	}
    	else
    	{
    		velocity.setProperty(key, value);
    	}
    }

    /**
     * Check whether the given template exists or not
     */
    public boolean resolve(String templateName)
    {
        return classLoader.getResource(templateName) != null;
    }
}
