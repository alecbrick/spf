/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.ccg.categories.syntax;

import edu.cornell.cs.nlp.utils.collections.SetUtils;

import java.util.Set;

public class TowerSyntax extends Syntax {

	// Syntax constants
	private static final char	CLOSE_PAREN			= ')';

	private static final String	CLOSE_PAREN_STR		= String.valueOf(CLOSE_PAREN);

	private static final char	OPEN_PAREN			= '(';

	private static final String	OPEN_PAREN_STR		= String.valueOf(OPEN_PAREN);

	private static final long	serialVersionUID	= 2647447680294080606L;
	private final int			hashCode;
	private final Syntax		left;

	private final int			numSlashes;
	private final Syntax		right;
	private final Syntax		base;

	public TowerSyntax(Syntax base, Syntax left, Syntax right) {
		assert base != null : "Missing base component";
		assert left != null : "Missing left component";
		assert right != null : "Missing right component";
		this.base = base;
		this.left = left;
		this.right = right;
		this.numSlashes = left.numSlashes() + right.numSlashes() + base.numSlashes();
		this.hashCode = calcHashCode();
	}

	/**
	 * Reads {@link TowerSyntax} from {@link String}.
	 */
	public static TowerSyntax read(String string) {
		// Find the outermost slash assumes that one exists.
		int depth = 0;
		char c;
		String currentString = string.trim();
		if (currentString.startsWith(OPEN_PAREN_STR)
				&& currentString.endsWith(CLOSE_PAREN_STR)) {
			// check if we need to strip them
			boolean trim = true;
			depth = 0;
			for (int i = 0; i < currentString.length() - 1; i++) {
				c = currentString.charAt(i);
				if (c == OPEN_PAREN) {
					depth++;
				} else if (c == CLOSE_PAREN) {
					depth--;
				}
				if (depth == 0) {
					trim = false;
				}
			}
			if (trim) {
				currentString = currentString.substring(1,
						currentString.length() - 1);
			}
		}
		depth = 0;
		Slash latestSlash = null;
		int latestForwardSlashPosition = -1;
		int latestBackwardSlashPosition = -1;
		for (int i = 0; i < currentString.length() - 1; i++) {
			c = currentString.charAt(i);
			if (c == OPEN_PAREN) {
				depth++;
			}
			if (c == CLOSE_PAREN) {
				depth--;
			}
			if (depth == 0) {
				if (c == Slash.FORWARD.getChar() &&
						currentString.charAt(i + 1) == Slash.FORWARD.getChar()) {
					latestForwardSlashPosition = i;
				} else if (c == Slash.BACKWARD.getChar() &&
						currentString.charAt(i + 1) == Slash.BACKWARD.getChar()) {
					latestBackwardSlashPosition = i;
				}
			}
		}
		if (latestForwardSlashPosition == -1 ||
				latestBackwardSlashPosition == -1) {
			throw new IllegalArgumentException("Missing continuation slashes in "
					+ currentString);
		}

		// base, left, right
		return new TowerSyntax(
				Syntax.read(currentString.substring(latestForwardSlashPosition + 2, latestBackwardSlashPosition)),
				Syntax.read(currentString.substring(0, latestForwardSlashPosition)),
				Syntax.read(currentString.substring(latestBackwardSlashPosition + 2)));
	}

	@Override
	public boolean containsSubSyntax(Syntax other) {
		if (equals(other)) {
			return true;
		}

		if (getLeft().containsSubSyntax(other)) {
			return true;
		}

		if (getRight().containsSubSyntax(other)) {
			return true;
		}

		if (getLeft().containsSubSyntax(other)) {
			return true;
		}

		return false;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TowerSyntax other = (TowerSyntax) obj;
		if (hashCode != other.hashCode) {
			return false;
		}
		if (!base.equals(other.base)) {
			return false;
		}
		if (!left.equals(other.left)) {
			return false;
		}
		if (!right.equals(other.right)) {
			return false;
		}
		return true;
	}

	@Override
	public Set<String> getAttributes() {
		final Set<String> baseAttributes = base.getAttributes();
		final Set<String> leftAttributes = left.getAttributes();
		final Set<String> rightAttributes = right.getAttributes();
        return SetUtils.union(
        		SetUtils.union(leftAttributes, rightAttributes),
				baseAttributes);
	}

	public Syntax getBase() {
		return base;
	}

	public Syntax getLeft() {
		return left;
	}

	public Syntax getRight() {
		return right;
	}

	@Override
	public boolean hasAttributeVariable() {
		if (left.hasAttributeVariable()) {
			return true;
		} else if (right.hasAttributeVariable()) {
			return true;
		}
		return base.hasAttributeVariable();
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public int numArguments() {
		return base.numArguments(); // TODO: not sure about this one
	}

	@Override
	public int numSlashes() {
		return base.numSlashes(); // or this one
	}

	@Override
	public Syntax replace(Syntax current, Syntax replacement) {
		if (this.equals(current)) {
			return replacement;
		}

		final Syntax strippedBase = base.replace(current, replacement);
		final Syntax strippedLeft = left.replace(current, replacement);
		final Syntax strippedRight = right.replace(current, replacement);
		if (strippedBase == base && strippedLeft == left &&
				strippedRight == right) {
			return this;
		} else {
			return new TowerSyntax(strippedBase, strippedLeft, strippedRight);
		}
	}

	@Override
	public Syntax replaceAttribute(String attribute, String replacement) {
	    final Syntax setBase = base.replaceAttribute(attribute, replacement);
		final Syntax setLeft = left.replaceAttribute(attribute, replacement);
		final Syntax setRight = right.replaceAttribute(attribute, replacement);
		if (setBase == base && setLeft == left && setRight == right) {
			return this;
		} else if (setBase == null || setLeft == null || setRight == null) {
			return null;
		} else {
			return new TowerSyntax(setBase, setLeft, setRight);
		}
	}

	@Override
	public Syntax setVariable(String assignment) {
		final Syntax setBase = base.setVariable(assignment);
		final Syntax setLeft = left.setVariable(assignment);
		final Syntax setRight = right.setVariable(assignment);
		if (setBase == base && setLeft == left && setRight == right) {
			return this;
		} else if (setBase == null || setLeft == null || setRight == null) {
			return null;
		} else {
			return new TowerSyntax(setBase, setLeft, setRight);
		}
	}

	@Override
	public Syntax stripAttributes() {
		final Syntax strippedBase = base.stripAttributes();
		final Syntax strippedLeft = left.stripAttributes();
		final Syntax strippedRight = right.stripAttributes();
		if (strippedBase == base && strippedLeft == left &&
				strippedRight == right) {
			return this;
		} else {
			return new TowerSyntax(strippedBase, strippedLeft, strippedRight);
		}
	}

	@Override
	public Syntax stripVariables() {
		final Syntax strippedBase = base.stripVariables();
		final Syntax strippedLeft = left.stripVariables();
		final Syntax strippedRight = right.stripVariables();
		if (strippedBase == base && strippedLeft == left &&
				strippedRight == right) {
			return this;
		} else {
			return new TowerSyntax(strippedBase, strippedLeft, strippedRight);
		}
	}

	@Override
	public String toString() {
		final StringBuilder ret = new StringBuilder();
		if (left instanceof TowerSyntax || left instanceof ComplexSyntax) {
			ret.append("(").append(left).append(")");
		} else {
			ret.append(left);
		}
		ret.append("//");
		if (base instanceof TowerSyntax || base instanceof ComplexSyntax){
			ret.append("(").append(base).append(")");
		} else {
			ret.append(base);
		}
		ret.append("\\\\");
        if (right instanceof TowerSyntax || right instanceof ComplexSyntax){
			ret.append("(").append(right).append(")");
		} else {
			ret.append(right);
		}
		return ret.toString();
	}

	private int calcHashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (left == null ? 0 : left.hashCode());
		result = prime * result + (right == null ? 0 : right.hashCode());
		result = prime * result + (base == null ? 0 : base.hashCode());
		return result;
	}

	@Override
	protected UnificationHelper unify(Syntax other, UnificationHelper helper) {
		if (!(other instanceof TowerSyntax)) {
			return null;
		}
        final UnificationHelper rightHelper = right.unify(
                ((TowerSyntax) other).right, helper);
        if (rightHelper == null) {
			return null;
		}
        final Syntax rightUnification = rightHelper.result;
        final UnificationHelper leftHelper = left.unify(
                ((TowerSyntax) other).left, rightHelper);
        if (leftHelper == null) {
			return null;
		}
        final Syntax leftUnification = leftHelper.result;
        final UnificationHelper baseHelper = base.unify(
				((TowerSyntax) other).base, leftHelper);
        final Syntax baseUnification = baseHelper.result;
        if (baseUnification == base && leftUnification == left &&
				rightUnification == right) {
            baseHelper.result = this;
        } else {
            baseHelper.result = new TowerSyntax(
                    baseUnification, leftUnification, rightUnification);
        }
        return baseHelper;
	}

	public int height() {
		if (base instanceof TowerSyntax) {
			return ((TowerSyntax) base).height() + 1;
		}
		return 2;
	}
}
