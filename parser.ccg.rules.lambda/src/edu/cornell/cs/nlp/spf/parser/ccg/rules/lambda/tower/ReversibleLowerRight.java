package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Monad;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lower.LowerLeft;

import java.util.HashSet;
import java.util.Set;

public class ReversibleLowerRight extends LowerLeft<LogicalExpression> implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {

    private final Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules;

    public ReversibleLowerRight(String label, ITowerCategoryServices<LogicalExpression> towerCategoryServices,
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

        Set<Category<LogicalExpression>> rights = new HashSet<>();
        for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
            rights.addAll(rule.reverseApplyLeft(left, result, span));
        }

        for (Category<LogicalExpression> right : rights) {
            if (!(right.getSemantics() instanceof Monad)) {
                continue;
            }
            ret.addAll(towerCategoryServices.unlower(right));
        }
        return ret;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(right instanceof TowerCategory)) {
            return ret;
        }
        if (result instanceof TowerCategory) {
            return ret;
        }

        TowerCategory<LogicalExpression> rightTower = (TowerCategory<LogicalExpression>) right;
        Tower rightSem = (Tower) rightTower.getSemantics();
        LogicalExpression rightTopBody = rightSem.getTop().getBody();
        if (!(rightTopBody.getType() instanceof MonadType)) {
            return ret;
        }

        Category<LogicalExpression> loweredRight = towerCategoryServices.lower(rightTower);
        for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
            ret.addAll(rule.reverseApplyRight(loweredRight, result, span));
        }
        return ret;
    }


    public static class Creator implements IResourceObjectCreator<ReversibleLowerRight> {

        private String	type;

        public Creator() {
            this("rule.recursive.lift.left.reversible");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ReversibleLowerRight create(ParameterizedExperiment.Parameters params,
                                           IResourceRepository repo) {
            BinaryRuleSet baseRules = repo.get(params.get("baseRules"));
            Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules = new HashSet<>();
                    repo.get(params.get("reversibleBaseRules"));

            for (String id : params.getSplit("reversibleBaseRules")) {
                IBinaryReversibleParseRule<LogicalExpression> rule = repo.get(id);
                reversibleBaseRules.add(rule);
            }

            return new ReversibleLowerRight(RULE_LABEL,
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
