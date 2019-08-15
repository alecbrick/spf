package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.delimit.DelimitRight;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class DelimitTest {
    public DelimitTest() {
        TestServices.init();
    }

    @Test
	public void test() {
		final ComplexCategory<LogicalExpression> primary = (ComplexCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S/<S> : (lambda $0:<e,t> (if:<<e,t>,<e,t>> $0)");
		final TowerCategory<LogicalExpression> secondary = (TowerCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S//S\\\\S : [(lambda $0:M[<e,t>] (>>= $1:<e,t> (stateM (lambda $2:e (and:<t*,t> (barks:<e,t> $2) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (dog:<e,t> $3))  )  )  )   ) ())  $0))]" +
                        "[(lambda $4:e (and:<t*,t>  (thought:<e,t> $4)  (c_REL:<e,<e,t>> $4 (a:<id,<<e,t>,e>> na:id $1)   )  ) )]");
		Assert.assertTrue(secondary.getSemantics() instanceof Tower);
		System.out.println(primary.getSemantics());

		final DelimitRight<LogicalExpression> rule = new DelimitRight<>("Delim", TestServices.getTowerCategoryServices(), TestServices.getBaseRules());
		final List<IBinaryRecursiveParseRule<LogicalExpression>> validRules = new ArrayList<>(TestServices.getRecursiveRules());
		final List<ParseRuleResult<LogicalExpression>> actual = rule.applyRecursive(primary,
				secondary, null, validRules);
		System.out.println(actual);
		final TowerCategory<LogicalExpression> expected = (TowerCategory<LogicalExpression>) TestServices
				.getCategoryServices()
				.read("S//S\\\\S : [(lambda $0:M[<e,t>] (>>= $1:<e,t> (stateM (lambda $2:e (and:<t*,t> (thought:<e,t> $2) (c_REL:<e,<e,t>> $2 (a:<id,<<e,t>,e>> na:id (lambda $3:e (and:<t*,t> (barks:<e,t> $3) (c_REL:<e,<e,t>> $3 (a:<id,<<e,t>,e>> na:id (lambda $4:e (dog:<e,t> $4)))))))))) ()) $0))][(lambda $5:e (if:<<e,t>,<e,t>> (lambda $6:e ($1 $6)) $5))]");
		boolean hasExpected = false;
		for (ParseRuleResult<LogicalExpression> result : actual) {
			if (result.getResultCategory().equals(expected)) {
				hasExpected = true;
			}
		}
		Assert.assertTrue(hasExpected);
	}
}
