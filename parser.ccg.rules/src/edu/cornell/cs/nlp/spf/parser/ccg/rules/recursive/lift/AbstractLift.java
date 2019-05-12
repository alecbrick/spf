package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.AbstractRecursiveBinaryParseRule;

public abstract class AbstractLift<MR> extends AbstractRecursiveBinaryParseRule<MR> {
    public static final String				RULE_LABEL	= "lift";

    public AbstractLift(String label,
                        ITowerCategoryServices<MR> towerCategoryServices,
                        BinaryRuleSet<MR> baseRules) {
        super(label, towerCategoryServices, baseRules);
    }

    @Override
    public ParseRuleResult<MR> apply(Category<MR> left,
                                     Category<MR> right, SentenceSpan span) {
        return null;
    }
}
