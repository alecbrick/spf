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
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic.MonadRule;

import java.util.HashSet;
import java.util.Set;

public class ForwardReversibleMonadicApplication implements IBinaryReversibleParseRule<LogicalExpression> {

    private final AbstractReversibleApplication baseReversibleRule;
    private final MonadRule<LogicalExpression, Monad> baseRule;

    public ForwardReversibleMonadicApplication(AbstractReversibleApplication baseReversibleRule,
                                               MonadRule baseMonadRule) {
        this.baseReversibleRule = baseReversibleRule;
        this.baseRule = baseMonadRule;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
        Set<Category<LogicalExpression>> rights = baseReversibleRule.reverseApplyLeft(left, result, span);
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
        LogicalExpression rightSemantics = right.getSemantics();
        Monad rightMonad = MonadServices.logicalExpressionToMonad(rightSemantics);
        Category<LogicalExpression> newRight = Category.create(right.getSyntax(), rightMonad);
        Set<Category<LogicalExpression>> lefts = baseReversibleRule.reverseApplyRight(newRight, result, span);
        return lefts;
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
            IResourceObjectCreator<ForwardReversibleMonadicApplication> {

		private final String	type;

		public Creator() {
			this("rule.monadic.application.reversible");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ForwardReversibleMonadicApplication create(ParameterizedExperiment.Parameters params,
                                                          IResourceRepository repo) {
            ForwardReversibleApplication baseReversibleRule = repo.get(params.get("baseReversibleRule"));
            MonadRule<LogicalExpression, Monad> baseMonadRule = repo.get(params.get("baseMonadRule"));
			return new ForwardReversibleMonadicApplication(baseReversibleRule, baseMonadRule);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
                    ForwardReversibleMonadicApplication.class)
                    .addParam("baseReversibleRule", ForwardReversibleApplication.class, "The non-monadic reversible form of this rule.")
                    .addParam("baseMonadRule", MonadRule.class, "The monadic non-reversible form of this rule.")
					.build();
		}

	}
}
