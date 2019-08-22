package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import edu.cornell.cs.nlp.spf.mr.lambda.*;

import java.util.HashMap;
import java.util.Map;

public class GetBindingMapping implements ILogicalExpressionVisitor {
    private LogicalExpression other;
    private Map<Variable, Variable> mapping;
    private boolean expFound = false;

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
        if (lambda.equals(otherLambda)) {
            expFound = true;
        }
        if (expFound) {
            other = otherLambda.getBody();
            lambda.getBody().accept(this);
            other = otherLambda;
        } else {
            lambda.getBody().accept(this);
        }
    }

    @Override
    public void visit(Literal literal) {
        Literal otherLiteral = (Literal) other;
        if (literal.equals(otherLiteral)) {
            expFound = true;
        }
        if (expFound) {
            other = otherLiteral.getPredicate();
            literal.getPredicate().accept(this);
            for (int i = 0; i < literal.numArgs(); i++) {
                other = otherLiteral.getArg(i);
                literal.getArg(i).accept(this);
            }
            other = otherLiteral;
        } else {
            literal.getPredicate().accept(this);
            for (int i = 0; i < literal.numArgs(); i++) {
                literal.getArg(i).accept(this);
            }
        }
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
        if (binding.equals(otherBinding)) {
            expFound = true;
        }
        if (expFound) {
            mapping.put(binding.getVariable(), otherBinding.getVariable());
            other = otherBinding.getLeft();
            binding.getLeft().accept(this);
            other = otherBinding.getRight();
            binding.getRight().accept(this);
            other = otherBinding;
        } else {
            binding.getLeft().accept(this);
            binding.getRight().accept(this);
        }
    }

    @Override
    public void visit(StateMonad stateM) {
        // nothing to do
    }
}
