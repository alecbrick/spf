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
package edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower;

import edu.cornell.cs.nlp.spf.ccg.categories.*;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.MakeApplicationSplits;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.MakeCompositionSplits;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.AllSubExpressions;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.IsContainingVariable;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IRecursiveBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic.IMonadServices;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.TowerRule;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract application rule that supports generating one of the arguments given
 * the result and the other.
 *
 * @author Yoav Artzi
 */
public class ReversibleTowerRule extends
		TowerRule<LogicalExpression> implements
		IBinaryReversibleParseRule<LogicalExpression> {

	/**
	 *
	 */
	private static final long	serialVersionUID	= 113249788916519430L;

	public static final ILogger	LOG	= LoggerFactory
											.create(ReversibleTowerRule.class);

	private final ICategoryServices<LogicalExpression> categoryServices;

	private final List<IBinaryReversibleParseRule<LogicalExpression>> baseRules;

	private final ForwardReversibleApplication forwardApp;
	private final IMonadServices<LogicalExpression, Monad> monadServices;

	public ReversibleTowerRule(ITowerCategoryServices<LogicalExpression> towerCategoryServices,
							   List<IRecursiveBinaryParseRule<LogicalExpression>> validRules,
							   ICategoryServices<LogicalExpression> categoryServices,
							   IMonadServices<LogicalExpression, Monad> monadServices,
							   List<IBinaryReversibleParseRule<LogicalExpression>> baseRules,
							   ForwardReversibleApplication forwardApp) {
		super(towerCategoryServices, validRules);
		this.categoryServices = categoryServices;
		this.baseRules = baseRules;
		this.monadServices = monadServices;
		this.forwardApp = forwardApp;
	}

	// TODO: Refactor because this is a mess
    private List<TowerCategory<LogicalExpression>> towerMonadicUnlower(TowerCategory towerCategory) {
		List<TowerCategory<LogicalExpression>> ret = new ArrayList<>();
		Tower tower = (Tower) towerCategory.getSemantics();
		TowerSyntax towerSyntax = towerCategory.getSyntax();
		Lambda top = tower.getTop();
		ComplexSyntax topSyntax = new ComplexSyntax(towerSyntax.getLeft(), towerSyntax.getRight(), Slash.FORWARD);
		Set<SplittingPair> pairs = MakeCompositionSplits.of(Category.create(topSyntax, top), categoryServices);
		for (SplittingPair pair : pairs) {
			Tower newTower = new Tower((Lambda) pair.getRight().getSemantics(), tower.getBottom());
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
        for (LogicalExpression body : monadServices.logicalExpressionFromMonad(exp)) {
            // TODO: Use this as new body (when refactoring lower)
		}
        Lambda newBottom = new Lambda(lambda.getArgument(), exp.getBody());
        Tower newTower = new Tower(newTop, newBottom);
        return new TowerCategory<>(newSyntax, newTower);
	}

	private List<TowerCategory<LogicalExpression>> monadicUnlower(Category<LogicalExpression> cat) {
		List<TowerCategory<LogicalExpression>> ret = new ArrayList<>();
		LogicalExpression inputSem = cat.getSemantics();
	    if (cat instanceof TowerCategory) {
	    	inputSem = towerCategoryServices.towerToLambda(inputSem);
		}
	    Set<LogicalExpression> toUnlower = new HashSet<>();
	    toUnlower.add(inputSem);
	    if (cat.getSemantics().getType() instanceof MonadType) {
	        toUnlower.addAll(monadServices.unexecMonad((Monad) cat.getSemantics()));
		}
	    for (LogicalExpression sem : toUnlower) {
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
				for (LogicalExpression body : monadServices.logicalExpressionFromMonad(monadExp)) {
					ret.add(new TowerCategory<>(newSyntax, new Tower(newTop, body)));
				}
			}
		}
	    return ret;
	}

	public List<TowerCategory<LogicalExpression>> unlower(Category<LogicalExpression> cat) {
	    // We need to use MakeApplicationSplits rather than just getting
		// subexpressions since we need to split into ALL possible
		// pairs such that left(right) = original.
        Syntax syn = cat.getSyntax();
		LogicalExpression sem = cat.getSemantics();
		if (cat instanceof TowerCategory) {
			TowerCategory towerCat = (TowerCategory) cat;
			syn = new ComplexSyntax(towerCat.getSyntax().getLeft(), towerCat.getSyntax().getRight(), Slash.FORWARD);
			sem = towerCategoryServices.towerToLambda(sem);
		}
		Set<SplittingPair> baseSplits = MakeApplicationSplits.of(
				Category.create(syn, sem), categoryServices);
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
			Lambda leftLambda = (Lambda) split.getLeft().getSemantics();
			if (leftLambda.getArgument().equals(leftLambda.getBody())) {
				continue;
			}
			semPairs.add(Pair.of((Lambda) split.getLeft().getSemantics(), split.getRight().getSemantics()));
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

	private List<ComplexCategory<LogicalExpression>> towerToList(
			Category<LogicalExpression> cat) {
		List<ComplexCategory<LogicalExpression>> ret = new ArrayList<>();
        while (cat instanceof TowerCategory) {
			TowerCategory<LogicalExpression> tower = (TowerCategory<LogicalExpression>) cat;
			TowerSyntax syntax = tower.getSyntax();
			Tower semantics = (Tower) tower.getSemantics();
			ret.add(0, new ComplexCategory<>(
					new ComplexSyntax(syntax.getLeft(), syntax.getRight(), Slash.FORWARD),
					semantics.getTop()));
			cat = towerCategoryServices.getBase(tower);
		}
        return ret;
	}

	// SORRY
	private void reverseCombineHelper(
            List<ComplexCategory<LogicalExpression>> leftCats,
            List<ComplexCategory<LogicalExpression>> resCats,
            int leftIndex, int resIndex,
            List<List<ComplexCategory<LogicalExpression>>> towerStack,
            List<Category<LogicalExpression>> rightBases,
            boolean isLeft,
            Set<Category<LogicalExpression>> ret) {
		// Base case.
		// TODO: Do we want to do this as we go?
		// Would definitely save us time in worst-case scenarios,
		// but in practice this will never be too ridiculous.
		if (leftIndex == leftCats.size() || resIndex == resCats.size()) {
			List<Category<LogicalExpression>> rightResults = new ArrayList<>(rightBases);
			List<Category<LogicalExpression>> tempResults = new ArrayList<>();
			for (List<ComplexCategory<LogicalExpression>> levelList : towerStack) {
				for (ComplexCategory<LogicalExpression> level : levelList) {
				    ComplexSyntax levelSyntax = level.getSyntax();
					for (Category<LogicalExpression> tower : rightResults) {
						tempResults.add(new TowerCategory<>(
								new TowerSyntax(tower.getSyntax(), levelSyntax.getLeft(), levelSyntax.getRight()),
								new Tower((Lambda) level.getSemantics(), tower.getSemantics())
						));
					}
				}
				rightResults = tempResults;
				tempResults = new ArrayList<>();
			}
			// Add extras to the top of each tower
		    if (resIndex != resCats.size()) {
		    	for (Category<LogicalExpression> tower : rightResults) {
		    		Category<LogicalExpression> currTower = tower;
		    		for (int i = resIndex; i < resCats.size(); i++) {
		    		    ComplexCategory<LogicalExpression> level = resCats.get(i);
		    		    ComplexSyntax levelSyntax = level.getSyntax();
						currTower = new TowerCategory<>(
								new TowerSyntax(currTower.getSyntax(), levelSyntax.getLeft(), levelSyntax.getRight()),
								new Tower((Lambda) level.getSemantics(), currTower.getSemantics())
						);
					}
		    		rightResults.add(currTower);
				}
			}
		    // Possible monadic unlower (for Lower Left/Right)
		    for (Category<LogicalExpression> cat : rightResults) {
		    	if (cat.getSemantics().getType() instanceof MonadType) {
		    		ret.addAll(monadicUnlower(cat));
				}
			}
		    ret.addAll(rightResults);
			return;
		}

		// Recursive case.
		ComplexCategory<LogicalExpression> leftCat = leftCats.get(leftIndex);
		ComplexCategory<LogicalExpression> resCat = resCats.get(resIndex);

		if (leftCat.equals(resCat)) {
			// Tower level is part of left, so we skip and continue.
            reverseCombineHelper(leftCats, resCats, leftIndex + 1,
					resIndex + 1, towerStack, rightBases, isLeft, ret);
            return;
		}

		// CASE 1: Result level was created via a composition of left and right.
        // Reverse-compose the result level and move to the next level on both towers.

		List<ComplexCategory<LogicalExpression>> toAdd = new ArrayList<>();
		if (isLeft) {
            // Reverse composition. This is just another form of application.
            // Suppose we have \x.f(g(x)). If we extract the argument from f(g(x))
            // given the function \y.f(y), we get g(x). Reapply the argument x
            // to get \x.g(x), which is what we want.
            Lambda resSem = (Lambda) resCat.getSemantics();
            LogicalExpression resBody = resSem.getBody();
            Category<LogicalExpression> bodyCategory = Category.create(resCat.getSyntax().getLeft(), resBody);
            Set<Category<LogicalExpression>> rights = forwardApp.reverseApplyLeft(
                    leftCat, bodyCategory, null);
            // We need to convert back to complex syntax because we used application.
            for (Category<LogicalExpression> rightBody : rights) {
                LogicalExpression rightBodySem = rightBody.getSemantics();
                Lambda rightLambda = new Lambda(resSem.getArgument(), rightBodySem);
                ComplexSyntax rightSyntax = new ComplexSyntax(
                        rightBody.getSyntax(), resCat.getSyntax().getRight(), Slash.FORWARD);
                toAdd.add(new ComplexCategory<>(rightSyntax, rightLambda));
            }
		} else {
			// Suppose we have \x.f(g(x)). If we extract the function from f(g(x))
			// given g(x), we get the function \y.f(y), which is what we want.
			Lambda resSem = (Lambda) resCat.getSemantics();
			LogicalExpression resBody = resSem.getBody();
			Category<LogicalExpression> bodyCategory = Category.create(resCat.getSyntax().getLeft(), resBody);
			Set<Category<LogicalExpression>> lefts =
					forwardApp.reverseApplyRight(leftCat, bodyCategory, null);
			for (Category<LogicalExpression> left : lefts) {
				if (left instanceof ComplexCategory) {
					toAdd.add((ComplexCategory<LogicalExpression>) left);
				}
			}
		}
		// Add the results, recurse, then remove the results.
		towerStack.add(toAdd);
		reverseCombineHelper(leftCats, resCats, leftIndex + 1,
				resIndex + 1, towerStack, rightBases, isLeft, ret);
		towerStack.remove(towerStack.size() - 1);

		// CASE 2: The level is part of the right tower.
		// Add it to the tower stack and continue.

		if (leftCats.size() - leftIndex < resCats.size() - resIndex) {
            toAdd = new ArrayList<>();
            toAdd.add(resCat);
            towerStack.add(toAdd);
            reverseCombineHelper(leftCats, resCats, leftIndex,
                    resIndex + 1, towerStack, rightBases, isLeft, ret);
            towerStack.remove(towerStack.size() - 1);
		}
	}

	// Performs reverse tower combination, including Lift Left and Lift Right.
	private Set<Category<LogicalExpression>> reverseCombineLeft(
			Category<LogicalExpression> left, Category<LogicalExpression> res,
			List<Category<LogicalExpression>> rightBases) {
	    Set<Category<LogicalExpression>> ret = new HashSet<>();
		List<ComplexCategory<LogicalExpression>> leftCats = towerToList(left);
		List<ComplexCategory<LogicalExpression>> resCats = towerToList(res);
		List<List<ComplexCategory<LogicalExpression>>> curr = new ArrayList<>();
		reverseCombineHelper(leftCats, resCats, 0, 0, curr, rightBases, true, ret);
		return ret;
	}

	@Override
    // TODO: Reverse monadic base methods
	public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
		List<Category<LogicalExpression>> possibleResults = new ArrayList<>();
		if (!(result instanceof TowerCategory) || result.height() < 2) {
			for (Category<LogicalExpression> res : unlower(result)) {
				if (res.height() >= left.height()) {
					possibleResults.add(res);
				}
			}
		}
		if (left.height() < result.height()) {
            possibleResults.add(result);
		}

		if (possibleResults.size() == 0) {
			return new HashSet<>();
		}

		List<Category<LogicalExpression>> lefts = new ArrayList<>();
		lefts.add(left);
		Category<LogicalExpression> liftCandidate = null;
		if (left instanceof TowerCategory) {
			TowerCategory<LogicalExpression> tempTower = (TowerCategory<LogicalExpression>) left;
			Tower towerSem = (Tower) tempTower.getSemantics();
			if (towerSem.getTop().getComplexType().getRange() instanceof MonadType) {
				liftCandidate = towerCategoryServices.lower(tempTower);
			}
		} else {
			liftCandidate = left;
		}
		if (liftCandidate != null) {
			if (liftCandidate.getSemantics().getType() instanceof MonadType) {
				lefts.add(towerCategoryServices.monadicLift(
						liftCandidate, new TowerSyntax(Syntax.S, Syntax.S, Syntax.S)));
			}
			lefts.add(liftCandidate);
		}

		Set<Category<LogicalExpression>> ret = new HashSet<>();
		for (Category<LogicalExpression> possibleLeft : lefts) {
            Category<LogicalExpression> leftBase = towerCategoryServices.getBase(possibleLeft);
            for (Category<LogicalExpression> res : possibleResults) {
				Category<LogicalExpression> resBase = towerCategoryServices.getBase(res);
				List<Category<LogicalExpression>> rightBases = new ArrayList<>();
				for (IBinaryReversibleParseRule<LogicalExpression> rule : baseRules) {
					rightBases.addAll(rule.reverseApplyLeft(leftBase, resBase, span));
				}
				ret.addAll(reverseCombineLeft(possibleLeft, res, rightBases));
			}
		}
		return ret;
	}

	@Override
	public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
	    throw new NoSuchMethodError();
	}

	public class ReversibleTowerRuleCreator implements
			IResourceObjectCreator<ReversibleTowerRule> {

        private final String	type;

        public ReversibleTowerRuleCreator() {
            this("rule.tower.reversible");
        }

        public ReversibleTowerRuleCreator(String type) {
            this.type = type;
        }

        @SuppressWarnings("unchecked")
        @Override
        public ReversibleTowerRule create(ParameterizedExperiment.Parameters params,
													   IResourceRepository repo) {
            List<IRecursiveBinaryParseRule<LogicalExpression>> recursiveRules = new ArrayList<>();
            for (String id : params.getSplit("recursiveRules")) {
                IRecursiveBinaryParseRule<LogicalExpression> rule = repo.get(id);
                recursiveRules.add(rule);
            }

            List<IBinaryReversibleParseRule<LogicalExpression>> baseRules = new ArrayList<>();
            ForwardReversibleApplication forwardApp = null;
            for (String id : params.getSplit("baseRules")) {
                IBinaryReversibleParseRule<LogicalExpression> rule = repo.get(id);
                baseRules.add(rule);
                if (rule instanceof ForwardReversibleApplication) {
                	forwardApp = (ForwardReversibleApplication) rule;
				}
            }
            assert(forwardApp != null);
            ICategoryServices<LogicalExpression> categoryServices =
					repo.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE);
			ITowerCategoryServices<LogicalExpression> towerCategoryServices =
					repo.get(ParameterizedExperiment.TOWER_CATEGORY_SERVICES_RESOURCE);
			IMonadServices<LogicalExpression, Monad> monadServices =
					repo.get(ParameterizedExperiment.MONAD_SERVICES_RESOURCE);

            return new ReversibleTowerRule(
            		towerCategoryServices,
					recursiveRules,
					categoryServices,
					monadServices,
					baseRules,
					forwardApp);
        }

        @Override
        public String type() {
            return type;
        }

        @Override
        public ResourceUsage usage() {
            return ResourceUsage
                    .builder(type, ReversibleTowerRule.class)
					.addParam("recursiveRules",
							String.class,
							"The list of recursive rules to be used during standard tower combination")
					.addParam("baseRules",
							String.class,
							"The list of possible binary reversible rules")
                    .build();
        }
    }
}
