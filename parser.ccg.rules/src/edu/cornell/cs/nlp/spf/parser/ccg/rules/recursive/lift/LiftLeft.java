package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LiftLeft<MR> extends AbstractLift<MR> {
    public LiftLeft(String label,
                    ITowerCategoryServices<MR> towerCategoryServices,
                    BinaryRuleSet<MR> baseRules) {
        super(label + "Left", towerCategoryServices, baseRules);
    }

    // All left-raising-specific logic goes here
    public List<ParseRuleResult<MR>> applyRecursive(
            Category<MR> left,
            Category<MR> right,
            SentenceSpan span,
            List<IBinaryRecursiveParseRule<MR>> validRules) {
        // Right must be a tower
        if (!(right instanceof TowerCategory)) {
            return new ArrayList<>();
        }
        TowerCategory<MR> rightTower = (TowerCategory<MR>) right;
        Category<MR> rightBase = towerCategoryServices.getBase(rightTower);

        // Don't lift unnecessarily
        // TODO i think it's broken
        List<IBinaryRecursiveParseRule<MR>> newValidRules =
                new ArrayList<>(validRules);
        if ((left instanceof TowerCategory)) {
            TowerCategory leftTower = (TowerCategory) left;
            if (leftTower.height() == rightTower.height()) {
                newValidRules.remove(this);
            } else if (leftTower.height() > rightTower.height()) {
                return Collections.emptyList();
            }
        }

        List<ParseRuleResult<MR>> results =
                combineRecursive(left, rightBase, span, newValidRules);
        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        for (ParseRuleResult<MR> result : results) {
            Category<MR> resultCategory = result.getResultCategory();
            TowerCategory<MR> resultTower =
                    towerCategoryServices.replaceBase(
                            rightTower, resultCategory);
            RuleName newRuleName = RecursiveRuleName.create(name.getLabel(),
                    result.getRuleName());
            ret.add(new ParseRuleResult<>(newRuleName, resultTower));
        }

        TowerCategory<MR> leftMonadTower = towerCategoryServices.monadicLift(
                left, rightTower.getSyntax().getLeft());
        if (leftMonadTower == null) {
            return ret;
        }
        // We use combination so as not to duplicate code, but since this
        // is a lift, we need to replace the rule name.
        results = combination.applyRecursive(leftMonadTower, right, span,
                newValidRules);
        for (ParseRuleResult<MR> result : results) {
            RecursiveRuleName oldRuleName =
                    (RecursiveRuleName) result.getRuleName();
            RecursiveRuleName newRuleName = RecursiveRuleName.create(
                    name.getLabel(), oldRuleName.getChild());
            ret.add(new ParseRuleResult<>(newRuleName,
                    result.getResultCategory()));
        }
        return ret;
    }

    public static class Creator<MR> implements
            IResourceObjectCreator<LiftLeft<MR>> {

        private String	type;

        public Creator() {
            this("rule.recursive.lift.left");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LiftLeft<MR> create(ParameterizedExperiment.Parameters params,
                                      IResourceRepository repo) {
            String id = params.get("baseRules");
            BinaryRuleSet<MR> baseList = repo.get(id);
            return new LiftLeft<>(RULE_LABEL,
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    baseList);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage.builder(type, LiftLeft.class)
                    .addParam("baseRules", BinaryRuleSet.class,
                            "Binary rules to apply at the base level.")
                    .build();
        }

    }
}
