package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.combination.Combination;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An entry point for continuized CCG rules.
 * Because the recursive rules have an order to how they get applied,
 * this class enforces that order.
 */
public class TowerRule<MR> implements IRecursiveBinaryParseRule<MR> {
    private static final String         RULE_LABEL = "vRS";

    protected final ITowerCategoryServices<MR> towerCategoryServices;
    private final List<IRecursiveBinaryParseRule<MR>> recursiveRules;

    // TODO: maybe make this parameterizable?
    private final UnaryRuleName towerRuleName = UnaryRuleName.create("tower");

    public TowerRule(ITowerCategoryServices<MR> towerCategoryServices,
                     List<IRecursiveBinaryParseRule<MR>> recursiveRules) {
        this.towerCategoryServices = towerCategoryServices;
        this.recursiveRules = recursiveRules;
    }

    @Override
    public ParseRuleResult<MR> apply(Category<MR> left, Category<MR> right, SentenceSpan span) {
        return null;
    }

    @Override
    public RuleName getName() {
        return towerRuleName;
    }

    @Override
    public List<ParseRuleResult<MR>> applyRecursive(
            Category<MR> left, Category<MR> right, SentenceSpan span,
            List<IRecursiveBinaryParseRule<MR>> validRules) {
        if (!(left instanceof TowerCategory || right instanceof TowerCategory)) {
            return Collections.emptyList();
        }

        // TODO: delimiting, base pre-computation

        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        for (IRecursiveBinaryParseRule<MR> rule : recursiveRules) {
            for (ParseRuleResult<MR> result : rule.applyRecursive(left, right, span, recursiveRules)) {
                ret.add(result);
                Category<MR> resultCategory = result.getResultCategory();
                if (!(resultCategory instanceof TowerCategory)) {
                    continue;
                }
                Category<MR> loweredCategory =
                        towerCategoryServices.lower(
                                (TowerCategory<MR>) resultCategory);
                if (loweredCategory != null) {
                    ret.add(new ParseRuleResult<>(
                            RecursiveRuleName.create(
                                    RULE_LABEL, result.getRuleName()),
                            loweredCategory));
                }
            }
        }

        return ret;
    }

    public static class Creator<MR> implements
            IResourceObjectCreator<TowerRule<MR>> {

        private String type;

        public Creator() {
            this("rule.tower");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public TowerRule<MR> create(ParameterizedExperiment.Parameters params,
                                    IResourceRepository repo) {
            List<IRecursiveBinaryParseRule<MR>> recursiveRules = new ArrayList<>();
            for (String id : params.getSplit("rules")) {
                IRecursiveBinaryParseRule<MR> rule = repo.get(id);
                recursiveRules.add(rule);
            }

            return new TowerRule<MR>(
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    recursiveRules);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage.builder(type, TowerRule.class)
                    .build();
        }
    }
}
