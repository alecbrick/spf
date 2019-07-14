package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.monad;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Monad;
import edu.cornell.cs.nlp.spf.mr.lambda.StateMonad;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.AbstractReversibleApplication;

import java.util.HashSet;
import java.util.Set;

public abstract class ReversibleMonadicApplication implements IBinaryReversibleParseRule<LogicalExpression> {

    private final AbstractReversibleApplication baseRule;

    public ReversibleMonadicApplication(AbstractReversibleApplication baseRule) {
        this.baseRule = baseRule;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> rights = baseRule.reverseApplyLeft(left, result, span);
        Set<Category<LogicalExpression>> ret = new HashSet<>();

        for (Category<LogicalExpression> right : rights) {
            if (right.getSemantics() instanceof StateMonad) {
                ret.add(Category.create(right.getSyntax(), ((StateMonad) right.getSemantics()).getBody()));
            }
            ret.add(right);
        }
        return ret;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> lefts = baseRule.reverseApplyRight(right, result, span);
        Set<Category<LogicalExpression>> ret = new HashSet<>();

        for (Category<LogicalExpression> left : lefts) {
            if (left.getSemantics() instanceof StateMonad) {
                ret.add(Category.create(left.getSyntax(), ((StateMonad) left.getSemantics()).getBody()));
            }
            ret.add(left);
        }
        return ret;
    }

    @Override
    public RuleName getName() {
        return null;
    }
}
