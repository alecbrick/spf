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
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftRight;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ReversibleLiftRight extends LiftRight<LogicalExpression>
        implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {


    private final Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules;
    private final Set<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveParseRules;
    private final ForwardReversibleApplication forwardApp;

    public ReversibleLiftRight(String label,
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
        if (!(left instanceof TowerCategory) || !(result instanceof TowerCategory)) {
            return ret;
        }

        TowerCategory<LogicalExpression> leftTower = (TowerCategory<LogicalExpression>) left;
        TowerCategory<LogicalExpression> resultTower = (TowerCategory<LogicalExpression>) result;

        TowerSyntax leftSyntax = leftTower.getSyntax();
        TowerSyntax resultSyntax = resultTower.getSyntax();

        if (!leftSyntax.getLeft().equals(resultSyntax.getLeft()) ||
                !leftSyntax.getRight().equals(resultSyntax.getRight())) {
            return ret;
        }

        Tower leftSem = (Tower) leftTower.getSemantics();
        Tower resultSem = (Tower) resultTower.getSemantics();
        if (!leftSem.getTop().equals(resultSem.getTop())) {
            return ret;
        }

        Map<Variable, Variable> mapping = GetBindingMapping.of(resultSem.getTop(), leftSem.getTop());

        Category<LogicalExpression> leftBase = towerCategoryServices.getBase(leftTower);
        Category<LogicalExpression> resultBase = towerCategoryServices.getBase(resultTower);

        if (mapping.size() > 0) {
            LogicalExpression leftBaseSem = leftBase.getSemantics();
            for (Variable resultVar : mapping.keySet()) {
                Variable rightVar = mapping.get(resultVar);
                leftBaseSem = ReplaceExpression.of(leftBaseSem, rightVar, resultVar);
            }
            leftBase = Category.create(leftBase.getSyntax(), leftBaseSem);
        }

        if (!(leftBase instanceof TowerCategory) && !(resultBase instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                ret.addAll(rule.reverseApplyLeft(leftBase, resultBase, span));
            }
        } else {
            for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : recursiveParseRules) {
                ret.addAll(rule.reverseApplyLeft(leftBase, resultBase, span));
            }
        }
        return ret;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(result instanceof TowerCategory)) {
            return ret;
        }
        Category<LogicalExpression> resultBase = towerCategoryServices.getBase(result);
        Set<Category<LogicalExpression>> bases = new HashSet<>();
        if (!(right instanceof TowerCategory) && !(resultBase instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                bases.addAll(rule.reverseApplyRight(right, resultBase, span));
            }
        }
        if (bases.isEmpty()) {
            return ret;
        }

        TowerCategory<LogicalExpression> resultTower = (TowerCategory<LogicalExpression>) result;
        TowerSyntax resultSyntax = resultTower.getSyntax();
        Tower resultSemantics = (Tower) resultTower.getSemantics();
        for (Category<LogicalExpression> base : bases) {
            ret.add(new TowerCategory<>(
                    new TowerSyntax(
                            base.getSyntax(), resultSyntax.getLeft(), resultSyntax.getRight()),
                    new Tower(resultSemantics.getTop(), base.getSemantics())
            ));
        }
        return ret;
    }

    public static class Creator implements IResourceObjectCreator<ReversibleLiftRight> {

        private String	type;

        public Creator() {
            this("rule.recursive.lift.right.reversible");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ReversibleLiftRight create(ParameterizedExperiment.Parameters params,
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
            return new ReversibleLiftRight(RULE_LABEL,
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
