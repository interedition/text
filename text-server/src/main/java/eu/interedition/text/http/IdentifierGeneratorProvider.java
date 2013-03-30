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

import com.google.inject.Provider;
import eu.interedition.text.ld.IdentifierGenerator;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.sql.DataSource;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
@Singleton
public class IdentifierGeneratorProvider implements Provider<IdentifierGenerator> {

    final String sequence;

    @Inject
    DataSource dataSource;

    public IdentifierGeneratorProvider(String name) {
        this.sequence = ("interedition_" + name + "_id");
    }

    @Override
    public IdentifierGenerator get() {
        return new IdentifierGenerator(dataSource, sequence).withSchema();
    }
}
