package eu.interedition.text.http.io;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.io.Closeables;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Singleton
public class Templates extends Configuration {

    @Inject
    public Templates(@Named("templatePath") String templatePath,
                     @Named("contextPath") String contextPath,
                     @Named("assetPath") String assetPath,
                     @Named("yuiPath") String yuiPath) {
        super();
        try {
            setSharedVariable("cp", contextPath);
            setSharedVariable("ap", assetPath);
            setSharedVariable("yp", yuiPath);
            setAutoIncludes(Collections.singletonList("/header.ftl"));
            setDefaultEncoding("UTF-8");
            setOutputEncoding("UTF-8");
            setURLEscapingCharset("UTF-8");
            setStrictSyntaxMode(true);
            setWhitespaceStripping(true);
            setTemplateLoader(Strings.isNullOrEmpty(templatePath)
                    ? new ClassTemplateLoader(getClass(), "/templates")
                    : new FileTemplateLoader(new File(templatePath))
            );
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
