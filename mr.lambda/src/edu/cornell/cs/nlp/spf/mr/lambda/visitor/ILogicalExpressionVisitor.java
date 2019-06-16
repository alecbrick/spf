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
package edu.cornell.cs.nlp.spf.mr.lambda.visitor;

import edu.cornell.cs.nlp.spf.mr.IMeaningRepresentationVisitor;
import edu.cornell.cs.nlp.spf.mr.lambda.*;

public interface ILogicalExpressionVisitor extends
		IMeaningRepresentationVisitor {

	void visit(Lambda lambda);

	void visit(Literal literal);

	void visit(LogicalConstant logicalConstant);

	default void visit(Tower tower) {
		tower.getTop().accept(this);
		tower.getBottom().accept(this);
	}

    default void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}

	void visit(Variable variable);

	default void visit(StateMonad m) {
		m.getBody().accept(this);
	}

	default void visit(Binding binding) {
		binding.getLeft().accept(this);
		binding.getRight().accept(this);
	}
}
