package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.*;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.MonadServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;

import java.util.ArrayList;
import java.util.List;

public class DelimitRight<MR> extends AbstractDelimit<MR> {
    public DelimitRight(String label,
                        ITowerCategoryServices<MR> towerCategoryServices,
                        BinaryRuleSet<MR> baseRules) {
        super(label + "Right", towerCategoryServices, baseRules);
    }

    public List<ParseRuleResult<MR>> applyRecursive(
            Category<MR> left,
            Category<MR> right,
            SentenceSpan span,
            List<IBinaryRecursiveParseRule<MR>> validRules) {
        Category<MR> leftBottom = towerCategoryServices.getBottom(left);
        Category<MR> rightBase = towerCategoryServices.getBottom(right);

        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        if (!(left instanceof ComplexCategory)) {
            return ret;
        }

        ComplexCategory<MR> complexLeftBottom = (ComplexCategory<MR>) leftBottom;
        ComplexSyntax leftBaseSyntax = complexLeftBottom.getSyntax();
        if (!leftBaseSyntax.getSlash().equals(Slash.FORWARD) ||
                !(leftBaseSyntax.getRight() instanceof DelimitSyntax)) {
            return ret;
        }

        DelimitSyntax delimitedSyntax = (DelimitSyntax) leftBaseSyntax.getRight();
        if (!delimitedSyntax.getWrappedSyntax().equals(rightBase.getSyntax())) {
            return ret;
        }

        Category<MR> newRight = right;
        if (right instanceof TowerCategory) {
            TowerCategory<MR> rightTower = (TowerCategory<MR>) right;
            Category<MR> loweredRight = towerCategoryServices.lower(rightTower);
            newRight = towerCategoryServices.monadicLift(loweredRight, MonadServices.getMonadSyntax());
            if (newRight == null) {
                return ret;
            }
        }

        ComplexSyntax newLeftBaseSyntax =
                new ComplexSyntax(leftBaseSyntax.getLeft(), delimitedSyntax.getWrappedSyntax(), Slash.FORWARD);
        Category<MR> newLeftBottom = Category.create(newLeftBaseSyntax, leftBottom.getSemantics());
        Category<MR> newLeft = towerCategoryServices.setBottom(left, newLeftBottom);


        List<ParseRuleResult<MR>> results =
                combineRecursive(newLeft, newRight, span, validRules);
        for (ParseRuleResult<MR> result : results) {
            Category<MR> resultCategory = result.getResultCategory();
            RuleName newRuleName = RecursiveRuleName.create(name.getLabel(),
                    result.getRuleName());
            ret.add(new ParseRuleResult<>(newRuleName, resultCategory));
        }

        return ret;
    }

    public static class Creator<MR> implements
            IResourceObjectCreator<DelimitRight<MR>> {

        private String	type;

        public Creator() {
            this("rule.recursive.delimit.right");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public DelimitRight<MR> create(ParameterizedExperiment.Parameters params,
                                       IResourceRepository repo) {
            String id = params.get("baseRules");
            BinaryRuleSet<MR> baseList = repo.get(id);
            return new DelimitRight<>(RULE_LABEL,
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    baseList);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage.builder(type, DelimitRight.class)
                    .addParam("baseRules", BinaryRuleSet.class,
                            "Binary rules to apply at the base level.")
                    .build();
        }

    }
}
