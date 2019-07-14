package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class ReversibleTowerRuleTest {
    public void testSplits(Category<LogicalExpression> left, Category<LogicalExpression> result, boolean debug) {

        ForwardReversibleApplication forwardApp =
                (ForwardReversibleApplication) TestServices.getBaseReversibleRules().get(0);
        ReversibleTowerRule reversibleTowerRule = new ReversibleTowerRule(
               TestServices.getTowerCategoryServices(),
                TestServices.getRecursiveRules(),
                TestServices.getCategoryServices(),
                TestServices.getMonadServices(),
                TestServices.getBaseReversibleRules(),
                forwardApp);
        Set<Category<LogicalExpression>> rights = reversibleTowerRule.reverseApplyLeft(
                left, result, null
        );

        int i = 1;
        for (Category<LogicalExpression> right : rights) {
            if (debug) {
                System.out.println("Pair " + i++);
                System.out.println("Left: " + left);
                System.out.println("Right: " + right);
            }
            List<ParseRuleResult<LogicalExpression>> results =
                    reversibleTowerRule.applyRecursive(left, right, null, TestServices.getRecursiveRules());
            // Tower rule doesn't apply to base categories
            if (!(left instanceof TowerCategory || right instanceof TowerCategory)) {
                continue;
            }
            boolean found = false;
            for (ParseRuleResult<LogicalExpression> res : results) {
                if (res.getResultCategory().equals(result)) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
        }
    }

    @Test
    public void getSplitsTest1() {
        final Category<LogicalExpression> left = TestServices
                .getCategoryServices().read(
                        "(N/N)//NP\\\\(S|NP) : [(lambda $0:<e,t> (lambda $1:<e,t> (lambda $2:e (and:<t*,t> ($0 $2) ($1 $2)))))][alaska:s]");
        final Category<LogicalExpression> result = TestServices
                .getCategoryServices().read(
                        "N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");
        testSplits(left, result, true);
    }

    @Test
    public void getSplitsTest2() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("NP/N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))");
        //testSplits(category, false);
    }

    @Test
    public void getSplitsTest3() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:<<e,t>,e> $0][(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))]");
        //testSplits(category, false);
    }

    @Test
    public void getMonadicSplitsTest1() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("NP/N : (lambda $0:<e,t> (stateM (the:<<e,t>,e> (lambda $1:e ($0 $1)) ) ()))");
        //testSplits(category, true);
    }

    @Test
    public void getMonadicSplitsTest2() {
        final Category<LogicalExpression> left = TestServices
                .getCategoryServices()
                .read("S|NP|(S|NP) : (lambda $0:<e,t> (lambda $1:e ($0 $1)))");
        final Category<LogicalExpression> result = TestServices
                .getCategoryServices()
                .read("S/(S/NP) : (lambda $0:<e,t> (stateM (the:<<e,t>,e> (lambda $1:e ($0 $1)) ) ()))");
        testSplits(left, result, true);
    }

    @Test
    public void getMonadicSplitsTest3() {
        final Category<LogicalExpression> left = TestServices
                .getCategoryServices()
                .read("S//NP\\\\S : [(lambda $0:M[t] (>>= $1:t (stateM (barks:<e,t> (the:<<e,t>,e> (lambda $2:e (dog:<e,t> $2)))) ()) $0))][i:e]");
        final Category<LogicalExpression> result = TestServices
                .getCategoryServices()
                .read("S : (>>= $0:t (stateM (barks:<e,t> (the:<<e,t>,e> dog:<e,t>)) ()) (stateM (thought:<e,<t,t>> i:e $0) () ))");
        testSplits(left, result, true);
    }

    @Test
    public void getMonadicSplitsTest4() {
        final Category<LogicalExpression> category = TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:M[<<e,t>,e>] $0][(lambda $0:<e,t> (stateM (the:<<e,t>,e> (lambda $1:e ($0 $1)) ) ()))]");
        //testSplits(category, true);
    }
}
