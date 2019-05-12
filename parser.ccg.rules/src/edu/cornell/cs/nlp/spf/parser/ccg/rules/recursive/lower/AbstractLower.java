package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.AbstractRecursiveBinaryParseRule;

import java.util.List;

public abstract class AbstractLower<MR> extends AbstractRecursiveBinaryParseRule<MR> {
    public static final String				RULE_LABEL	= "lower";

    public AbstractLower(String label,
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
