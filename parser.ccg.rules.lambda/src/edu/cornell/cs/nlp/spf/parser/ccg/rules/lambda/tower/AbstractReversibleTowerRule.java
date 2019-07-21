package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleRecursiveParseRule;

import java.util.Set;

public abstract class AbstractReversibleTowerRule implements IBinaryReversibleRecursiveParseRule<LogicalExpression> {
    protected Set<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveParseRules;

    @Override
    public void addRecursiveRule(IBinaryReversibleRecursiveParseRule<LogicalExpression> rule) {
        recursiveParseRules.add(rule);
    }
}
