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
package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;

import java.io.Serializable;
import java.util.Set;

/**
 * Category services, such as composition and application.
 *
 * @author Yoav Artzi
 * @param <MR>
 *            Semantic type
 */
public interface ITowerCategoryServices<MR> extends Serializable {

    // These get the very bottom of the input tower, which is different than its "base" category.
	Category<MR> getBottom(Category<MR> cat);
	Category<MR> setBottom(Category<MR> cat, Category<MR> newBase);

	MR getTopSemantics(TowerCategory<MR> tower);

	MR getBottomSemantics(TowerCategory<MR> tower);

	TowerCategory<MR> combineTowersWithBase(
			TowerCategory<MR> leftTower,
			TowerCategory<MR> rightTower,
			Category<MR> newBase);

	TowerCategory<MR> lift(Category<MR> toRaise);

	Category<MR> lower(TowerCategory<MR> toLower);

	Set<TowerCategory<MR>> unlower(Category<MR> toUnlower);

	TowerCategory<MR> replaceBase(
			TowerCategory<MR> towerForTop,
			Category<MR> base);

	@Override
	boolean equals(Object obj);

	@Override
	int hashCode();

	boolean canCombineTops(TowerCategory<MR> leftTower, TowerCategory<MR> rightTower);

	TowerCategory<MR> monadicLift(Category<MR> toLift, Syntax patternSyntax);

	// If tower, true if base is true
	// If complex, true if argument is monadic
	// Otherwise, false
	boolean hasMonadicBaseArg(Category<MR> cat);

	MR towerToLambda(MR tower);

	MR lambdaToTower(MR lambda);

}
