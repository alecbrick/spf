package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.monad;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Monad;
import edu.cornell.cs.nlp.spf.mr.lambda.MonadParams;
import edu.cornell.cs.nlp.spf.mr.lambda.MonadServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.UnaryRuleName;

import java.util.HashSet;
import java.util.Set;

public class ReversibleUnaryUnwrap implements IUnaryReversibleParseRule<LogicalExpression> {
    protected final UnaryRuleName						name;

    public ReversibleUnaryUnwrap(String label) {
        this.name = UnaryRuleName.create(label);
    }

    @Override
    public ParseRuleResult<LogicalExpression> apply(Category<LogicalExpression> category, SentenceSpan span) {
        if (!(category.getSemantics() instanceof Monad)) {
            return null;
        }
        Monad monadSem = (Monad) category.getSemantics();
        // in case it's a binding, though unlikely
        MonadParams resultParams = monadSem.exec();
        LogicalExpression result = resultParams.getOutput();
        return new ParseRuleResult<>(getName(), Category.create(category.getSyntax(), result));
    }

    @Override
    public UnaryRuleName getName() {
        return name;
    }

    @Override
    public boolean isValidArgument(Category<LogicalExpression> category, SentenceSpan span) {
        return category.getSemantics() instanceof Monad;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApply(Category<LogicalExpression> result, SentenceSpan span) {
        Monad inputSem = MonadServices.logicalExpressionToMonad(result.getSemantics());
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        ret.add(Category.create(result.getSyntax(), inputSem));
        return ret;
    }

    public static class Creator implements
			IResourceObjectCreator<ReversibleUnaryUnwrap> {

		private final String	type;

		public Creator() {
			this("rule.monadic.unwrap.reversible");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ReversibleUnaryUnwrap create(ParameterizedExperiment.Parameters params,
											IResourceRepository repo) {
			return new ReversibleUnaryUnwrap(params.get("name"));
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					ReversibleUnaryUnwrap.class)
					.addParam("name", String.class, "Rule name")
					.build();
		}

	}
}
