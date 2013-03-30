/*
 * Copyright (c) 2013 The Interedition Development Group.
 *
 * This file is part of CollateX.
 *
 * CollateX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CollateX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CollateX.  If not, see <http://www.gnu.org/licenses/>.
 */

package eu.interedition.text.http;

import com.google.common.util.concurrent.AbstractIdleService;
import com.sun.jersey.api.container.ContainerFactory;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.http.server.StaticHttpHandler;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Singleton
public class HttpService extends AbstractIdleService {

    private HttpServer httpServer;

    @Inject
    public HttpService(@Named("httpPort") int httpPort,
                       @Named("contextPath") String contextPath,
                       @Named("yuiRoot") String yuiRoot,
                       @Named("yuiPath") String yuiPath,
                       @Named("assetRoot") String assetRoot,
                       @Named("assetPath") String assetPath,
                       HttpResourceConfig httpResourceConfig) {

        this.httpServer = HttpServer.createSimpleServer(null, httpPort);

        final HttpHandler httpHandler = ContainerFactory.createContainer(HttpHandler.class, httpResourceConfig);

        final ServerConfiguration config = httpServer.getServerConfiguration();
        config.addHttpHandler(new CustomStaticHttpHandler(assetRoot, assetPath), assetPath + "/*");
        if (!yuiRoot.isEmpty()) {
            config.addHttpHandler(new CustomStaticHttpHandler(yuiRoot, yuiPath), yuiPath + "/*");
        }
        config.addHttpHandler(httpHandler, contextPath + "/*");
    }

    @Override
    protected void startUp() throws Exception {
        httpServer.start();
    }

    @Override
    protected void shutDown() throws Exception {
        httpServer.stop();
    }

    static class CustomStaticHttpHandler extends StaticHttpHandler {

        final String base;

        CustomStaticHttpHandler(String docRoot, String base) {
            super(docRoot);
            setFileCacheEnabled(false);
            this.base = base;
        }

        @Override
        protected String getRelativeURI(Request request) {
            String uri = request.getRequestURI();
            if (uri.contains("..")) {
                return null;
            }

            if (!base.isEmpty()) {
                if (!uri.startsWith(base)) {
                    return null;
                }

                uri = uri.substring(base.length());
            }

            return uri;
        }
    }
}
