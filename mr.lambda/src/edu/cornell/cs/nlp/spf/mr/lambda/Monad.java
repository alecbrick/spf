/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.mr.lambda;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lambda expression with a single argument and a logical expression as the
 * body.
 *
 * @author Yoav Artzi
 */
public abstract class Monad extends LogicalExpression {
	/**
	 * The head string for a monad expression.
	 */
	public static final ILogger		LOG					= LoggerFactory
																.create(Monad.class);

	private static final long		serialVersionUID	= -9074603389979811699L;

	public abstract MonadParams exec(MonadParams arg, Map<Variable, LogicalExpression> bindings);

	public MonadParams exec(MonadParams arg) {
		return exec(arg, new HashMap<>());
	}

	public abstract LogicalExpression getBody();

	public static class Reader implements IReader<Monad> {
	    public List<IReader<? extends Monad>> monadReaders = new ArrayList<>();

	    public Reader() {
            monadReaders.add(new StateMonad.Reader());
            monadReaders.add(new Binding.Reader());
		}

		@Override
		public Monad read(String string, ScopeMapping<String, LogicalExpression> mapping, TypeRepository typeRepository, ITypeComparator typeComparator, LogicalExpressionReader reader) {
			for (IReader r : monadReaders) {
				if (r.test(string)) {
					return (Monad) r.read(string, mapping, typeRepository, typeComparator, reader);
				}
			}
			throw new IllegalArgumentException(
					"Invalid monad syntax: " + string);
		}

		@Override
		public boolean test(String s) {
			for (IReader r : monadReaders) {
				if (r.test(s)) {
					return true;
				}
			}
			return false;
		}

	}
}
