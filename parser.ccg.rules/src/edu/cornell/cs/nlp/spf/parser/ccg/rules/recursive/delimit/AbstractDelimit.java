package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.AbstractBinaryRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.combination.Combination;

public abstract class AbstractDelimit<MR> extends AbstractBinaryRecursiveParseRule<MR> {
    public static final String				RULE_LABEL	= "Delim";
    protected Combination<MR>               combination;

    public AbstractDelimit(String label,
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
