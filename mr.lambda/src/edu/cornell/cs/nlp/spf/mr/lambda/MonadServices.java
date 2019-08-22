package edu.cornell.cs.nlp.spf.mr.lambda;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.AllSubExpressions;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetAllFreeVariables;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetSkolemIds;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MonadServices {
    protected static MonadServices INSTANCE;

    protected Syntax getMonadSyntaxImpl() {
        return Syntax.S;
    }

    public static Syntax getMonadSyntax() {
        return INSTANCE.getMonadSyntaxImpl();
    }

    protected Monad logicalExpressionToMonadImpl(LogicalExpression exp) {
        return new StateMonad(exp);
    }

    public static Monad logicalExpressionToMonad(LogicalExpression exp) {
        return INSTANCE.logicalExpressionToMonadImpl(exp);
    }

    protected Set<LogicalExpression> logicalExpressionFromMonadImpl(Monad m) {
        Set<LogicalExpression> ret = new HashSet<>();
        ret.add(m.getBody());
        return ret;
    }

    public static Set<LogicalExpression> logicalExpressionFromMonad(Monad m) {
        return INSTANCE.logicalExpressionFromMonadImpl(m);
    }

    // A Binding can be executed, producing a monad.
    // For lexicon generation, this process must be reversible.
    // TODO: unexec more than once
    protected Set<Monad> unexecMonadImpl(Monad input) {
        List<LogicalExpression> exps = AllSubExpressions.of(input);
        Type trueType = LogicLanguageServices.getTypeRepository().getTruthValueType();
        Set<Monad> ret = new HashSet<>();
        Set<Variable> inputBoundVariables = new HashSet<>();
        if (input instanceof Binding) {
            inputBoundVariables.addAll(((Binding) input).getBoundVariables());
        }
        for (LogicalExpression exp : exps) {
            if (!exp.getType().equals(trueType)) {
                continue;
            }
            // Don't extract expressions containing bound variables
            if (inputBoundVariables.size() > 0) {
                Set<Variable> freeVars = GetAllFreeVariables.of(exp);
                freeVars.retainAll(inputBoundVariables);
                if (freeVars.size() > 0) {
                    continue;
                }
            }
            Variable variable = new Variable(trueType);
            Monad left = logicalExpressionToMonad(exp);
            // LogicalExpression, since this could replace the entire expression.
            LogicalExpression right = ReplaceExpression.of(input, exp, variable);
            Binding binding = new Binding(left, right, variable);
            // Honestly maybe only need to run updateStates at the end
            ret.add((Binding) updateStates(binding));
        }
        return ret;
    }

    public static Set<Monad> unexecMonad(Monad input) {
        return INSTANCE.unexecMonadImpl(input);
    }

    // By default, we only want states to contain the entities in their
    // logical expressions.
    // Unexecing might disrupt this, so we need to fix the states.
    protected LogicalExpression updateStates(LogicalExpression exp) {
        if (exp instanceof Binding) {
            Binding bindingExp = (Binding) exp;
            LogicalExpression newLeft = updateStates(bindingExp.getLeft());
            LogicalExpression newRight = updateStates(bindingExp.getRight());
            if (!newLeft.equals(bindingExp.getLeft()) ||
                    !newRight.equals(bindingExp.getRight())) {
                return new Binding(newLeft, newRight, bindingExp.getVariable());
            } else {
                return bindingExp;
            }
        } else if (exp instanceof StateMonad) {
            StateMonad monadExp = (StateMonad) exp;
            Set<SkolemId> state = new HashSet<>(monadExp.getState().getState());
            Set<SkolemId> skolemIds = GetSkolemIds.of(monadExp);
            state.retainAll(skolemIds);
            return new StateMonad(monadExp.getBody(), new State<>(state));
        } else {
            return exp;
        }
    }


    protected LogicalExpression bindStateImpl(LogicalExpression exp, Set<SkolemId> state) {
        return exp;
    }

    public static LogicalExpression bindState(LogicalExpression exp, Set<SkolemId> state) {
        return INSTANCE.bindStateImpl(exp, state);
    }


    public static void setInstance(MonadServices monadServices) {
        MonadServices.INSTANCE = monadServices;
    }

    public static class Builder {
		public MonadServices build() {
			return new MonadServices();
		}
	}
}
