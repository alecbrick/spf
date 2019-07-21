package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lower;

import edu.cornell.cs.nlp.spf.ccg.categories.*;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;

public class UnaryLower<MR> implements IUnaryParseRule<MR> {
	private static final long			serialVersionUID	= -6282371109224310054L;
	protected final ITowerCategoryServices<MR> towerCategoryServices;
	protected final UnaryRuleName ruleName;

	public UnaryLower(String label,
					  ITowerCategoryServices<MR> towerCategoryServices) {
	    this.ruleName = UnaryRuleName.create(label);
        this.towerCategoryServices = towerCategoryServices;
	}

	@Override
	public ParseRuleResult<MR> apply(Category<MR> category, SentenceSpan span) {
		if (!(category instanceof TowerCategory)) {
			return null;
		}
		TowerCategory<MR> towerCategory = (TowerCategory<MR>) category;

		final Category<MR> lowered = towerCategoryServices.lower(towerCategory);

		if (lowered == null) {
			return null;
		} else {
			return new ParseRuleResult<>(ruleName, lowered);
		}
	}

	@Override
	public boolean isValidArgument(Category<MR> category, SentenceSpan span) {
		return (category instanceof TowerCategory);
	}

	@Override
	public UnaryRuleName getName() {
		return ruleName;
	}

	@Override
	public String toString() {
		return ruleName.toString();
	}

	public static class Creator<MR> implements
            IResourceObjectCreator<UnaryLower<MR>> {

		private final String	type;

		public Creator() {
			this("rule.recursive.lower.unary");
		}

		public Creator(String type) {
			this.type = type;
		}

		@Override
		public UnaryLower<MR> create(Parameters params,
                                                  IResourceRepository repo) {
			final ITowerCategoryServices<MR> towerCategoryServices = repo
					.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE);
			return new UnaryLower<>(params.get("name"),
					towerCategoryServices);
		}

		@Override
		public String type() {
			return type;
		}

		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type,
					UnaryLower.class)
					.addParam("name", String.class, "Rule name")
					.build();
		}

	}
}
