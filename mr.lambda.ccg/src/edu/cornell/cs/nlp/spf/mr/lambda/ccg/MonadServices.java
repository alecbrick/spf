package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.AllSubExpressions;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetAllFreeVariables;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic.IMonadServices;
import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MonadServices implements IMonadServices<LogicalExpression, Monad> {
    public Monad logicalExpressionToMonad(LogicalExpression exp) {
        return new StateMonad(exp);
    }

    public Set<LogicalExpression> logicalExpressionFromMonad(Monad m) {
        Set<LogicalExpression> ret = new HashSet<>();
        ret.add(m.getBody());
        return ret;
    }

    // TODO: What if it's not a state monad?
    @Override
    public Pair<Category<LogicalExpression>, Category<LogicalExpression>> liftCategoriesToMonad(
            Category<LogicalExpression> left, Category<LogicalExpression> right) {
        if (left instanceof ComplexCategory && right instanceof ComplexCategory) {
            // Composition
            Lambda leftSem = (Lambda) left.getSemantics();
            Lambda rightSem = (Lambda) right.getSemantics();
            // Forward composition
            if (((ComplexCategory<LogicalExpression>) left).getSlash() == Slash.FORWARD) {
                if (leftSem.getComplexType().getDomain() instanceof MonadType &&
                        !(rightSem.getComplexType().getRange() instanceof MonadType)) {
                    rightSem = new Lambda(rightSem.getArgument(), logicalExpressionToMonad(rightSem.getBody()));
                }

            } else {
                if (rightSem.getComplexType().getDomain() instanceof MonadType &&
                        !(leftSem.getComplexType().getRange() instanceof MonadType)) {
                    leftSem = new Lambda(leftSem.getArgument(), logicalExpressionToMonad(leftSem.getBody()));
                }
            }
            return Pair.of(Category.create(left.getSyntax(), leftSem),
                    Category.create(right.getSyntax(), rightSem));
        } else {
            // Application
            LogicalExpression leftSem = left.getSemantics();
            LogicalExpression rightSem = right.getSemantics();
            if (left instanceof ComplexCategory) {
                Lambda leftSemLambda = (Lambda) leftSem;
                if (leftSemLambda.getComplexType().getDomain() instanceof MonadType &&
                        !(rightSem.getType() instanceof MonadType)) {
                    rightSem = new StateMonad(rightSem);
                }
            } else if (right instanceof ComplexCategory) {
                Lambda rightSemLambda = (Lambda) rightSem;
                if (rightSemLambda.getComplexType().getDomain() instanceof MonadType &&
                        !(leftSem.getType() instanceof MonadType)) {
                    leftSem = new StateMonad(leftSem);
                }
            }
            return Pair.of(Category.create(left.getSyntax(), leftSem),
                    Category.create(right.getSyntax(), rightSem));
        }
    }

    // A Binding can be executed, producing a monad.
    // For lexicon generation, this process must be reversible.
    // TODO: unexec more than once
    public Set<Monad> unexecMonad(Monad input) {
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

    // By default, we only want states to contain the entities in their
    // logical expressions.
    // Unexecing might disrupt this, so we need to fix the states.
    private LogicalExpression updateStates(LogicalExpression exp) {
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
            return logicalExpressionToMonad(monadExp.getBody());
        } else {
            return exp;
        }
    }
}
