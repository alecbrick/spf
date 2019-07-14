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
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.TowerType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSet;

import java.util.Set;

/**
 * Lambda expression with a single argument and a logical expression as the
 * body.
 *
 * @author Yoav Artzi
 */
public class Tower extends LogicalExpression
		implements ITowerSemantics<LogicalExpression> {
	/**
	 * The head string for a lambda expression.
	 */
	public static final String		HEAD_STRING			= "lambda";

	public static final ILogger		LOG					= LoggerFactory
																.create(Tower.class);

	public static final char		OPEN_PAREN		 	= '[';
	public static final char		CLOSE_PAREN		 	= ']';
	private static final long		serialVersionUID	= -9074603389979811699L;

	private final Lambda 			top;

	private final LogicalExpression	bottom;

	private final Set<Variable>		freeVariables;

	private final TowerType type;

	public Tower(Lambda top, LogicalExpression bottom) {
		this(top, bottom, LogicLanguageServices.getTypeRepository());
	}

	@SuppressWarnings("unchecked")
	private Tower(Lambda top, LogicalExpression bottom,
				  TypeRepository typeRepository) {
		assert top != null;
		assert bottom != null;
		this.top = top;
		this.bottom = bottom;
		this.type = typeRepository.getTowerTypeCreateIfNeeded(top.getType(),
				bottom.getType());
		assert type != null : String.format(
				"Invalid lambda type: top=%s, bottom=%s, inferred type=%s",
				top, bottom, type);
		ReferenceSet<Variable> topFree = new ReferenceOpenHashSet<>(top.getFreeVariables());
		ReferenceSet<Variable> bottomFree = new ReferenceOpenHashSet<>(bottom.getFreeVariables());
		topFree.retainAll(bottomFree);
		this.freeVariables = topFree;
	}

	// Converts a lambda into an equivalent Tower expression.
	public Tower(Lambda lambda) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (top == null ? 0 : top.hashCode());
		result = prime * result + (bottom == null ? 0 : bottom.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean containsFreeVariable(Variable variable) {
		return freeVariables.contains(variable);
	}

	@Override
	public boolean containsFreeVariables(Set<Variable> variables) {
		if (freeVariables.isEmpty()) {
			return false;
		}

		final Set<Variable> bigSet;
		final Set<Variable> smallSet;
		if (freeVariables.size() >= variables.size()) {
			bigSet = freeVariables;
			smallSet = variables;
		} else {
			bigSet = variables;
			smallSet = freeVariables;
		}

		for (final Variable variable : smallSet) {
			if (bigSet.contains(variable)) {
				return true;
			}
		}
		return false;
	}

	public Lambda getTop() {
		return top;
	}

	public LogicalExpression getBottom() {
		return bottom;
	}

	public TowerType getTowerType() {
		return type;
	}

	@Override
	public Set<Variable> getFreeVariables() {
		return freeVariables;
	}

	@Override
	public Type getType() {
		return type;
	}

	@Override
	public int numFreeVariables() {
		return freeVariables.size();
	}

	@Override
	protected boolean doEquals(LogicalExpression exp,
			ScopeMapping<Variable, Variable> mapping) {
		if (this == exp) {
			// Since skolem IDs from this literal may be used in other parts of
			// the logical form, we need to create a mapping of them. As the
			// instances are identical, we can just update the mapping by
			// creating a mapping from each SkolemId to itself.
			if (!freeVariables.isEmpty()) {
				for (final Variable freeVariable : freeVariables) {
					if (freeVariable instanceof SkolemId) {
						mapping.push(freeVariable, freeVariable);
					}
				}
			}
			return true;
		}
		if (getClass() != exp.getClass()) {
			return false;
		}
		final Tower other = (Tower) exp;
		if (!type.equals(other.type)) {
			return false;
		}

		boolean ret = top.equals(other.top, mapping);
		ret = ret && bottom.equals(other.bottom, mapping);

		return ret;
	}





	public static class Reader implements IReader<Tower> {

		@Override
		public Tower read(String string,
						  ScopeMapping<String, LogicalExpression> mapping,
						  TypeRepository typeRepository, ITypeComparator typeComparator,
						  LogicalExpressionReader reader) {

			try {
                final String innerString = string.substring(1, string.length() - 1)
                        .trim();
                int i = 0;
                final StringBuilder topStringBuilder = new StringBuilder();
                char c;
                int parenthesisCounter = 0;
                while (i < innerString.length()
                        && !((c = innerString.charAt(i)) == CLOSE_PAREN && parenthesisCounter == 0)) {
                    ++i;
                    topStringBuilder.append(c);
                    if (c == OPEN_PAREN) {
                        ++parenthesisCounter;
                    } else if (c == CLOSE_PAREN) {
                        --parenthesisCounter;
                    }
                }
                i += 2;
				final String topString = topStringBuilder.toString().trim();
                final String bottomString = innerString.substring(i).trim();

                final LogicalExpression topLogical = reader.read(
                		topString, mapping, typeRepository, typeComparator);
                if (!(topLogical instanceof Lambda)) {
                	throw new LogicalExpressionRuntimeException(
                			"Invalid continuation: " + topString);
				}
				final Lambda top = (Lambda) topLogical;
                final LogicalExpression bottom = reader.read(
                		bottomString, mapping, typeRepository, typeComparator);

				return new Tower(top, bottom);
			} catch (final RuntimeException e) {
				LOG.error("Continuation tower syntax error: %s", string);
				throw e;
			}

		}

		@Override
		public boolean test(String string) {
			return string.startsWith(OPEN_PAREN + Lambda.PREFIX);
		}

	}

}
