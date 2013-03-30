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

import com.google.common.base.Throwables;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.reflect.ClassPath;
import com.google.inject.Injector;
import com.sun.jersey.api.container.filter.GZIPContentEncodingFilter;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.server.impl.container.filter.NormalizeFilter;

import javax.inject.Inject;
import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class HttpResourceConfig extends DefaultResourceConfig {

    private final HashSet<Object> singletons;

    @Inject
    public HttpResourceConfig(Injector injector) {
        super();

        this.singletons = Sets.newHashSet();

        try {
            final Class<?> thisClass = getClass();
            final Package thisPackage = thisClass.getPackage();
            for (ClassPath.ClassInfo classInfo : ClassPath.from(thisClass.getClassLoader()).getTopLevelClassesRecursive(thisPackage.getName())) {
                Class<?> candidate = classInfo.load();
                if (candidate.isAnnotationPresent(Path.class) || candidate.isAnnotationPresent(Provider.class)) {
                    singletons.add(injector.getInstance(candidate));
                }
            }
            final HashMap<String,Object> config = Maps.newHashMap();
            config.put(PROPERTY_CONTAINER_REQUEST_FILTERS, Arrays.<Class<?>>asList(NormalizeFilter.class, GZIPContentEncodingFilter.class));
            config.put(PROPERTY_CONTAINER_RESPONSE_FILTERS, Arrays.<Class<?>>asList(GZIPContentEncodingFilter.class));
            config.put(FEATURE_CANONICALIZE_URI_PATH, true);
            config.put(FEATURE_NORMALIZE_URI, true);
            config.put(FEATURE_REDIRECT, true);
            setPropertiesAndFeatures(config);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public Set<Object> getSingletons() {
        return this.singletons;
    }
}
