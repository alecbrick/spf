package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpressionReader;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
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
                .lift(toRaise);

        Assert.assertTrue(actual.equals(expected));
    }

    @Test
    public void lowerTest1() {
        LogicalExpressionReader.setLambdaWrapped(false);
        final TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("NP//S\\\\S : [(lambda $0:M[<<<e,t>,e>,<<e,t>,e>>] (>>= $1:<<<e,t>,e>,<<e,t>,e>> (stateM (lambda $2:<<e,t>,e> $2) ()) $0)][$1]");
        final Category lowered = TestServices.getTowerCategoryServices()
                .lower(tower);
        final Category expected = TestServices.getCategoryServices().read(
                "NP : (>>= $0:<<<e,t>,e>,<<e,t>,e>> (stateM (lambda $1:<<e,t>,e> $1) ()) (stateM $0 ()))"
        );
        Assert.assertEquals(expected, lowered);
    }

    @Test
    public void lowerTest2() {
        LogicalExpressionReader.setLambdaWrapped(false);
        final TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(S/NP)\\\\S : [(lambda $0:M[<<e,t>,e>] (>>= $1:<<<e,t>,e>,<<e,t>,e>> (stateM (lambda $2:<<e,t>,e> $2) ()) $0)][(lambda $2:<<e,t>,e> ($1 $2)]");
        final Category lowered = TestServices.getTowerCategoryServices()
                .lower(tower);
        System.out.println(lowered);
        final Category expected = TestServices.getCategoryServices().read(
                "S/NP : (lambda $0:<<e,t>,e> (>>= $1:<<<e,t>,e>,<<e,t>,e>> (stateM (lambda $2:<<e,t>,e> $2) ()) (stateM ($1 $0) ())))"
        );
        Assert.assertEquals(expected, lowered);
    }

    @Test
    public void lowerTest3() {
        LogicalExpressionReader.setLambdaWrapped(false);
        final TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//(NP//S\\\\S)\\\\S : [(lambda $0:M[<e,e>] (>>= $1:<e,e> (stateM (lambda $2:e $2) ()) $0)][[(lambda $3:M[<e,e>] (>>= $4:<e,e> (stateM (lambda $5:e $5) ()) $3)][$4]]");
        final Category lowered = TestServices.getTowerCategoryServices()
                .lower(tower);
        System.out.println(lowered);
        final Category expected = TestServices.getCategoryServices().read(
                "NP : (>>= $0:<e,e> (stateM (lambda $1:e $1) ()) (>>= $2:<e,e> (stateM (lambda $3:e $3) ()) (stateM $2 ())))"
        );
        Assert.assertEquals(expected, lowered);
    }

    @Test
    public void lowerTest4() {
        LogicalExpressionReader.setLambdaWrapped(false);
        final TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) TestServices
                .getCategoryServices()
                .read("S//((S//NP\\\\S)//S\\\\S)\\\\S : [(lambda $0:M[<e,t>] $0)]" +
                        "[[(lambda $1:M[<e,t>] (lambda $2:<<e,t>,M[<e,t>]> (>>=  $3:<e,t> (stateM $1 ()) ($2 $3))))]" +
                        "[(lambda $4:e (cool:<e,t> $4))]]");
        final Category lowered = TestServices.getTowerCategoryServices()
                .lower(tower);
        final Category expected = TestServices.getCategoryServices().read(
                "S//NP\\\\S : [(lambda $0:M[<e,t>] (>>= $1:<e,t> (stateM (lambda $2:e (cool:<e,t> $2)) ()) $0))][$1]"
        );
        Assert.assertEquals(expected, lowered);
    }

    @Test
    public void towerToLambdaSimpleTest() {
        LogicalExpressionReader.setLambdaWrapped(false);
        final Tower tower = (Tower) TestServices.getCategoryServices()
                .readSemantics("[(lambda $0:<<e,t>,e> (lambda $1:<<<e,t>,e>,t> ($1 $0)))][(lambda $0:<e,t> (the:<<e,t>,e> (lambda $1:e ($0 $1))))]");
        final Lambda actual = TestServices.getTowerCategoryServices()
                .towerToLambda(tower);
        final Lambda expected = (Lambda) TestServices.getCategoryServices()
                .readSemantics("(lambda $0:<<<e,t>,e>,<<e,t>,e>> (lambda $1:<<<e,t>,e>,t> ($1 ($0 (lambda $2:<e,t> (the:<<e,t>,e> (lambda $3:e ($2 $3))))))))");
        Assert.assertEquals(expected, actual);
        final Tower result = TestServices.getTowerCategoryServices()
                .lambdaToTower(actual);
        Assert.assertEquals(tower, result);
    }

    @Test
    public void towerToLambdaTest() {
        LogicalExpressionReader.setLambdaWrapped(false);
        final Tower tower = (Tower) TestServices.getCategoryServices()
                .readSemantics("[(lambda $0:<<e,t>,e> $0)][(lambda $0:<e,t> (lambda $1:e (and:<t*,t> (loc:<lo,<lo,t>> $1 alaska:s) ($0 $1))))]");
        final Lambda actual = TestServices.getTowerCategoryServices()
                .towerToLambda(tower);
        final Lambda expected = (Lambda) TestServices.getCategoryServices()
                .readSemantics("(lambda $0:<<<e,t>,<e,t>>,<<e,t>,e>> ($0 (lambda $1:<e,t> (lambda $2:e (and:<t*,t> (loc:<lo,<lo,t>> $2 alaska:s) ($1 $2))))))");
        Assert.assertEquals(actual, expected);
        final Tower result = TestServices.getTowerCategoryServices()
                .lambdaToTower(actual);
        Assert.assertEquals(tower, result);
    }

    @Test
    public void lambdaToTowerTest() {
        final Lambda lambda = (Lambda) TestServices.getCategoryServices()
                .readSemantics("(lambda $1:<e,t> (the:<<e,t>,e> (lambda $2:e (and:<t*,t> (loc:<lo,<lo,t>> $2 alaska:s) ($1 $2)))))", false);
        final Tower actual = TestServices.getTowerCategoryServices()
                .lambdaToTower(lambda);
        // TODO: Finish this if you have time to get scoping correct.
        // Tower bottoms can fall under the scope of tower tops.
        // Because of that, it's difficult to read tower tops and bottoms separately,
        // since the scopes aren't treated correctly.
        System.out.println(actual);
    }
}
