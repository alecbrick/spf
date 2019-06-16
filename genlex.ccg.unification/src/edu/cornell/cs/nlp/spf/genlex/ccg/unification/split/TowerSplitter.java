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
package edu.cornell.cs.nlp.spf.genlex.ccg.unification.split;

import edu.cornell.cs.nlp.spf.ccg.categories.*;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Tower splitting service object.
 * 
 * @author Alec Brickner
 */
public class TowerSplitter extends Splitter {

	public static final int MAX_HEIGHT = 3;
	protected final ITowerCategoryServices<LogicalExpression> towerCategoryServices;

	public TowerSplitter(
			ICategoryServices<LogicalExpression> categoryServices,
			ITowerCategoryServices<LogicalExpression> towerCategoryServices) {
		super(categoryServices);
	    this.towerCategoryServices = towerCategoryServices;
	}

	public List<TowerCategory<LogicalExpression>> unlower(Category<LogicalExpression> cat) {
		Set<SplittingPair> baseSplits = MakeApplicationSplits.of(cat, categoryServices);
		List<TowerCategory<LogicalExpression>> ret = new ArrayList<>();
		// For each possible split, create a new tower
		Set<Pair<Lambda, LogicalExpression>> semPairs = new HashSet<>();
		for (SplittingPair split : baseSplits) {
			Category<LogicalExpression> top = split.getLeft();
			// Must be a forward application
			if (!(top instanceof ComplexCategory) ||
					!((ComplexSyntax) top.getSyntax()).getRight().equals(
							split.getRight().getSyntax())) {
				continue;
			}
			if (!(top.getSemantics() instanceof Lambda)) {
				continue;
			}
			// Remove \x.x tower tops. (Idk if this works but it might be big for reducing splits)
			Lambda leftLambda = (Lambda) split.left.getSemantics();
			if (leftLambda.getArgument().equals(leftLambda.getBody())) {
				continue;
			}
			semPairs.add(Pair.of((Lambda) split.left.getSemantics(), split.right.getSemantics()));
		}
		// Only add semantic splits, because we only care about those
		for (Pair<Lambda, LogicalExpression> semPair : semPairs) {
			TowerSyntax newTowerSyntax = new TowerSyntax(Syntax.S, cat.getSyntax(), Syntax.S);
			Tower newTowerSemantics = new Tower(
					semPair.first(),
					semPair.second());
			TowerCategory<LogicalExpression> newTower = new TowerCategory<>(newTowerSyntax, newTowerSemantics);
			ret.add(newTower);
		}
		return ret;
	}

	public Set<SplittingPair> getSplits(Category<LogicalExpression> category) {
        final Set<SplittingPair> splits = new HashSet<SplittingPair>();
        // Split towers without lowering.
        splits.addAll(getRecursiveSplits(category));

        // Set limits on possible heights
        if ((category instanceof TowerCategory) &&
				((TowerCategory) category).height() >= MAX_HEIGHT) {
        	return splits;
		}

		// Reverse the Lower operation.
        List<TowerCategory<LogicalExpression>> newTowers = new ArrayList<>();
        // TODO: Figure out how to lower into a tower category
		// Unlower the base of a tower.
        if (category instanceof TowerCategory) {
        	// Since we're unlowering a tower, we can have multiple original tower tops and bottoms
        	TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) category;
        	Category<LogicalExpression> base = towerCategoryServices.getBase(tower);
        	for (TowerCategory<LogicalExpression> newBase : unlower(base)) {
        		TowerCategory<LogicalExpression> newTower =
						towerCategoryServices.replaceBase(tower, newBase);
        		newTowers.add(newTower);
			}
		} else {
        	newTowers = unlower(category);
		}

        for (TowerCategory<LogicalExpression> tower : newTowers) {
        	splits.addAll(getRecursiveSplits(tower));
		}
        return splits;
	}

	public Set<SplittingPair> getRecursiveSplits(Category<LogicalExpression> category) {
        final Set<SplittingPair> splits = new HashSet<>();
        if (!(category instanceof TowerCategory)) {
        	splits.addAll(super.getSplits(category));
		}
		splits.addAll(MakeTowerSplits.of(category, categoryServices,
				towerCategoryServices));
		return splits;
	}
	
	public static class Creator implements IResourceObjectCreator<TowerSplitter> {
		
		@SuppressWarnings("unchecked")
		@Override
		public TowerSplitter create(Parameters parameters,
                                    IResourceRepository resourceRepo) {
			return new TowerSplitter(
					resourceRepo.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE),
					resourceRepo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE));
		}
		
		@Override
		public String type() {
			return "splitter.unification.tower";
		}
		
		@Override
		public ResourceUsage usage() {
			return new ResourceUsage.Builder(type(), TowerSplitter.class)
					.setDescription(
							"Continuation tower splitter for unification-based GENLEX")
					.build();
		}
	}
}
