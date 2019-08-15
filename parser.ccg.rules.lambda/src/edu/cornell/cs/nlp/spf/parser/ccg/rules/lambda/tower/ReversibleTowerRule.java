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
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryReversibleRecursiveParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.SentenceSpan;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.TowerRule;
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

	private final List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveParseRules;

	public ReversibleTowerRule(ITowerCategoryServices<LogicalExpression> towerCategoryServices,
							   List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> validRules,
							   ICategoryServices<LogicalExpression> categoryServices,
							   List<IBinaryReversibleParseRule<LogicalExpression>> baseRules,
							   ForwardReversibleApplication forwardApp) {
		super(towerCategoryServices, new ArrayList<>(validRules));
		this.recursiveParseRules = validRules;
		this.categoryServices = categoryServices;
		this.baseRules = baseRules;
		this.forwardApp = forwardApp;
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
			cat = towerCategoryServices.getBottom(tower);
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
		    		ret.addAll(towerCategoryServices.unlower(cat));
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
	public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
		// Unlower here
		Set<Category<LogicalExpression>> possibleResults = new HashSet<>();
		possibleResults.add(result);
		if (result instanceof TowerCategory) {
			possibleResults.addAll(towerCategoryServices.unlower(result));
		}

		// TODO: Reverse monadic base methods
		Set<Category<LogicalExpression>> ret = new HashSet<>();
		for (Category<LogicalExpression> res : possibleResults) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : this.recursiveParseRules) {
                ret.addAll(rule.reverseApplyLeft(left, res, span));
            }
		}
		return ret;
	}

	/*
	@Override
	public Set<Category<LogicalExpression>> reverseApplyLeft(Category<LogicalExpression> left, Category<LogicalExpression> result, SentenceSpan span) {
		List<Category<LogicalExpression>> possibleResults = new ArrayList<>();
		if (!(result instanceof TowerCategory) || result.height() < 2) {
			for (Category<LogicalExpression> res : towerCategoryServices.unlower(result)) {
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
            Category<LogicalExpression> leftBase = towerCategoryServices.getBottom(possibleLeft);
            for (Category<LogicalExpression> res : possibleResults) {
				Category<LogicalExpression> resBase = towerCategoryServices.getBottom(res);
				List<Category<LogicalExpression>> rightBases = new ArrayList<>();
				for (IBinaryReversibleParseRule<LogicalExpression> rule : baseRules) {
					rightBases.addAll(rule.reverseApplyLeft(leftBase, resBase, span));
				}
				ret.addAll(reverseCombineLeft(possibleLeft, res, rightBases));
			}
		}
		return ret;
	}*/

	@Override
	public Set<Category<LogicalExpression>> reverseApplyRight(Category<LogicalExpression> right, Category<LogicalExpression> result, SentenceSpan span) {
        // Unlower here
		Set<Category<LogicalExpression>> possibleResults = new HashSet<>();
		possibleResults.add(result);
		if (result instanceof TowerCategory) {
			possibleResults.addAll(towerCategoryServices.unlower(result));
		}

		// TODO: Reverse monadic base methods
		Set<Category<LogicalExpression>> ret = new HashSet<>();
		for (Category<LogicalExpression> res : possibleResults) {
            for (IBinaryReversibleParseRule<LogicalExpression> rule : this.recursiveParseRules) {
                ret.addAll(rule.reverseApplyRight(right, res, span));
            }
		}
		return ret;
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
            List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> recursiveRules = new ArrayList<>();
            for (String id : params.getSplit("recursiveRules")) {
                IBinaryReversibleRecursiveParseRule<LogicalExpression> rule = repo.get(id);
                recursiveRules.add(rule);
            }

            // Recursive rules can't add themselves.
			// As such, the list of recursive rules is specified here.
            for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : recursiveRules) {
            	for (IBinaryReversibleRecursiveParseRule<LogicalExpression> toAdd : recursiveRules) {
            		rule.addRecursiveRule(toAdd);
				}
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

            return new ReversibleTowerRule(
            		towerCategoryServices,
					recursiveRules,
					categoryServices,
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
