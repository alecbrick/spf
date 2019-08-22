package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ComplexCategory;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.ParseRuleResult;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax.N;

public class IndefiniteTest {
    List<IBinaryRecursiveParseRule<LogicalExpression>> recursiveRules;
    TowerRule<LogicalExpression> towerRule;

    public IndefiniteTest() {
        TestServices.init();
        this.recursiveRules = new ArrayList<>(TestServices.getRecursiveRules());
        this.towerRule = new TowerRule<>(TestServices.getTowerCategoryServices(), recursiveRules);
    }

    public List<ParseRuleResult<LogicalExpression>> combineWithLeft(
            List<ParseRuleResult<LogicalExpression>> lefts,
            Category<LogicalExpression> right) {
        List<ParseRuleResult<LogicalExpression>> ret = new ArrayList<>();
        for (ParseRuleResult<LogicalExpression> left : lefts) {
            ret.addAll(towerRule.applyRecursive(left.getResultCategory(), right, null, null));
        }
        return ret;
    }

    // If one of my relatives dies, I'll inherit a fortune.
    @Test
    public void test() {
        LogicalExpressionCategoryServices categoryServices = TestServices.getCategoryServices();
        Category<LogicalExpression> ifCat = Category.create(
                Syntax.read("S/S/<A>"),
                LogicalExpression.read(
                        "(lambda $0:e (lambda $1:<e,t> (lambda $2:e (and:<t*,t> " +
                                "($1 $2) " +
                                "(cond:<e,<e,t>> $2 $0)))))")
        );

        // i'm so sorry
        Lambda towerTop = (Lambda) LogicalExpression.read(
            "(lambda $0:t (lambda $1:<e,M[t]> (>>= $2:e (stateM (a:<id,<<e,t>,e>> !0  (lambda $3:e $0)) {!0})   ($1 $2))))");//[  (lambda $5:<e,t>  ($5 $3)  )    ]");
        Variable scopedVariable = ((Lambda) ((Literal) ((StateMonad) ((Binding) ((Lambda) towerTop.getBody()).getBody()).getLeft()).getBody()).getArg(1)).getArgument();
        LogicalExpressionReader.setLambdaWrapped(false);
        Variable baseArg = (Variable) Variable.read("$0:<e,t>");
        LogicalExpression[] args = {
                scopedVariable
        };
        LogicalExpression towerBase = new Lambda(baseArg, new Literal(baseArg, args));

        Category<LogicalExpression> indef = Category.create(
                new TowerSyntax(new ComplexSyntax(Syntax.S, Syntax.N, Slash.FORWARD), new TowerSyntax(Syntax.NP, Syntax.S, Syntax.S), Syntax.S),
                new Tower(towerTop, towerBase)
        );

        Category<LogicalExpression> relative = Category.create(
                Syntax.read("N"),
                LogicalExpression.read(
                        "(lambda $0:e (relative:<e,t> $0))"
                )
        );
        List<ParseRuleResult<LogicalExpression>> results = towerRule.applyRecursive(indef, relative, null, null);
        System.out.println("One of my relatives");
        System.out.println(results);

        Category<LogicalExpression> die = categoryServices.read(
                "S\\NP : (lambda $0:e (lambda $1:e (and:<t*,t> (die:<e,t> $1) (ARG0:<e,<e,t>> $1 $0))))"
        );
        System.out.println(die);
        results = combineWithLeft(results, die);
        System.out.println(results);


    }
}
