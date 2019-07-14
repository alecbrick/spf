package edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Monad;
import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.Set;

public interface IMonadServices<MR, M> {
    M logicalExpressionToMonad(MR exp);

    Set<MR> logicalExpressionFromMonad(M monad);

    Pair<Category<MR>, Category<MR>> liftCategoriesToMonad(
            Category<MR> left, Category<MR> right);

    Set<M> unexecMonad(M input);
}
