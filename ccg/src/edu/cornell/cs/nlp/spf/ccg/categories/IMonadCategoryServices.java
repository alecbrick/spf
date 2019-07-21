package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Monad;
import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.Set;

public interface IMonadCategoryServices<MR, M> {
    Pair<Category<MR>, Category<MR>> liftCategoriesToMonad(
            Category<MR> left, Category<MR> right);
}
