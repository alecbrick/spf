package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Monad;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lower.LowerLeft;

import java.util.HashSet;
import java.util.Set;

public class ReversibleLowerLeft extends LowerLeft<LogicalExpression> implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {

    private final Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules;

    public ReversibleLowerLeft(String label, ITowerCategoryServices<LogicalExpression> towerCategoryServices,
                               BinaryRuleSet<LogicalExpression> baseRules, Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules) {
        super(label, towerCategoryServices, baseRules);
        this.reversibleBaseRules = reversibleBaseRules;
    }

    @Override
    public void addRecursiveRule(IBinaryReversibleRecursiveParseRule<LogicalExpression> rule) {
        // nothing to do
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(left instanceof TowerCategory)) {
            return ret;
        }
        if (result instanceof TowerCategory) {
            return ret;
        }

        TowerCategory<LogicalExpression> leftTower = (TowerCategory<LogicalExpression>) left;
        Tower leftSem = (Tower) leftTower.getSemantics();
        LogicalExpression leftTopBody = leftSem.getTop().getBody();
        if (!(leftTopBody.getType() instanceof MonadType)) {
            return ret;
        }

        Category<LogicalExpression> loweredLeft = towerCategoryServices.lower(leftTower);
        for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
            ret.addAll(rule.reverseApplyLeft(loweredLeft, result, span));
        }
        return ret;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();

        Set<Category<LogicalExpression>> lefts = new HashSet<>();
        for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
            lefts.addAll(rule.reverseApplyRight(right, result, span));
        }

        for (Category<LogicalExpression> left : lefts) {
            if (!(left.getSemantics() instanceof Monad)) {
                continue;
            }
            ret.addAll(towerCategoryServices.unlower(left));
        }
        return ret;
    }

    public static class Creator implements IResourceObjectCreator<ReversibleLowerLeft> {

        private String	type;

        public Creator() {
            this("rule.recursive.lift.left.reversible");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ReversibleLowerLeft create(ParameterizedExperiment.Parameters params,
                                         IResourceRepository repo) {
            BinaryRuleSet baseRules = repo.get(params.get("baseRules"));
            Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules = new HashSet<>();
                    repo.get(params.get("reversibleBaseRules"));

            for (String id : params.getSplit("reversibleBaseRules")) {
                IBinaryReversibleParseRule<LogicalExpression> rule = repo.get(id);
                reversibleBaseRules.add(rule);
            }

            return new ReversibleLowerLeft(RULE_LABEL,
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    baseRules,
                    reversibleBaseRules);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage.builder(type, ReversibleCombination.class)
                    .addParam("baseRules", BinaryRuleSet.class,
                            "Binary rules to apply at the base level.")
                    .addParam("reversibleBaseRules", String.class,
                            "Reversible binary rules for base-level lexicon generation.")
                    .build();
        }
    }
}
