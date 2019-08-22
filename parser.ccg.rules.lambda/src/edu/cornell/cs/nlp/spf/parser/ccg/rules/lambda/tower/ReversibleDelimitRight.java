package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.DelimitSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit.DelimitLeft;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit.DelimitRight;
import edu.cornell.cs.nlp.utils.log.Log;

import java.util.HashSet;
import java.util.Set;

public class ReversibleDelimitRight extends DelimitRight<LogicalExpression>
        implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {
    private final Set<IBinaryReversibleParseRule<LogicalExpression>> reversibleBaseRules;
    private final Set<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveParseRules;
    private final ForwardReversibleApplication forwardApp;

    public ReversibleDelimitRight(String label,
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
        Category<LogicalExpression> leftBottom = towerCategoryServices.getBottom(left);
        if (!(leftBottom instanceof ComplexCategory)) {
            return ret;
        }
        ComplexCategory<LogicalExpression> complexLeftBottom = (ComplexCategory<LogicalExpression>) leftBottom;
        if (!complexLeftBottom.getSlash().equals(Slash.FORWARD) || !(complexLeftBottom.getSyntax().getRight() instanceof DelimitSyntax)) {
            return ret;
        }
        ComplexSyntax leftBottomSyntax = complexLeftBottom.getSyntax();
        DelimitSyntax delimitedSyntax = (DelimitSyntax) leftBottomSyntax.getRight();

        ComplexSyntax newLeftBaseSyntax =
                new ComplexSyntax(leftBottomSyntax.getLeft(), delimitedSyntax.getWrappedSyntax(), Slash.FORWARD);
        Category<LogicalExpression> newLeftBottom = Category.create(newLeftBaseSyntax, leftBottom.getSemantics());
        Category<LogicalExpression> newLeft = towerCategoryServices.setBottom(left, newLeftBottom);

        Category<LogicalExpression> resultBottom = towerCategoryServices.getBottom(result);
        Set<Category<LogicalExpression>> baseRights = forwardApp.reverseApplyLeft(newLeft, resultBottom, span);
        if (baseRights.isEmpty()) {
            return ret;
        }

        // TODO: Delimit is not necessarily a tower rule!
        Set<Category<LogicalExpression>> rights = new HashSet<>();
        for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : recursiveParseRules) {
            rights.addAll(rule.reverseApplyLeft(newLeft, result, span));
        }
        if (!(result instanceof TowerCategory) &&
            !(left instanceof TowerCategory)) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : reversibleBaseRules) {
                rights.addAll(rule.reverseApplyLeft(newLeft, result, null));
            }
        }

        // reverse reset
        Set<Category<LogicalExpression>> resetRights = new HashSet<>();
        for (Category<LogicalExpression> right : rights) {
            if (right instanceof TowerCategory) {
                resetRights.addAll(towerCategoryServices.reverseReset(right));
            } else {
                resetRights.add(right);
            }
        }
        return resetRights;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();

        // TODO, though likely unnecessary.
        return ret;
    }
}
