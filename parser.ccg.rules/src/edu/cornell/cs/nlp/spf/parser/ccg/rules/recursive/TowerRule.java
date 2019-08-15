package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.DelimitSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit.AbstractDelimit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An entry point for continuized CCG rules.
 * Because the recursive rules have an order to how they get applied,
 * this class enforces that order.
 */
public class TowerRule<MR> implements IBinaryRecursiveParseRule<MR> {
    private static final String         RULE_LABEL = "vRS";

    protected final ITowerCategoryServices<MR> towerCategoryServices;
    private final List<IBinaryRecursiveParseRule<MR>> recursiveRules;

    // TODO: maybe make this parameterizable?
    private final UnaryRuleName towerRuleName = UnaryRuleName.create("tower");

    public TowerRule(ITowerCategoryServices<MR> towerCategoryServices,
                     List<IBinaryRecursiveParseRule<MR>> recursiveRules) {
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
            List<IBinaryRecursiveParseRule<MR>> validRules) {
        if (!(left instanceof TowerCategory || right instanceof TowerCategory)) {
            return Collections.emptyList();
        }

        // We only want to allow AbstractDelimit as a top-level rule. (I think.)
        List<IBinaryRecursiveParseRule<MR>> rulesToUse = new ArrayList<>();
        for (IBinaryRecursiveParseRule<MR> rule : recursiveRules) {
            if (!(rule instanceof AbstractDelimit)) {
                rulesToUse.add(rule);
            }
        }


        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        for (IBinaryRecursiveParseRule<MR> rule : recursiveRules) {
            for (ParseRuleResult<MR> result : rule.applyRecursive(left, right, span, rulesToUse)) {
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
            List<IBinaryRecursiveParseRule<MR>> recursiveRules = new ArrayList<>();
            for (String id : params.getSplit("rules")) {
                IBinaryRecursiveParseRule<MR> rule = repo.get(id);
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
