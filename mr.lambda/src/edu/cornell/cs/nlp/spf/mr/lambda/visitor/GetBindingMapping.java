package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import edu.cornell.cs.nlp.spf.mr.lambda.*;

import java.util.HashMap;
import java.util.Map;

public class GetBindingMapping implements ILogicalExpressionVisitor {
    private LogicalExpression other;
    private Map<Variable, Variable> mapping;

    private GetBindingMapping(LogicalExpression other) {
        this.other = other;
        this.mapping = new HashMap<>();
    }

    public static Map<Variable, Variable> of(LogicalExpression exp, LogicalExpression other) {
        GetBindingMapping visitor = new GetBindingMapping(other);
        visitor.visit(exp);
        return visitor.mapping;
    }

    @Override
    public void visit(Lambda lambda) {
        Lambda otherLambda = (Lambda) other;
        other = otherLambda.getBody();
        lambda.getBody().accept(this);
        other = otherLambda;
    }

    @Override
    public void visit(Literal literal) {
        Literal otherLiteral = (Literal) other;
        other = otherLiteral.getPredicate();
        literal.getPredicate().accept(this);
        for (int i = 0; i < literal.numArgs(); i++) {
            other = otherLiteral.getArg(i);
            literal.getArg(i).accept(this);
        }
        other = otherLiteral;
    }

    @Override
    public void visit(LogicalConstant logicalConstant) {
        // nothing to do
    }

    @Override
    public void visit(Variable variable) {
        // nothing to do
    }

    @Override
    public void visit(Binding binding) {
        Binding otherBinding = (Binding) other;
        mapping.put(binding.getVariable(), otherBinding.getVariable());
        other = otherBinding.getLeft();
        binding.getLeft().accept(this);
        other = otherBinding.getRight();
        binding.getRight().accept(this);
        other = otherBinding;
    }

    @Override
    public void visit(StateMonad stateM) {
        // nothing to do
    }
}
