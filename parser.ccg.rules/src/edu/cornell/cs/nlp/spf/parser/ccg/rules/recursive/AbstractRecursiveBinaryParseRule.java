package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRecursiveBinaryParseRule<MR> implements IRecursiveBinaryParseRule<MR> {

    protected RuleName name;
    protected ITowerCategoryServices<MR> towerCategoryServices;
    protected BinaryRuleSet<MR> baseRules;

    public AbstractRecursiveBinaryParseRule(String label,
                                            ITowerCategoryServices<MR> towerCategoryServices,
                                            BinaryRuleSet<MR> baseRules) {
        this.name = UnaryRuleName.create(label);
        this.towerCategoryServices = towerCategoryServices;
        this.baseRules = baseRules;
    }

    protected RuleName createRuleName(ParseRuleResult<MR> recursiveRule) {
        StringBuilder builder = new StringBuilder();
        builder.append(getName().toString());
        builder.append(",");
        builder.append(recursiveRule.getRuleName().toString());
        return UnaryRuleName.create(builder.toString());
    }

    protected List<ParseRuleResult<MR>> combineRecursive(Category<MR> left,
                                                Category<MR> right,
                                                SentenceSpan span,
                                                List<IRecursiveBinaryParseRule<MR>> validRules) {
        List<ParseRuleResult<MR>> ret = new ArrayList();
        if (!(left instanceof TowerCategory) && !(right instanceof TowerCategory)) {
            for (IBinaryParseRule<MR> rule : baseRules) {
                ParseRuleResult<MR> result = rule.apply(left, right, span);
                if (result != null) {
                    ret.add(result);
                }
            }
        } else {
            for (IRecursiveBinaryParseRule<MR> rule : validRules) {
                List<ParseRuleResult<MR>> results =
                        rule.applyRecursive(left, right, span, validRules);
                ret.addAll(results);
            }
        }

        return ret;
    }

    @Override
    public RuleName getName() {
        return name;
    }
}
