package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.mr.lambda.Variable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ApplyAndSimplify;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetBindingMapping;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.combination.Combination;

import java.util.*;

public class ReversibleCombination extends Combination<LogicalExpression> implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {

    public static final String				RULE_LABEL	= "combine";
    private final ForwardReversibleApplication forwardApp;
    private final Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules;
    private final Set<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveParseRules;

    public ReversibleCombination(String label, ITowerCategoryServices<LogicalExpression> towerCategoryServices,
                                 BinaryRuleSet<LogicalExpression> baseRules,
                                 Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules,
                                 ForwardReversibleApplication forwardApp) {
        super(label, towerCategoryServices, baseRules);
        this.reversibleBaseRules = reversibleBaseRules;
        this.recursiveParseRules = new HashSet<>();
        this.forwardApp = forwardApp;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyLeft(
            Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {

        Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(left instanceof TowerCategory &&
                result instanceof TowerCategory)) {
            return ret;
        }

        TowerCategory<LogicalExpression> leftTower = (TowerCategory<LogicalExpression>) left;
        TowerCategory<LogicalExpression> resultTower = (TowerCategory<LogicalExpression>) result;

        TowerSyntax leftSyntax = leftTower.getSyntax();
        TowerSyntax resultSyntax = resultTower.getSyntax();

        Tower leftSem = (Tower) leftTower.getSemantics();
        Tower resultSem = (Tower) resultTower.getSemantics();

        Lambda leftTop = leftSem.getTop();
        Lambda resultTop = resultSem.getTop();

        // split tops
        // We don't have reversible composition, so we emulate it
        // with reverse application.
        Set<Category<LogicalExpression>> rightTops =
                forwardApp.reverseApplyLeft(
                    Category.create(new ComplexSyntax(
                            leftSyntax.getLeft(), leftSyntax.getRight(),
                            Slash.FORWARD), leftTop),
                    Category.create(resultSyntax.getLeft(),
                            resultTop.getBody()), span);

        if (rightTops.isEmpty()) {
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

        Set<Category<LogicalExpression>> bases = new HashSet<>();
        if (!(leftBase instanceof TowerCategory) && !(resultBase instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                bases.addAll(rule.reverseApplyLeft(leftBase, resultBase, span));
            }
        } else {
            for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : recursiveParseRules) {
                bases.addAll(rule.reverseApplyLeft(leftBase, resultBase, span));
            }
        }

        if (bases.isEmpty()) {
            return ret;
        }

        for (Category<LogicalExpression> top : rightTops) {
            Lambda newTopSem = new Lambda(resultTop.getArgument(), top.getSemantics());
            for (Category<LogicalExpression> base : bases) {
                ret.add(new TowerCategory<>(
                        new TowerSyntax(base.getSyntax(), top.getSyntax(), resultSyntax.getRight()),
                        new Tower(newTopSem, base.getSemantics())
                ));
            }
        }

        return ret;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
                Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(right instanceof TowerCategory &&
                result instanceof TowerCategory)) {
            return ret;
        }

        TowerCategory<LogicalExpression> rightTower = (TowerCategory<LogicalExpression>) right;
        TowerCategory<LogicalExpression> resultTower = (TowerCategory<LogicalExpression>) result;

        TowerSyntax rightSyntax = rightTower.getSyntax();
        TowerSyntax resultSyntax = resultTower.getSyntax();

        Tower rightSem = (Tower) rightTower.getSemantics();
        Tower resultSem = (Tower) resultTower.getSemantics();


        Lambda rightTop = rightSem.getTop();
        Lambda resultTop = resultSem.getTop();
        Variable resultArgument = resultTop.getArgument();
        LogicalExpression appliedRight = ApplyAndSimplify.of(rightTop, resultArgument);

        LogicalExpression topBodySemantics = resultSem.getTop().getBody();
        if (resultSyntax.getLeft() instanceof TowerSyntax) {
            topBodySemantics = towerCategoryServices.lambdaToTower(topBodySemantics);
        }

        // split tops
        // We don't have reversible composition, so we emulate it
        // with reverse application.
        Set<Category<LogicalExpression>> leftTops =
                forwardApp.reverseApplyRight(
                    Category.create(rightSyntax.getLeft(), appliedRight),
                    Category.create(resultSyntax.getLeft(),
                            topBodySemantics), span);

        if (leftTops.isEmpty()) {
            return ret;
        }

        // Get a mapping between the binding variables so that we can keep track
        // of them during recursion.
        Map<Variable, Variable> mapping = GetBindingMapping.of(topBodySemantics, appliedRight);

        Category<LogicalExpression> rightBase = towerCategoryServices.getBase(rightTower);
        Category<LogicalExpression> resultBase = towerCategoryServices.getBase(resultTower);

        if (mapping.size() > 0) {
            LogicalExpression rightBaseSem = rightBase.getSemantics();
            for (Variable resultVar : mapping.keySet()) {
                Variable rightVar = mapping.get(resultVar);
                rightBaseSem = ReplaceExpression.of(rightBaseSem, rightVar, resultVar);
            }
            rightBase = Category.create(rightBase.getSyntax(), rightBaseSem);
        }

        Set<Category<LogicalExpression>> bases = new HashSet<>();
        if (!(rightBase instanceof TowerCategory) && !(resultBase instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                bases.addAll(rule.reverseApplyRight(rightBase, resultBase, span));
            }
        } else {
            for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : recursiveParseRules) {
                bases.addAll(rule.reverseApplyRight(rightBase, resultBase, span));
            }
        }

        if (bases.isEmpty()) {
            return ret;
        }

        for (Category<LogicalExpression> top : leftTops) {
            ComplexCategory<LogicalExpression> complexTop =
                    (ComplexCategory<LogicalExpression>) top;
            for (Category<LogicalExpression> base : bases) {
                ret.add(new TowerCategory<>(
                        new TowerSyntax(base.getSyntax(),
                                complexTop.getSyntax().getLeft(),
                                complexTop.getSyntax().getRight()),
                        new Tower((Lambda) complexTop.getSemantics(), base.getSemantics())
                ));
            }
        }

        return ret;
    }

    @Override
    public void addRecursiveRule(IBinaryReversibleRecursiveParseRule<LogicalExpression> rule) {
        this.recursiveParseRules.add(rule);
    }

    public static class Creator implements IResourceObjectCreator<ReversibleCombination> {

        private String	type;

        public Creator() {
            this("rule.recursive.combination.reversible");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ReversibleCombination create(ParameterizedExperiment.Parameters params,
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
            return new ReversibleCombination(RULE_LABEL,
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
