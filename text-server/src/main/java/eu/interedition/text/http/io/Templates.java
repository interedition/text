package eu.interedition.text.http.io;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import eu.interedition.text.http.Configuration;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Singleton
public class Templates extends freemarker.template.Configuration {

    @Inject
    public Templates(Configuration configuration) {
        super();
        try {
            setSharedVariable("cp", configuration.get("contextPath"));
            setSharedVariable("ap", configuration.get("assetPath"));
            setSharedVariable("yp", configuration.get("yuiPath"));
            setAutoIncludes(Collections.singletonList("/header.ftl"));
            setDefaultEncoding("UTF-8");
            setOutputEncoding("UTF-8");
            setURLEscapingCharset("UTF-8");
            setStrictSyntaxMode(true);
            setWhitespaceStripping(true);
            setTemplateLoader(new FileTemplateLoader((File) configuration.get("templatePath")));
        } catch (TemplateModelException e) {
            throw Throwables.propagate(e);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    public void write(String name, Object model, Writer writer) throws IOException {
        try {
            getTemplate(name).process(model, writer);
        } catch (TemplateException e) {
            throw new WebApplicationException(e);
        }
    }

    public void write(String name, Writer writer) throws IOException {
        write(name, null, writer);
    }

    public Response render(String name, Object model) throws IOException {
        final StringWriter entity = new StringWriter();
        write(name, model, entity);
        return Response.ok().type(MediaType.TEXT_HTML_TYPE).entity(entity.toString()).build();
    }

    public Response render(String name) throws IOException {
        return render(name, null);
    }
}
