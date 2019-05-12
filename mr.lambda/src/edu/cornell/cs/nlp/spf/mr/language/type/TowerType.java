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
package edu.cornell.cs.nlp.spf.mr.language.type;

import edu.cornell.cs.nlp.spf.mr.lambda.LogicLanguageServices;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * A language entity type.
 *
 * @author Yoav Artzi
 */
public class TowerType extends Type {
	public static final char	TOWER_TYPE_CLOSE_PAREN		= ']';
	public static final String	TOWER_TYPE_CLOSE_PAREN_STR	= String.valueOf(TOWER_TYPE_CLOSE_PAREN);
	public static final char	TOWER_TYPE_OPEN_PAREN			= '[';
	public static final String	TOWER_TYPE_OPEN_PAREN_STR		= String.valueOf(TOWER_TYPE_OPEN_PAREN);

	private final Type top;
	private final Type bottom;

	TowerType(String label, Type top, Type bottom) {
		super(label);
		this.top = top;
		this.bottom = bottom;
	}

    public static String composeString(Type top, Type bottom) {
		return new StringBuilder(20).append(TOWER_TYPE_OPEN_PAREN)
				.append(top.toString())
				.append(TOWER_TYPE_CLOSE_PAREN)
				.append(TOWER_TYPE_OPEN_PAREN)
				.append(bottom.toString())
				.append(TOWER_TYPE_CLOSE_PAREN).toString();
	}

	public Type getDomain() {
		return null;
	}

	public Type getRange() {
		return null;
	}

	/**
	 * Returns true iff the type is an array.
	 *
	 * @return
	 */
	public boolean isArray() {
		return false;
	}

	/**
	 * Return true iff the object is a complex function type.
	 *
	 * @return
	 */
	public boolean isComplex() {
		return false;
	}

	public Type getTop() {
		return top;
	}

	public Type getBottom() {
		return bottom;
	}

	/**
	 * Is current type a child of another.
	 *
	 * @param other
	 */
	public boolean isExtending(Type other) {
		if (other == null) {
			return false;
		}

		if (this.equals(other)) {
			return true;
		} else if (other instanceof TowerType) {
			TowerType otherTower = (TowerType) other;
			return top.isExtending(otherTower.getTop()) &&
					bottom.isExtending(otherTower.getBottom());
		}
		return false;
	}

	/**
	 * Return 'true' iff the given type and this type share a path on the
	 * hierarchical tree.
	 *
	 * @param other
	 */
	public boolean isExtendingOrExtendedBy(Type other) {
		return this.isExtending(other) || other.isExtending(this);
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
