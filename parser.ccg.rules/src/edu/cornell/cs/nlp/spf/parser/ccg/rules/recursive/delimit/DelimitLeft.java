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
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;

import java.util.ArrayList;
import java.util.List;

public class DelimitLeft<MR> extends AbstractDelimit<MR> {
    public DelimitLeft(String label,
                       ITowerCategoryServices<MR> towerCategoryServices,
                       BinaryRuleSet<MR> baseRules) {
        super(label + "Left", towerCategoryServices, baseRules);
    }

    public List<ParseRuleResult<MR>> applyRecursive(
            Category<MR> left,
            Category<MR> right,
            SentenceSpan span,
            List<IBinaryRecursiveParseRule<MR>> validRules) {
        Category<MR> leftBottom = towerCategoryServices.getBottom(left);
        Category<MR> rightBottom = towerCategoryServices.getBottom(right);

        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        if (!(right instanceof ComplexCategory)) {
            return ret;
        }

        ComplexCategory<MR> complexRightBase = (ComplexCategory<MR>) rightBottom;
        ComplexSyntax rightBaseSyntax = complexRightBase.getSyntax();
        if (!rightBaseSyntax.getSlash().equals(Slash.BACKWARD) ||
                !(rightBaseSyntax.getRight() instanceof DelimitSyntax)) {
            return ret;
        }

        DelimitSyntax delimitedSyntax = (DelimitSyntax) rightBaseSyntax.getRight();
        if (!delimitedSyntax.getWrappedSyntax().equals(leftBottom.getSyntax())) {
            return ret;
        }

        Category<MR> newLeft = left;
        if (left instanceof TowerCategory) {
            TowerCategory<MR> leftTower = (TowerCategory<MR>) left;
            Category<MR> loweredLeft = towerCategoryServices.lower(leftTower);
            newLeft = towerCategoryServices.monadicLift(loweredLeft, Syntax.S);
            if (newLeft == null) {
                return ret;
            }
        }

        ComplexSyntax newRightBaseSyntax =
                new ComplexSyntax(rightBaseSyntax.getLeft(), delimitedSyntax.getWrappedSyntax(), Slash.BACKWARD);
        Category<MR> newRightBottom = Category.create(newRightBaseSyntax, rightBottom.getSemantics());
        Category<MR> newRight = towerCategoryServices.setBottom(right, newRightBottom);


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
            IResourceObjectCreator<DelimitLeft<MR>> {

        private String	type;

        public Creator() {
            this("rule.recursive.delimit.left");
        }

        public Creator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public DelimitLeft<MR> create(ParameterizedExperiment.Parameters params,
                                      IResourceRepository repo) {
            String id = params.get("baseRules");
            BinaryRuleSet<MR> baseList = repo.get(id);
            return new DelimitLeft<>(RULE_LABEL,
                    repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE),
                    baseList);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage.builder(type, DelimitLeft.class)
                    .addParam("baseRules", BinaryRuleSet.class,
                            "Binary rules to apply at the base level.")
                    .build();
        }

    }
}
