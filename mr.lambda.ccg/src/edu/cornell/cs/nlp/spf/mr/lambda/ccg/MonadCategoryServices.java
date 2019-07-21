package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.IMonadCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class MonadCategoryServices implements IMonadCategoryServices<LogicalExpression, Monad> {
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
                    rightSem = new Lambda(rightSem.getArgument(), MonadServices.logicalExpressionToMonad(rightSem.getBody()));
                }

            } else {
                if (rightSem.getComplexType().getDomain() instanceof MonadType &&
                        !(leftSem.getComplexType().getRange() instanceof MonadType)) {
                    leftSem = new Lambda(leftSem.getArgument(), MonadServices.logicalExpressionToMonad(leftSem.getBody()));
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

}
