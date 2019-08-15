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
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.*;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.*;

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

	private List<TowerCategory<LogicalExpression>> towerMonadicUnlower(TowerCategory towerCategory) {
		List<TowerCategory<LogicalExpression>> ret = new ArrayList<>();
		Tower tower = (Tower) towerCategory.getSemantics();
		TowerSyntax towerSyntax = towerCategory.getSyntax();
		Lambda top = tower.getTop();
		ComplexSyntax topSyntax = new ComplexSyntax(towerSyntax.getLeft(), towerSyntax.getRight(), Slash.FORWARD);
		Set<SplittingPair> pairs = MakeCompositionSplits.of(Category.create(topSyntax, top), categoryServices);
		for (SplittingPair pair : pairs) {
			Tower newTower = new Tower((Lambda) pair.right.getSemantics(), tower.getBottom());
			TowerCategory<LogicalExpression> newTowerCategory = new TowerCategory<>(towerSyntax, newTower);
			// note - not necessarily monadic
			List<TowerCategory<LogicalExpression>> results = monadicUnlower(newTowerCategory);
			for (TowerCategory<LogicalExpression> result : results) {
				TowerSyntax resultSyntax = new TowerSyntax(result.getSyntax(), Syntax.S, Syntax.S);
				Tower resultSemantics = new Tower((Lambda) pair.getLeft().getSemantics(), result.getSemantics());
				ret.add(new TowerCategory<>(resultSyntax, resultSemantics));
			}
		}

		return ret;
	}

	private TowerCategory<LogicalExpression> complexMonadicUnlower(ComplexCategory complexCategory,
																		 StateMonad exp) {
		TowerSyntax newSyntax = new TowerSyntax(complexCategory.getSyntax(), Syntax.S, Syntax.S);
		Lambda lambda = (Lambda) complexCategory.getSemantics();
		// TODO: Is this something we need?
		/*
        if (exp.getState().size() > 0) {
            return null;
        } */
        if (!IsContainingVariable.of(exp, lambda.getArgument())) {
            return null;
        }
        Variable newVar = new Variable(
                LogicLanguageServices.getTypeRepository()
                    .generalizeType(exp.getType()));
        Lambda newTop = new Lambda(newVar, ReplaceExpression.of(
        		lambda.getBody(), exp, newVar));

        // new top should NOT have bottom lambda variable
        if (IsContainingVariable.of(newTop, lambda.getArgument())) {
            return null;
        }
        Lambda newBottom = new Lambda(lambda.getArgument(), exp.getBody());
        Tower newTower = new Tower(newTop, newBottom);
        return new TowerCategory<>(newSyntax, newTower);
	}

	private List<TowerCategory<LogicalExpression>> monadicUnlower(Category<LogicalExpression> cat) {
		List<TowerCategory<LogicalExpression>> ret = new ArrayList<>();
		LogicalExpression sem = cat.getSemantics();
	    if (cat instanceof TowerCategory) {
	    	sem = towerCategoryServices.towerToLambda(sem);
		}
		for (LogicalExpression exp : AllSubExpressions.of(sem)) {
			if (!(exp instanceof StateMonad)) {
				continue;
			}
			StateMonad monadExp = (StateMonad) exp;
			if (cat instanceof ComplexCategory) {
				ComplexCategory complexCategory = (ComplexCategory) cat;
				ComplexSyntax complexSyntax = complexCategory.getSyntax();
				if (complexSyntax.getLeft().equals(Syntax.S)) {
					// case 2
					TowerCategory<LogicalExpression> result =
							complexMonadicUnlower(complexCategory, monadExp);
					if (result != null) {
						ret.add(result);
					}
				}
			}
			// case 1, applicable if monad
			TowerSyntax newSyntax = new TowerSyntax(Syntax.S, cat.getSyntax(), Syntax.S);
			// TODO: Do we need this?
			/*
			if (monadExp.getState().size() > 0) {
				return ret;
			}
			*/
            Variable newVar = new Variable(
                LogicLanguageServices.getTypeRepository()
                    .generalizeType(exp.getType()));
            Lambda newTop = new Lambda(newVar, ReplaceExpression.of(sem, exp, newVar));
			ret.add(new TowerCategory<>(newSyntax, new Tower(newTop, monadExp.getBody())));
		}
	    return ret;
	}

	public List<TowerCategory<LogicalExpression>> unlower(Category<LogicalExpression> cat) {
	    // We need to use MakeApplicationSplits rather than just getting
		// subexpressions since we need to split into ALL possible
		// pairs such that left(right) = original.
		LogicalExpression sem = cat.getSemantics();
		if (cat instanceof TowerCategory) {
			sem = towerCategoryServices.towerToLambda(sem);
		}
		Set<SplittingPair> baseSplits = MakeApplicationSplits.of(
				Category.create(Syntax.S, sem), categoryServices);
		List<TowerCategory<LogicalExpression>> ret = new ArrayList<>();
		// For each possible split, create a new tower
		Set<Pair<Lambda, LogicalExpression>> semPairs = new HashSet<>();
		for (SplittingPair split : baseSplits) {
		    if (!split.getRight().getSyntax().equals(Syntax.S)) {
		    	continue;
			}
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
		ret.addAll(monadicUnlower(cat));
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
        if (category instanceof TowerCategory) {
			// Since we're unlowering a tower, we can have multiple original tower tops and bottoms
			TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) category;
			Category<LogicalExpression> base = towerCategoryServices.getBottom(tower);
			/* TODO: Probably isn't necessary. I think we want lowers to be "one fell swoop" lowers.
			// Unlower the base of a tower.
			/*
			for (TowerCategory<LogicalExpression> newBase : unlower(base)) {
				TowerCategory<LogicalExpression> newTower =
						towerCategoryServices.replaceBase(tower, newBase);
				newTowers.add(newTower);
			}*/
			Tower towerSemantics = (Tower) tower.getSemantics();
			if (towerSemantics.getTop().getType().getDomain() instanceof MonadType) {
                newTowers.addAll(towerMonadicUnlower(tower));
			}
		}
        newTowers.addAll(unlower(category));

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
