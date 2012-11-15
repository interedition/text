/*
 * S-Expression Parser in Java.
 *
 * Copyright (C) 2012 Joel F. Klein
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.interedition.text.lisp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * @author <a href="http://rosettacode.org/wiki/S-Expressions#Java" title="rosettacode.org">Joel F. Klein</a>
 */
public class ExpressionList extends ArrayList<Expression> implements Expression {
    private ExpressionList parent = null;
    private int indent = 1;

    @Override
    public boolean add(Expression e) {
        if (e instanceof ExpressionList) {
            ((ExpressionList) e).parent = this;
            if (size() != 0 && get(0) instanceof Atom)
                ((ExpressionList) e).indent = 2;
        }
        return super.add(e);
    }

    public String toString() {
        String indent = "";
        if (parent != null && parent.get(0) != this) {
            indent = "\n";
            char[] chars = new char[(parent != null ? parent.indent + this.indent : 0)];
            Arrays.fill(chars, ' ');
            indent += new String(chars);
        }

        String output = indent + "(";
        for (Iterator<Expression> it = this.iterator(); it.hasNext(); ) {
            Expression expression = it.next();
            output += expression.toString();
            if (it.hasNext())
                output += " ";
        }
        output += ")";
        return output;
    }
}
