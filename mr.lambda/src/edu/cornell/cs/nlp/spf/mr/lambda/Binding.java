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

import edu.cornell.cs.nlp.spf.base.LispReader;
import edu.cornell.cs.nlp.spf.mr.IMeaningRepresentation;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.LambdaWrapped;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceSets;

import java.io.StringReader;
import java.util.Map;
import java.util.Set;

/**
 * Lambda calculus literal.
 *
 * @author Yoav Artzi
 */
public class Binding<T extends Monad> extends Monad {
	public static final String			HEAD_STRING			= ">>=";
	public static final ILogger			LOG					= LoggerFactory
																	.create(Binding.class);
	public static final String			PREFIX				= LogicalExpression.PARENTHESIS_OPEN + HEAD_STRING;

	private static final long			serialVersionUID	= -4209330309716600396L;

	/**
	 * The arguments are stored in an array which is never exposed publicly.
	 * This allows the class to guarantee its immutability, while providing high
	 * performance. This member doesn't provide an iterator, due to the cost of
	 * creating an iterator and superior performance of direct access (using
	 * {@link #getArg(int)}).
	 */
	private LogicalExpression left;
	private LogicalExpression right;
	private Variable variable;

	protected Set<Variable> freeVariables;

	protected final Type type;

	public Binding(LogicalExpression left, LogicalExpression right, Variable variable) {
		assert((left instanceof Monad) || (left instanceof Variable));
		assert((right instanceof Monad) || (right instanceof Variable));
		this.left = left;
		this.right = right;
		this.variable = variable;
		// The type of a binding is the type of its right monad.
		this.type = right.getType();

		// TODO: The free variables will change when we fill in variables!
        Set<Variable> free = new ReferenceOpenHashSet<>();
		if (left instanceof Monad) {
			free.addAll(left.getFreeVariables());
		}
		if (right instanceof Monad) {
			free.addAll(right.getFreeVariables());
		}
		freeVariables = free;
	}

	@Override
	public MonadParams exec(MonadParams arg, Map<Variable, LogicalExpression> bindings) {
		assert(!(left instanceof Variable) && !(right instanceof Variable));
		T leftMonad = (T) left;
		T rightMonad = (T) right;
		MonadParams leftOutput = leftMonad.exec(arg, bindings);
		LogicalExpression logicalOut = leftOutput.getOutput();
		bindings.put(this.variable, logicalOut);
		MonadParams rightOutput = rightMonad.exec(leftOutput, bindings);
		return rightOutput;
	}

	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
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
	protected int calcHashCode() {
        final int prime = 31;
		int result = 1;
		result = prime * result + (left == null ? 0 : left.hashCode());
		result = prime * result + (right == null ? 0 : right.hashCode());
		result = prime * result + (variable == null ? 0 : variable.hashCode());
		return result;
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
		final Binding other = (Binding) exp;
		if (!type.equals(other.getType())) {
			return false;
		}

		if (!left.equals(other.left, mapping)) {
			return false;
		}
		if (variable.getType().equals(other.variable.getType())) {
			// I'm not sure if this is necessary - I'm just copying Lambda.
			mapping.push(variable, other.variable);
		} else {
			return false;
		}

		final boolean ret = right.equals(other.right, mapping);
		mapping.pop(variable);

		return ret;
	}

	public LogicalExpression getLeft() {
		return left;
	}

	public LogicalExpression getRight() {
		return right;
	}

	public Variable getVariable() {
		return variable;
	}

	public static class Reader implements IReader<Binding> {

		@Override
		// (>>= v M1 M2)
		public Binding read(String string,
                            ScopeMapping<String, LogicalExpression> mapping,
                            TypeRepository typeRepository, ITypeComparator typeComparator,
                            LogicalExpressionReader reader) {
			try {
				final LispReader lispReader = new LispReader(new StringReader(
						string));
				// read (>>=
				lispReader.next();

                // The second argument is the variable that gets bound.
				final Pair<String, Variable> variableDef = Variable
						.readVariableDefintion(lispReader.next(),
								typeRepository);

				final LogicalExpression m1 = reader.read(
						lispReader.next(), mapping, typeRepository,
						typeComparator);
				assert((m1 instanceof Variable) || (m1 instanceof Monad));

				// Add the bound variable to the mapping.
				// We won't remove this variable from the mapping, because it can
				// be referenced outside of its scope.
				// This is important for tower bottoms, which will
				// include references to bound monad variables.
				mapping.push(variableDef.first(), variableDef.second());
				final LogicalExpression m2 = reader.read(
						lispReader.next(), mapping, typeRepository,
						typeComparator);
				assert((m2 instanceof Variable) || (m2 instanceof Monad));

				// Verify that we don't have any more elements.
				if (lispReader.hasNext()) {
					throw new LogicalExpressionRuntimeException(String.format(
							"Invalid binding: %s", string));
				}

				return new Binding(m1, m2, variableDef.second());
			} catch (final RuntimeException e) {
				LOG.error("Binding syntax error: %s", string);
				throw e;
			}
		}

		@Override
		public boolean test(String string) {
			return string.startsWith(Binding.PREFIX);
		}
	}
}
