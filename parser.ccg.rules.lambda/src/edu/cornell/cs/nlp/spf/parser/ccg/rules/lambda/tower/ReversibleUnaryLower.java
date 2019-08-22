package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.MonadServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IUnaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lower.UnaryLower;

import java.util.HashSet;
import java.util.Set;

public class ReversibleUnaryLower extends UnaryLower<LogicalExpression>
        implements IUnaryReversibleParseRule<LogicalExpression> {
    public ReversibleUnaryLower(String label, ITowerCategoryServices<LogicalExpression> towerCategoryServices) {
        super(label, towerCategoryServices);
    }

    @Override
    public Set<Category<LogicalExpression>> reverseApply(Category<LogicalExpression> result, SentenceSpan span) {
        if (!(result.getSyntax().equals(Syntax.S) ||
				result.getSyntax().equals(MonadServices.getMonadSyntax()))) {
            return new HashSet<>();
        }

        return new HashSet<>(towerCategoryServices.unlower(result));
    }

    public static class Creator implements
            IResourceObjectCreator<ReversibleUnaryLower> {

		private final String	type;

		public Creator() {
			this("rule.recursive.lower.unary.reversible");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public ReversibleUnaryLower create(ParameterizedExperiment.Parameters params,
															  IResourceRepository repo) {
			final ITowerCategoryServices<LogicalExpression> towerCategoryServices = repo
					.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE);
			return new ReversibleUnaryLower(params.get("name"),
					towerCategoryServices);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					ReversibleUnaryLower.class)
					.addParam("name", String.class, "Rule name")
					.build();
		}

	}
}
