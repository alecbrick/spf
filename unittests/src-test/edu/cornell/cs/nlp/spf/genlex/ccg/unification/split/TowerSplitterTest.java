package edu.cornell.cs.nlp.spf.genlex.ccg.unification.split;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.TowerRule;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class TowerSplitterTest {
    public void testSplits(Category<LogicalExpression> category, boolean debug) {


        TowerSplitter splitter = new TowerSplitter(TestServices.getCategoryServices(), TestServices.getTowerCategoryServices());
        Set<SplittingServices.SplittingPair> actual = splitter.getSplits(category);

        TowerRule<LogicalExpression> towerRule = new TowerRule<>(
                TestServices.getTowerCategoryServices(),
                TestServices.getRecursiveRules()
        );
        int i = 1;
        for (SplittingServices.SplittingPair pair : actual) {
            Category<LogicalExpression> left = pair.getLeft();
            Category<LogicalExpression> right = pair.getRight();
            if (debug) {
                System.out.println("Pair " + i++);
                System.out.println("Left: " + left);
                System.out.println("Right: " + right);
            }
            List<ParseRuleResult<LogicalExpression>> results =
                    towerRule.applyRecursive(left, right, null, TestServices.getRecursiveRules());
            // Tower rule doesn't apply to base categories
            if (!(left instanceof TowerCategory || right instanceof TowerCategory)) {
                continue;
            }
            boolean found = false;
            for (ParseRuleResult<LogicalExpression> result : results) {
                if (result.getResultCategory().equals(category)) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }
    }

    @Test
    public void getSplitsTest1() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices().read(
                        "N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");
        testSplits(category, true);
    }

    @Test
    public void getSplitsTest2() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("NP/N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))");
        testSplits(category, false);
    }

    @Test
    public void getSplitsTest3() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:<<e,t>,e> $0][(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))]");
        testSplits(category, false);
    }

    @Test
    public void getMonadicSplitsTest1() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("NP/N : (lambda $0:<e,t> (stateM (the:<<e,t>,e> (lambda $1:e ($0 $1)) ) ()))");
        testSplits(category, true);
    }

    @Test
    public void getMonadicSplitsTest2() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("S/NP : (lambda $0:<e,t> (stateM (the:<<e,t>,e> (lambda $1:e ($0 $1)) ) ()))");
        testSplits(category, true);
    }

    @Test
    public void getMonadicSplitsTest3() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:M[<<e,t>,e>] $0][(lambda $0:<e,t> (stateM (the:<<e,t>,e> (lambda $1:e ($0 $1)) ) ()))]");
        testSplits(category, true);
    }

}
