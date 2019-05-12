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

public class LiftRight<MR> extends AbstractLift<MR> {

    public LiftRight(String label, ITowerCategoryServices<MR> towerCategoryServices,
                    BinaryRuleSet baseRules) {
        super(label + "Right", towerCategoryServices, baseRules);
    }

    // All right-raising-specific logic goes here
    public List<ParseRuleResult<MR>> applyRecursive(
            Category<MR> left,
            Category<MR> right,
            SentenceSpan span,
            List<IRecursiveBinaryParseRule<MR>> validRules) {
        // Left must be a tower
        if (!(left instanceof TowerCategory)) {
            return new ArrayList<>();
        }
        TowerCategory<MR> leftTower = (TowerCategory<MR>) left;
        Category<MR> leftBase = towerCategoryServices.getBase(leftTower);

        List<IRecursiveBinaryParseRule<MR>> newValidRules = new ArrayList<>(validRules);
        if ((right instanceof TowerCategory)) {
            TowerCategory rightTower = (TowerCategory) right;
            if (leftTower.height() == rightTower.height()) {
                newValidRules.remove(this);
            } else if (leftTower.height() < rightTower.height()) {
                return Collections.emptyList();
            }
        }

        List<ParseRuleResult<MR>> results =
                combineRecursive(leftBase, right, span, newValidRules);
        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        for (ParseRuleResult<MR> result : results) {
            Category<MR> resultCategory = result.getResultCategory();
            TowerCategory<MR> resultTower =
                    towerCategoryServices.replaceBase(
                            leftTower, resultCategory);
            RuleName newRuleName = createRuleName(result);
            ret.add(new ParseRuleResult<>(newRuleName, resultTower));
        }
        return ret;
    }

    public static class Creator<MR> implements
            IResourceObjectCreator<LiftRight<MR>> {

        private String type;

        public Creator() {
            this("rule.recursive.lift.right");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public LiftRight<MR> create(ParameterizedExperiment.Parameters params,
                                   IResourceRepository repo) {
            String id = params.get("baseRules");
            BinaryRuleSet<MR> baseList = repo.get(id);
            return new LiftRight<>(RULE_LABEL,
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    baseList);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage.builder(type, LiftRight.class)
                    .addParam("baseRules", BinaryRuleSet.class,
                            "Binary rules to apply at the base level.")
                    .build();
        }
    }
}
