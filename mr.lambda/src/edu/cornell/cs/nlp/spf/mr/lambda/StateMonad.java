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
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader.IReader;
import edu.cornell.cs.nlp.spf.mr.lambda.mapping.ScopeMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ApplyAndSimplify;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ILogicalExpressionVisitor;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Lambda expression with a single argument and a logical expression as the
 * body.
 *
 * @author Yoav Artzi
 */
public class StateMonad extends Monad {
	/**
	 * The head string for a lambda expression.
	 */
	public static final String		HEAD_STRING			= "stateM";

	public static final ILogger		LOG					= LoggerFactory
																.create(StateMonad.class);

	public static final String		PREFIX				= LogicalExpression.PARENTHESIS_OPEN
																+ HEAD_STRING;
	private static final long		serialVersionUID	= -9074603389979811699L;

	private final LogicalExpression	body;

	protected final MonadType type;

	protected State<SkolemId> state;

	protected final Set<Variable>		freeVariables;

	public StateMonad(LogicalExpression body) {
		this(body, new State<>(), LogicLanguageServices.getTypeRepository());
	}

	public StateMonad(LogicalExpression body, State<SkolemId> state) {
		this(body, state, LogicLanguageServices.getTypeRepository());
	}

	private StateMonad(LogicalExpression body, State<SkolemId> state,
					  TypeRepository typeRepository) {
		this.body = body;
		this.state = state;
        this.type = typeRepository.getMonadTypeCreateIfNeeded(body.getType());

		this.freeVariables = body.getFreeVariables();
	}

	@Override
	public MonadParams exec(MonadParams args, Map<Variable, LogicalExpression> bindings) {
	    StateMonadParams input = (StateMonadParams) args;
	    state.add(input.getState());
		Set<Variable> bindingVars = bindings.keySet();
		bindingVars.retainAll(body.getFreeVariables());
		LogicalExpression tempResult = body;
		for (Variable v : bindingVars) {
			Lambda func = new Lambda(v, tempResult);
			LogicalExpression arg = bindings.get(v);
			tempResult = ApplyAndSimplify.of(func, arg);
		}
		// Bind the current state to underspecified relations
		tempResult = MonadServices.bindState(tempResult, state.getState());

		return new StateMonadParams(this.state, tempResult);
	}

	@Override
	public MonadParams exec() {
	    return exec(new StateMonadParams(), new HashMap<>());
	}

	public boolean doEquals(LogicalExpression exp,
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

        final StateMonad other = (StateMonad) exp;
        if (type != other.type) {
        	return false;
		}
        if (!state.equals(other.state, mapping)) {
        	return false;
		}
        return body.equals(other.body, mapping);
	}

    @Override
	public int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (state == null ? 0 : state.hashCode());
		result = prime * result + (body == null ? 0 : body.hashCode());
		result = prime * result + (type == null ? 0 : type.hashCode());
		return result;
	}

	@Override
	public void accept(ILogicalExpressionVisitor visitor) {
		visitor.visit(this);
	}

	public boolean containsFreeVariable(Variable variable) {
		return freeVariables.contains(variable);
	}

	public boolean containsFreeVariables(Set<Variable> variables) {
		return freeVariables.containsAll(variables);
	}

	public Set<Variable> getFreeVariables() {
		return freeVariables;
	}

	@Override
	public Type getType() {
		return type;
	}

	public int numFreeVariables() {
		return freeVariables.size();
	}

	@Override
	public LogicalExpression getBody() {
		return body;
	}

	public State<SkolemId> getState() {
	    return state;
	}

	public static class Reader implements IReader<StateMonad> {

		@Override
		// "(stateM (lambda x...) (s1 s2 s3))"
		public StateMonad read(String string,
							   ScopeMapping<String, LogicalExpression> mapping,
							   TypeRepository typeRepository,
							   ITypeComparator typeComparator,
							   LogicalExpressionReader reader) {
            try {
				final LispReader lispReader = new LispReader(new StringReader(
						string));

				// The first word is the test prefix, which we ignore.
				lispReader.next();

				final LogicalExpression body = reader.read(lispReader.next(),
						mapping, typeRepository, typeComparator);

				State<SkolemId> monadState = new State<>();
				String stateList = lispReader.next();
				// remove parentheses
				stateList = stateList.substring(1, stateList.length() - 1);
				if (!stateList.equals("")) {
					for (String s : stateList.split(" ")) {
						SkolemId skolem = (SkolemId) reader.read(s, mapping,
								typeRepository, typeComparator);
						monadState.add(skolem);
					}
				}
				StateMonad ret = new StateMonad(body, monadState);
				return ret;
			} catch (final RuntimeException e) {
            	LOG.error("State monad syntax error: %s", string);
            	throw e;
			}
		}

		@Override
		public boolean test(String s) {
			return s.startsWith(StateMonad.PREFIX);
		}
	}
}
