package edu.cornell.cs.nlp.spf.ccg.categories.syntax;

import org.junit.Assert;
import org.junit.Test;

public class RecursiveSyntaxTest {
    @Test
    public void test() {
        final Syntax s = Syntax.read("S//S\\\\S");
        final TowerSyntax s1 = new TowerSyntax(Syntax.S, Syntax.S, Syntax.S);
        Assert.assertTrue(s.equals(s1));
    }

    @Test
    public void test2() {
        final Syntax s = Syntax.read("S//NP\\\\S");
        final TowerSyntax s1 = new TowerSyntax(Syntax.NP, Syntax.S, Syntax.S);
        Assert.assertTrue(s.equals(s1));
    }

    @Test
    public void test3() {
        final Syntax s = Syntax.read("S//(S//NP\\\\S)\\\\S");
        final TowerSyntax s1 = new TowerSyntax(Syntax.NP, Syntax.S, Syntax.S);
        final TowerSyntax s2 = new TowerSyntax(s1, Syntax.S, Syntax.S);
        Assert.assertTrue(s.equals(s2));
        Assert.assertFalse(s.equals(s1));
    }

    @Test
    public void test4() {
        final Syntax s = Syntax.read("(S//NP\\\\S)//(S//NP\\\\S)\\\\(S//NP\\\\S)");
        final TowerSyntax s1 = new TowerSyntax(Syntax.NP, Syntax.S, Syntax.S);
        final TowerSyntax s2 = new TowerSyntax(s1, s1, s1);
        Assert.assertTrue(s.equals(s2));
        Assert.assertFalse(s.equals(s1));
    }
}
