package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.DelimitSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit.DelimitLeft;

import java.util.HashSet;
import java.util.Set;

public class ReversibleDelimitLeft extends DelimitLeft<LogicalExpression>
        implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {
    private final Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules;
    private final Set<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveParseRules;
    private final ForwardReversibleApplication forwardApp;

    public ReversibleDelimitLeft(String label,
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
        // TODO, though likely unnecessary.
        return ret;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        Category<LogicalExpression> rightBottom = towerCategoryServices.getBottom(right);
        if (!(rightBottom instanceof ComplexCategory)) {
            return ret;
        }
        ComplexCategory<LogicalExpression> complexRightBottom = (ComplexCategory<LogicalExpression>) rightBottom;
        if (!complexRightBottom.getSlash().equals(Slash.BACKWARD) || !(complexRightBottom.getSyntax().getRight() instanceof DelimitSyntax)) {
            return ret;
        }
        ComplexSyntax rightBottomSyntax = complexRightBottom.getSyntax();
        DelimitSyntax delimitedSyntax = (DelimitSyntax) rightBottomSyntax.getRight();

        ComplexSyntax newRightBaseSyntax =
                new ComplexSyntax(rightBottomSyntax.getLeft(), delimitedSyntax.getWrappedSyntax(), Slash.FORWARD);
        Category<LogicalExpression> newRightBottom = Category.create(newRightBaseSyntax, rightBottom.getSemantics());
        Category<LogicalExpression> newRight = towerCategoryServices.setBottom(right, newRightBottom);

        Category<LogicalExpression> resultBottom = towerCategoryServices.getBottom(result);
        Set<Category<LogicalExpression>> baseRights = forwardApp.reverseApplyLeft(newRight, resultBottom, span);
        if (baseRights.isEmpty()) {
            return ret;
        }

        Set<Category<LogicalExpression>> lefts = new HashSet<>();
        for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : recursiveParseRules) {
            lefts.addAll(rule.reverseApplyRight(newRight, result, span));
        }
        if (!(result instanceof TowerCategory) &&
            !(right instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                lefts.addAll(rule.reverseApplyRight(newRight, result, null));
            }
        }

        // reverse reset
        Set<Category<LogicalExpression>> resetLefts = new HashSet<>();
        for (Category<LogicalExpression> left : lefts) {
            if (left instanceof TowerCategory) {
                resetLefts.addAll(towerCategoryServices.reverseReset(left));
            } else {
                resetLefts.add(left);
            }
        }
        return resetLefts;
    }
}
