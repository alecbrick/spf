package edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.utils.composites.Pair;

public class MonadRule<MR, M> implements IBinaryParseRule<MR> {
    private final IMonadServices<MR, M> monadServices;
    private final IBinaryParseRule<MR> rule;
    private final UnaryRuleName name = UnaryRuleName.create("m");

    public MonadRule(IBinaryParseRule rule, IMonadServices<MR, M> monadServices) {
        this.rule = rule;
        this.monadServices = monadServices;
    }

    @Override
    public ParseRuleResult<MR> apply(Category<MR> left, Category<MR> right, SentenceSpan span) {
        Pair<Category<MR>, Category<MR>> monadCategories =
                monadServices.liftCategoriesToMonad(left, right);
        ParseRuleResult<MR> res = rule.apply(
                monadCategories.first(), monadCategories.second(), span);
        return new ParseRuleResult<>(MonadRuleName.create(
                "m", rule.getName()), res.getResultCategory());
    }

    @Override
    public RuleName getName() {
        return name;
    }
}
