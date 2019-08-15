package edu.cornell.cs.nlp.spf.ccg.categories.syntax;

import java.util.Set;

public class DelimitSyntax extends Syntax {
    private static final char	OPEN_DELIM			    = '<';
    private static final String	OPEN_DELIM_STR			= String.valueOf(OPEN_DELIM);
    private static final char	CLOSE_DELIM			    = '>';
    private static final String	CLOSE_DELIM_STR			= String.valueOf(CLOSE_DELIM);

    // A delimiter just wraps syntax.
    protected Syntax wrappedSyntax;
    private final int hashCode;

    public DelimitSyntax(Syntax wrappedSyntax) {
        this.wrappedSyntax = wrappedSyntax;
        this.hashCode = calcHashCode();
    }

    public static DelimitSyntax read(String string) {
        String currentString = string.trim();
        if (!(currentString.startsWith(OPEN_DELIM_STR) &&
                currentString.endsWith((CLOSE_DELIM_STR)))) {
            throw new IllegalArgumentException("Missing delimiter brackets in "
                    + currentString);
        }
        String wrappedString = currentString.substring(1, currentString.length() - 1);
        return new DelimitSyntax(Syntax.read(wrappedString));
    }

    public Syntax getWrappedSyntax() {
        return wrappedSyntax;
    }

    @Override
    public boolean containsSubSyntax(Syntax other) {
        return wrappedSyntax.containsSubSyntax(other);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof DelimitSyntax)) {
            return false;
        }
        DelimitSyntax delimitOther = (DelimitSyntax) obj;
        return wrappedSyntax.equals(delimitOther.wrappedSyntax);
    }

    @Override
    public Set<String> getAttributes() {
        return wrappedSyntax.getAttributes();
    }

    @Override
    public boolean hasAttributeVariable() {
        return wrappedSyntax.hasAttributeVariable();
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (wrappedSyntax == null ? 0 : wrappedSyntax.hashCode());
		return result;
	}

    @Override
    public int numArguments() {
        return wrappedSyntax.numArguments();
    }

    @Override
    public int numSlashes() {
        return wrappedSyntax.numSlashes();
    }

    @Override
    public Syntax replace(Syntax current, Syntax replacement) {
        if (this.equals(current)) {
            return replacement;
        }

        return new DelimitSyntax(wrappedSyntax.replace(current, replacement));
    }

    @Override
    public Syntax replaceAttribute(String attribute, String replacement) {
        return new DelimitSyntax(wrappedSyntax.replaceAttribute(attribute, replacement));
    }

    @Override
    public Syntax setVariable(String assignment) {
        return new DelimitSyntax(wrappedSyntax.setVariable(assignment));
    }

    @Override
    public Syntax stripAttributes() {
        return new DelimitSyntax(wrappedSyntax.stripAttributes());
    }

    @Override
    public Syntax stripVariables() {
        return new DelimitSyntax(wrappedSyntax.stripVariables());
    }

    @Override
    public String toString() {
        final StringBuilder ret = new StringBuilder();
        ret.append("<");
        ret.append(wrappedSyntax);
        ret.append(">");
        return ret.toString();
    }

    @Override
    protected UnificationHelper unify(Syntax other, UnificationHelper helper) {
        if (other instanceof DelimitSyntax) {
            UnificationHelper resultHelper = wrappedSyntax.unify(
                    ((DelimitSyntax) other).wrappedSyntax, helper);
            if (resultHelper != null) {
                if (resultHelper.result == wrappedSyntax) {
                    resultHelper.result = this;
                    return resultHelper;
                } else {
                    resultHelper.result = new DelimitSyntax(
                            resultHelper.result);
                    return resultHelper;
                }
            }
        }
        return null;
    }
}
