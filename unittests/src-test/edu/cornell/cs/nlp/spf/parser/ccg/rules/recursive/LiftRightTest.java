package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.ContinuationTower;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.combination.Combination;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftRight;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class LiftRightTest {
    public LiftRightTest() {
        TestServices.init();
    }

    @Test
	public void test() {
		final TowerCategory<LogicalExpression> primary = (TowerCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S//(NP/N)\\\\S : [(lambda $0:<<e,t>,e> $0][(lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))]");
		final ComplexCategory<LogicalExpression> secondary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");
		Assert.assertTrue(primary.getSemantics() instanceof ContinuationTower);
		Assert.assertTrue(secondary.getSemantics() instanceof Lambda);

		final LiftRight<LogicalExpression> rule = new LiftRight<>(
				"^", TestServices.getTowerCategoryServices(), TestServices.getBaseRules());
		final List<ParseRuleResult<LogicalExpression>> actual = rule.applyRecursive(primary,
				secondary, null, new ArrayList<>());
		final TowerCategory<LogicalExpression> expected = (TowerCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S//(NP/N)\\\\S : [(lambda $0:<<e,t>,e> $0)][(lambda $1:<e,t> (the:<<e,t>,e> (lambda $2:e (and:<t*,t> (loc:<lo,<lo,t>> $2 alaska:s) ($1 $2)))))]");
		boolean hasExpected = false;
		for (ParseRuleResult<LogicalExpression> result : actual) {
			if (result.getResultCategory().equals(expected)) {
				hasExpected = true;
			}
		}
		Assert.assertTrue(hasExpected);
	}
}
