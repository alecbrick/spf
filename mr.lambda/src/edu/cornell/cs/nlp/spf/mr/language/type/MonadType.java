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

public class MonadType extends Type {
	public static final char	MONAD_TYPE_CLOSE_PAREN		= ']';
	public static final String	MONAD_TYPE_CLOSE_PAREN_STR		= String.valueOf(MONAD_TYPE_CLOSE_PAREN);
	public static final String	MONAD_TYPE_OPEN_PAREN			= "M[";
	private static final long	serialVersionUID				= -4179088110249120938L;

	private final Type			domain;

	MonadType(String label, Type domain) {
		super(label);
		assert domain != null;
		this.domain = domain;
	}

	public static String composeString(Type domain) {
		return new StringBuilder(20).append(MONAD_TYPE_OPEN_PAREN)
				.append(domain.toString())
				.append(MONAD_TYPE_CLOSE_PAREN).toString();
	}

	public static MonadType create(String label, Type domain) {
        return new MonadType(label, domain);
	}

	@Override
	public Type getDomain() {
		return domain;
	}

	@Override
	public Type getRange() {
		return null;
	}

	@Override
	public boolean isArray() {
		return false;
	}

	@Override
	public boolean isComplex() {
		return false;
	}

	@Override
	public boolean isExtending(Type other) {
		return other != null
				&& (other == this || domain.isExtending(other.getDomain()));
	}

	@Override
	public boolean isExtendingOrExtendedBy(Type other) {
		return isExtending(other) || other.isExtending(this);
	}

	public boolean isOrderSensitive() {
		return false;
	}

	@Override
	public String toString() {
		return getName();
	}

}
