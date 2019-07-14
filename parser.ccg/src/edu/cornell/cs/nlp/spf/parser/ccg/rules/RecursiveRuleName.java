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
package edu.cornell.cs.nlp.spf.parser.ccg.rules;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.*;

/**
 * Structured rule name. Indicates the direction of the rule, its label and
 * order.
 *
 * @author Yoav Artzi
 */
public class RecursiveRuleName extends RuleName {

	public static String		RULE_ADD			= "+";
	private static final long	serialVersionUID	= -8734352006518878281L;
	protected RuleName 			child;

	protected RecursiveRuleName(String label, RuleName child) {
		super(label, child.getDirection(), child.getOrder());
		this.child = child;
	}

	public static RecursiveRuleName create(String label, RuleName child) {
		return new RecursiveRuleName(label, child);
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
		final RecursiveRuleName other = (RecursiveRuleName) obj;
        if (!getLabel().equals(other.getLabel())) {
			return false;
		}
        return child.equals(other.child);
	}

	@Override
	public String toString() {
		String label = getLabel();
		return (label + "," + child.toString());
	}

	public RuleName getChild() {
		return child;
	}
}
