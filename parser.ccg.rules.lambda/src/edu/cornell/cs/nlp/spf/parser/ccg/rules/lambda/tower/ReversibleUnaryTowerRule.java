package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;

import java.util.Set;

public class ReversibleUnaryTowerRule implements IUnaryReversibleParseRule<LogicalExpression> {
    ITowerCategoryServices<LogicalExpression> towerCategoryServices;
    IUnaryReversibleParseRule<LogicalExpression> rule;

    public ReversibleUnaryTowerRule(ITowerCategoryServices<LogicalExpression> towerCategoryServices,
                                    IUnaryReversibleParseRule<LogicalExpression> rule) {
        this.towerCategoryServices = towerCategoryServices;
        this.rule = rule;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApply(Category<LogicalExpression> result, SentenceSpan span) {
        if (!(result instanceof TowerCategory)) {
            return null;
        }
        TowerCategory<LogicalExpression> towerCategory = (TowerCategory<LogicalExpression>) result;
        Category<LogicalExpression> base = towerCategoryServices.getBottom(towerCategory);
        return rule.reverseApply(base, span);
    }

    @Override
    public ParseRuleResult<LogicalExpression> apply(Category<LogicalExpression> category, SentenceSpan span) {
        if (!(category instanceof TowerCategory)) {
            return null;
        }
        TowerCategory<LogicalExpression> towerCategory = (TowerCategory<LogicalExpression>) category;
        Category<LogicalExpression> base = towerCategoryServices.getBottom(towerCategory);
        return rule.apply(base, span);
    }

    @Override
    public UnaryRuleName getName() {
        return null;
    }

    @Override
    public boolean isValidArgument(Category<LogicalExpression> category, SentenceSpan span) {
        return (category instanceof TowerCategory);
    }

    public class ReversibleUnaryTowerRuleCreator implements
            IResourceObjectCreator<ReversibleUnaryTowerRule> {

        private final String	type;

        public ReversibleUnaryTowerRuleCreator() {
            this("rule.tower.unary.reversible");
        }

        public ReversibleUnaryTowerRuleCreator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ReversibleUnaryTowerRule create(ParameterizedExperiment.Parameters params,
                                          IResourceRepository repo) {
            IUnaryReversibleParseRule<LogicalExpression> rule = repo.get(params.get("rule"));

			ITowerCategoryServices<LogicalExpression> towerCategoryServices =
					repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE);

            return new ReversibleUnaryTowerRule(
            		towerCategoryServices,
                    rule);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage
                    .builder(type, ReversibleUnaryTowerRule.class)
                    .addParam("rule",
                            IUnaryReversibleParseRule.class,
                            "The unary rule to apply to the base of the tower.")
                    .build();
        }
    }
}
