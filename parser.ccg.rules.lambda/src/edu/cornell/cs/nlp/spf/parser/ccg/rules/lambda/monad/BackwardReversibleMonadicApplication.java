package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.monad;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Monad;
import edu.cornell.cs.nlp.spf.mr.lambda.MonadServices;
import edu.cornell.cs.nlp.spf.mr.lambda.StateMonad;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.RuleName;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.AbstractReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.BackwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic.MonadRule;

import java.util.HashSet;
import java.util.Set;

public class BackwardReversibleMonadicApplication implements IBinaryReversibleParseRule<LogicalExpression> {

    private final AbstractReversibleApplication baseReversibleRule;
    private final MonadRule<LogicalExpression, Monad> baseRule;

    public BackwardReversibleMonadicApplication(AbstractReversibleApplication baseReversibleRule,
                                                MonadRule baseMonadRule) {
        this.baseReversibleRule = baseReversibleRule;
        this.baseRule = baseMonadRule;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
        LogicalExpression leftSemantics = left.getSemantics();
        Monad leftMonad = MonadServices.logicalExpressionToMonad(leftSemantics);
        Category<LogicalExpression> newLeft = Category.create(left.getSyntax(), leftMonad);
        return baseReversibleRule.reverseApplyLeft(newLeft, result, span);
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> lefts = baseReversibleRule.reverseApplyRight(right, result, span);
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
    public ParseRuleResult<LogicalExpression> apply(Category<LogicalExpression> left, Category<LogicalExpression> right, SentenceSpan span) {
        return baseRule.apply(left, right, span);
    }

    @Override
    public RuleName getName() {
        return null;
    }

    public static class Creator implements
            IResourceObjectCreator<BackwardReversibleMonadicApplication> {

		private final String	type;

		public Creator() {
			this("rule.monadic.application.reversible");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public BackwardReversibleMonadicApplication create(ParameterizedExperiment.Parameters params,
                                                           IResourceRepository repo) {
            BackwardReversibleApplication baseReversibleRule = repo.get(params.get("baseReversibleRule"));
            MonadRule<LogicalExpression, Monad> baseMonadRule = repo.get(params.get("baseMonadRule"));
			return new BackwardReversibleMonadicApplication(baseReversibleRule, baseMonadRule);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					BackwardReversibleMonadicApplication.class)
                    .addParam("baseReversibleRule", BackwardReversibleApplication.class, "The non-monadic reversible form of this rule.")
                    .addParam("baseMonadRule", MonadRule.class, "The monadic non-reversible form of this rule.")
					.build();
		}

	}
}
