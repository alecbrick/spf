package edu.cornell.cs.nlp.spf.mr.lambda;

import edu.cornell.cs.nlp.spf.TestServices;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ApplyAndSimplify;
import org.junit.Assert;
import org.junit.Test;

public class MonadTest {

    @Test
    public void test1() {
        String monadString = "(stateM (lambda $0:e $0) (!1))";
        Monad m = (Monad) TestServices.getCategoryServices().readSemantics(monadString, false);
        Assert.assertEquals(monadString, m.toString());
    }

    @Test
    public void test2() {
        String monadString = "(>>= $0:e (stateM (lambda $1:e $1) (!1)) (stateM (lambda $2:<e,e> ($2 $0)) ()))";
        Monad m = (Monad) TestServices.getCategoryServices().readSemantics(monadString, false);
        Assert.assertEquals(monadString, m.toString());
    }

    @Test
    public void test3() {
        String monadString = "(stateM (lambda $0:e $0) (!1))";
        StateMonad m = (StateMonad) TestServices.getCategoryServices().readSemantics(monadString, false);
        MonadParams inputParams = new StateMonadParams();
        MonadParams outputParams = m.exec(inputParams);
        System.out.println(outputParams.getOutput());
    }

    @Test
    public void test4() {
        String monadString = "(>>= $1:<e,e> (stateM (lambda $1:e $1) (!1)) (stateM (lambda $2:e ($1 $2)) ()))";
        Binding m = (Binding) TestServices.getCategoryServices().readSemantics(monadString, false);
        MonadParams inputParams = new StateMonadParams();
        MonadParams outputParams = m.exec(inputParams);
        System.out.println(outputParams.getOutput());
    }

    @Test
    public void test5() {
        String lambdaString = "(lambda $0:M[<e,e>] (>>= $1:<e,e> $0 (stateM (lambda $2:e ($1 $2)) ())))";
        Lambda lambda = (Lambda) TestServices.getCategoryServices().readSemantics(lambdaString, false);
        String monadString = "(stateM (lambda $0:e $0) (!1))";
        StateMonad monad = (StateMonad) TestServices.getCategoryServices().readSemantics(monadString, false);
        Binding output = (Binding) ApplyAndSimplify.of(lambda, monad);
        String bindingString = "(>>= $1:<e,e> (stateM (lambda $1:e $1) (!1)) (stateM (lambda $2:e ($1 $2)) ()))";
        Binding correct = (Binding) TestServices.getCategoryServices().readSemantics(bindingString, false);
        Assert.assertTrue(output.equals(correct));
    }

    @Test
    public void test6() {
        String towerString = "[(lambda $0:M[<e,e>] (>>= $1:<e,e> (stateM (lambda $2:e $2) (!1)) $0)][$1]";
        Tower tower = (Tower) TestServices.getCategoryServices().readSemantics(towerString, false);
        System.out.println(tower);
    }
}
