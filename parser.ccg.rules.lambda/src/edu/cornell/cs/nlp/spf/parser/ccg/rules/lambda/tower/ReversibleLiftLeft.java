package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetBindingMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftLeft;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReversibleLiftLeft extends LiftLeft<LogicalExpression>
        implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {


    private final Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules;
    private final Set<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveParseRules;
    private final ForwardReversibleApplication forwardApp;

    public ReversibleLiftLeft(String label,
                              ITowerCategoryServices<LogicalExpression> towerCategoryServices,
                              BinaryRuleSet<LogicalExpression> baseRules,
                              Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules,
                              ForwardReversibleApplication forwardApp) {
        super(label, towerCategoryServices, baseRules);
        this.reversibleBaseRules = reversibleBaseRules;
        this.recursiveParseRules = new HashSet<>();
        this.forwardApp = forwardApp;
    }

    @Override
    public void addRecursiveRule(IBinaryReversibleRecursiveParseRule<LogicalExpression> rule) {
        this.recursiveParseRules.add(rule);
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(result instanceof TowerCategory)) {
            return ret;
        }
        Category<LogicalExpression> resultBase = towerCategoryServices.getBottom(result);
        Set<Category<LogicalExpression>> bases = new HashSet<>();
        if (!(left instanceof TowerCategory) && !(resultBase instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                bases.addAll(rule.reverseApplyLeft(left, resultBase, span));
            }
        }
        if (bases.isEmpty()) {
            return ret;
        }

        TowerCategory<LogicalExpression> resultTower = (TowerCategory) result;
        TowerSyntax resultSyntax = resultTower.getSyntax();
        Tower resultSemantics = (Tower) resultTower.getSemantics();
        for (Category<LogicalExpression> base : bases) {
            ret.add(new TowerCategory<>(
                    new TowerSyntax(
                        resultSyntax.getLeft(), resultSyntax.getRight(),
                            base.getSyntax()),
                    new Tower(resultSemantics.getTop(), base.getSemantics())
            ));
        }
        return ret;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(right instanceof TowerCategory) || !(result instanceof TowerCategory)) {
            return ret;
        }

        TowerCategory<LogicalExpression> rightTower = (TowerCategory<LogicalExpression>) right;
        TowerCategory<LogicalExpression> resultTower = (TowerCategory<LogicalExpression>) result;

        TowerSyntax rightSyntax = rightTower.getSyntax();
        TowerSyntax resultSyntax = resultTower.getSyntax();

        if (!rightSyntax.getLeft().equals(resultSyntax.getLeft()) ||
                !rightSyntax.getRight().equals(resultSyntax.getRight())) {
            return ret;
        }

        Tower rightSem = (Tower) rightTower.getSemantics();
        Tower resultSem = (Tower) resultTower.getSemantics();
        if (!rightSem.getTop().equals(resultSem.getTop())) {
            return ret;
        }

        Map<Variable, Variable> mapping = GetBindingMapping.of(resultSem.getTop(), rightSem.getTop());

        Category<LogicalExpression> rightBase = towerCategoryServices.getBottom(rightTower);
        Category<LogicalExpression> resultBase = towerCategoryServices.getBottom(resultTower);

        if (mapping.size() > 0) {
            LogicalExpression rightBaseSem = rightBase.getSemantics();
            for (Variable resultVar : mapping.keySet()) {
                Variable rightVar = mapping.get(resultVar);
                rightBaseSem = ReplaceExpression.of(rightBaseSem, rightVar, resultVar);
            }
            rightBase = Category.create(rightBase.getSyntax(), rightBaseSem);
        }

        if (!(rightBase instanceof TowerCategory) && !(resultBase instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                ret.addAll(rule.reverseApplyRight(rightBase, resultBase, span));
            }
        } else {
            for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : recursiveParseRules) {
                ret.addAll(rule.reverseApplyRight(rightBase, resultBase, span));
            }
        }
        return ret;
    }

    public static class Creator implements IResourceObjectCreator<ReversibleLiftLeft> {

        private String	type;

        public Creator() {
            this("rule.recursive.lift.left.reversible");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ReversibleLiftLeft create(ParameterizedExperiment.Parameters params,
                                         IResourceRepository repo) {
            BinaryRuleSet baseRules = repo.get(params.get("baseRules"));
            Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules = new HashSet<>();
                    repo.get(params.get("reversibleBaseRules"));

            for (String id : params.getSplit("reversibleBaseRules")) {
                IBinaryReversibleParseRule<LogicalExpression> rule = repo.get(id);
                reversibleBaseRules.add(rule);
            }
            ForwardReversibleApplication forwardApp =
                    repo.get(params.get("forwardApp"));
            return new ReversibleLiftLeft(RULE_LABEL,
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    baseRules,
                    reversibleBaseRules,
                    forwardApp);
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
                    .addParam("forwardApp", ForwardReversibleApplication.class,
                            "Reversible forward application.")
                    .build();
        }
    }
}
