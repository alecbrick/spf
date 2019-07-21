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
package edu.cornell.cs.nlp.spf.parser.ccg.cky;

import edu.cornell.cs.nlp.spf.parser.ccg.cky.chart.Cell;
import edu.cornell.cs.nlp.spf.parser.ccg.normalform.NormalFormValidator;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;

import java.util.List;

public class CKYRecursiveBinaryParsingRule<MR> extends CKYBinaryParsingRule<MR> {
	private static final long			serialVersionUID	= -5629394704296771855L;
	private final NormalFormValidator	nfValidator;
	private final IBinaryRecursiveParseRule<MR> rule;

	public CKYRecursiveBinaryParsingRule(IBinaryRecursiveParseRule<MR> rule) {
		this(rule, null);
	}

	public CKYRecursiveBinaryParsingRule(IBinaryRecursiveParseRule<MR> rule,
                                         NormalFormValidator nfValidator) {
		super(rule, nfValidator);
		this.rule = rule;
		this.nfValidator = nfValidator;
	}

	@Override
	public String toString() {
		return String.format("%s[%s]",
				CKYRecursiveBinaryParsingRule.class.getSimpleName(), rule);
	}

	/**
	 * Takes two cell, left and right, as input. Assumes these cells are
	 * adjacent.
	 */
	protected List<ParseRuleResult<MR>> apply(Cell<MR> left, Cell<MR> right,
											  SentenceSpan span) {
		assert left.getEnd() + 1 == right.getStart();
		if (nfValidator != null
				&& !nfValidator.isValid(left, right, rule.getName())) {
			return null;
		}
		return rule.applyRecursive(left.getCategory(), right.getCategory(),
				span, null);
	}
}
