package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import org.junit.Assert;
import org.junit.Test;

public class TowerCategoryServicesTest {

    public TowerCategoryServicesTest() {
        TestServices.init();
    }

    @Test
    public void getTopSemanticsTest() {
        TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:<<e,t>,e> $0)][(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))]");

        ComplexCategory<LogicalExpression> expectedTop = (ComplexCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S/S : (lambda $0:<<e,t>,e> $0)");

        Lambda expected = (Lambda) expectedTop.getSemantics();
        Lambda actual = (Lambda) TestServices
                .getTowerCategoryServices()
                .getTopSemantics(tower);

        Assert.assertTrue(actual.equals(expected));
    }

    @Test
    public void getBottomSemanticsTest() {
        TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:<<e,t>,e> $0)][(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))]");

        ComplexCategory<LogicalExpression> expectedBottom = (ComplexCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("N/N : (lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))");

        Lambda expected = (Lambda) expectedBottom.getSemantics();
        Lambda actual = (Lambda) TestServices
                .getTowerCategoryServices()
                .getBottomSemantics(tower);

        Assert.assertTrue(actual.equals(expected));
    }

    @Test
    public void combineTowersWithBaseTest() {
        final TowerCategory<LogicalExpression> left = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(NP/N)\\\\S : [(lambda $0:<<e,t>,e> $0][(lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))]");

        final TowerCategory<LogicalExpression> right = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:<<e,t>,e> $0)][(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))]");

        final ComplexCategory<LogicalExpression> base = (ComplexCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("NP/N : (lambda $1:<e,t> (the:<<e,t>,e> (lambda $2:e (and:<t*,t> (loc:<lo,<lo,t>> $2 alaska:s) ($1 $2)))))");

        final TowerCategory<LogicalExpression> expected =(TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(NP/N)\\\\S : [(lambda $0:<<e,t>,e> $0)][(lambda $1:<e,t> (the:<<e,t>,e> (lambda $2:e (and:<t*,t> (loc:<lo,<lo,t>> $2 alaska:s) ($1 $2)))))]");

        final TowerCategory<LogicalExpression> actual = TestServices
                .getTowerCategoryServices()
                .combineTowersWithBase(left, right, base);

        Assert.assertTrue(actual.equals(expected));
    }

    @Test
    public void replaceBaseTest() {
        final TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(N/N)\\\\S : [(lambda $0:<<e,t>,e> $0)][(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))]");

        final ComplexCategory<LogicalExpression> base = (ComplexCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("NP/N : (lambda $1:<e,t> (the:<<e,t>,e> (lambda $2:e (and:<t*,t> (loc:<lo,<lo,t>> $2 alaska:s) ($1 $2)))))");

        final TowerCategory<LogicalExpression> expected = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(NP/N)\\\\S : [(lambda $0:<<e,t>,e> $0)][(lambda $1:<e,t> (the:<<e,t>,e> (lambda $2:e (and:<t*,t> (loc:<lo,<lo,t>> $2 alaska:s) ($1 $2)))))]");

        final TowerCategory<LogicalExpression> actual = TestServices
                .getTowerCategoryServices()
                .replaceBase(tower, base);

        Assert.assertTrue(actual.equals(expected));
    }

    @Test
    public void raiseTest() {
        final ComplexCategory<LogicalExpression> toRaise = (ComplexCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("NP/N : (lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))");

        final TowerCategory<LogicalExpression> expected = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(NP/N)\\\\S : [(lambda $0:<<e,t>,e> $0][(lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))]");

        final TowerCategory<LogicalExpression> actual = TestServices
                .getTowerCategoryServices()
                .raise(toRaise);

        Assert.assertTrue(actual.equals(expected));
    }

    @Test
    public void lowerTest() {

    }
}
