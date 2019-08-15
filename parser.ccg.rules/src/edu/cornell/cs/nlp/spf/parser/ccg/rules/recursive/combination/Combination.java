package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.combination;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.AbstractBinaryRecursiveParseRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Combination<MR> extends AbstractBinaryRecursiveParseRule<MR> {

    public static final String				RULE_LABEL	= "combine";

    public Combination(String label, ITowerCategoryServices<MR> towerCategoryServices,
                       BinaryRuleSet<MR> baseRules) {
        super(label, towerCategoryServices, baseRules);
    }

    @Override
    public List<ParseRuleResult<MR>> applyRecursive(Category<MR> left,
                                                    Category<MR> right,
                                                    SentenceSpan span,
                                                    List<IBinaryRecursiveParseRule<MR>> validRules) {
        if (!(left instanceof TowerCategory && right instanceof TowerCategory)) {
            return Collections.emptyList();
        }
        TowerCategory<MR> leftTower = (TowerCategory<MR>) left;
        TowerCategory<MR> rightTower = (TowerCategory<MR>) right;

        if (!towerCategoryServices.canCombineTops(leftTower, rightTower)) {
            return Collections.emptyList();
        }
        TowerSyntax leftSyntax = leftTower.getSyntax();
        TowerSyntax rightSyntax = rightTower.getSyntax();
        if (leftSyntax.getRight().unify(rightSyntax.getLeft()) == null) {
            return Collections.emptyList();
        }



        Category<MR> leftBase = towerCategoryServices.getBottom(leftTower);
        Category<MR> rightBase = towerCategoryServices.getBottom(rightTower);
        List<ParseRuleResult<MR>> results = combineRecursive(leftBase, rightBase, span, validRules);
        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        for (ParseRuleResult<MR> result : results) {
            Category<MR> resultCategory = result.getResultCategory();
            TowerCategory<MR> resultTower =
                    towerCategoryServices.combineTowersWithBase(
                            leftTower, rightTower, resultCategory);
            RuleName newRuleName = createRuleName(result);
            ret.add(new ParseRuleResult<>(newRuleName, resultTower));
        }
        return ret;
    }

    @Override
    public ParseRuleResult<MR> apply(Category<MR> left,
                                     Category<MR> right, SentenceSpan span) {
        return null;
    }

    @Override
    public RuleName getName() {
        return name;
    }

    public static class Creator<MR> implements IResourceObjectCreator<Combination> {

        private String	type;

        public Creator() {
            this("rule.recursive.combination");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Combination<MR> create(ParameterizedExperiment.Parameters params,
                                             IResourceRepository repo) {
            String id = params.get("baseRules");
            BinaryRuleSet<MR> baseList = repo.get(id);
            return new Combination<MR>(RULE_LABEL,
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    baseList);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage.builder(type, Combination.class)
                    .addParam("baseRules", BinaryRuleSet.class,
                            "Binary rules to apply at the base level.")
                    .build();
        }

    }
}
