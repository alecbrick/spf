package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.ccg.categories.*;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.MakeApplicationSplits;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.MakeCompositionSplits;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.*;
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.utils.composites.Pair;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class TowerCategoryServices extends AbstractTowerCategoryServices<LogicalExpression> {
    private final ICategoryServices<LogicalExpression> categoryServices;

    public TowerCategoryServices() {
        this(false);
    }

    public TowerCategoryServices(boolean doTypeChecking) {
        this.categoryServices = new LogicalExpressionCategoryServices(doTypeChecking);
    }

    public Category<LogicalExpression> setBase(Category<LogicalExpression> cat, Category<LogicalExpression> base) {
        if (cat instanceof TowerCategory) {
            TowerCategory<LogicalExpression> towerCat = (TowerCategory<LogicalExpression>) cat;
            TowerSyntax syn = towerCat.getSyntax();
            Tower sem = (Tower) towerCat.getSemantics();
            Category<LogicalExpression> newBase = setBase(getBase(cat), base);
            TowerSyntax newSyn = new TowerSyntax(newBase.getSyntax(), syn.getLeft(), syn.getRight());
            Tower newSem = new Tower(sem.getTop(), newBase.getSemantics());
            return new TowerCategory<>(newSyn, newSem);
        } else {
            return base;
        }
    }

    public Lambda getTopSemantics(TowerCategory<LogicalExpression> tower) {
        Tower semantics = (Tower) tower.getSemantics();
        return semantics.getTop();
    }

    @Override
    public LogicalExpression getBottomSemantics(TowerCategory<LogicalExpression> tower) {
        Tower semantics = (Tower) tower.getSemantics();
        return semantics.getBottom();
    }

    public TowerCategory<LogicalExpression> combineTowersWithBase(
            TowerCategory<LogicalExpression> leftTower,
            TowerCategory<LogicalExpression> rightTower,
            Category<LogicalExpression> newBase) {
        TowerSyntax newSyntax = new TowerSyntax(
                newBase.getSyntax(),
                leftTower.getSyntax().getLeft(),
                rightTower.getSyntax().getRight()
        );

        Tower leftSemantics = (Tower) leftTower.getSemantics();
        Tower rightSemantics = (Tower) rightTower.getSemantics();
        Lambda newTop =
                (Lambda) categoryServices.compose(leftSemantics.getTop(),
                        rightSemantics.getTop(), 1);

        Tower newSemantics =
                new Tower(newTop, newBase.getSemantics());
        return new TowerCategory<LogicalExpression>(
                newSyntax, newSemantics
        );
    }


    /**
     * Take the top syntax and semantics of towerForTop,
     * and create a tower using base as a base.
     * No idea how to name this one. Sorry!
     */
    public TowerCategory<LogicalExpression> replaceBase(
            TowerCategory<LogicalExpression> toReplace,
            Category<LogicalExpression> base) {
        TowerSyntax oldSyntax = toReplace.getSyntax();
        Tower oldSemantics =
                (Tower) toReplace.getSemantics();
        TowerSyntax newTowerSyntax = new TowerSyntax(
                base.getSyntax(), oldSyntax.getLeft(),
                oldSyntax.getRight());
        Tower newTowerSemantics = new Tower(
                oldSemantics.getTop(),
                base.getSemantics()
        );
        return new TowerCategory<>(newTowerSyntax, newTowerSemantics);
    }

    @Override
    public boolean canCombineTops(TowerCategory<LogicalExpression> leftTower, TowerCategory<LogicalExpression> rightTower) {
        if (leftTower.getSyntax().getRight().unify(rightTower.getSyntax().getLeft()) == null) {
            return false;
        }

        Tower leftSemantics = (Tower) leftTower.getSemantics();
        Tower rightSemantics = (Tower) rightTower.getSemantics();

        if (categoryServices.compose(
                leftSemantics.getTop(),
                rightSemantics.getTop(), 1) == null) {
            return false;
        }

        return true;
    }

    @Override
    public TowerCategory<LogicalExpression> monadicLift(
            Category<LogicalExpression> toLift,
            Syntax patternSyntax) {
        Type toLiftType = toLift.getSemantics().getType();
        if (!(toLiftType instanceof MonadType)) {
            return null;
        }
        MonadType monadType = (MonadType) toLiftType;
        final Variable boundVariable = new Variable(LogicLanguageServices
                .getTypeRepository().generalizeType(
                        LogicLanguageServices.getTypeRepository()
                                .generalizeType(monadType.getDomain())));

        final Variable lambdaVariable = new Variable(LogicLanguageServices
                .getTypeRepository().generalizeType(
                        LogicLanguageServices.getTypeRepository()
                                .generalizeType(monadType)));

        final Binding topMonad = new Binding(toLift.getSemantics(),
                lambdaVariable, boundVariable);
        final Lambda towerTop = new Lambda(lambdaVariable, topMonad);
        final Tower newSemantics = new Tower(towerTop, boundVariable);
        final TowerSyntax newSyntax = new TowerSyntax(toLift.getSyntax(), patternSyntax, patternSyntax);

        return new TowerCategory<>(newSyntax, newSemantics);
    }

    @Override
    public boolean hasMonadicBaseArg(Category<LogicalExpression> cat) {
        if (cat instanceof TowerCategory) {
            return hasMonadicBaseArg(getBase(cat));
        }
        if (cat instanceof ComplexCategory) {
            Lambda lambda = (Lambda) cat.getSemantics();
            return lambda.getComplexType().getDomain() instanceof MonadType;
        }
        return false;
    }

    // Lift a category into a continuation tower.
    public TowerCategory<LogicalExpression> lift(
            Category<LogicalExpression> toLift) {
        final Syntax syn = toLift.getSyntax();
        final LogicalExpression sem = toLift.getSemantics();



        final TowerSyntax newSyntax = new TowerSyntax(
                syn, Syntax.S, Syntax.S
        );

        final Type semType = sem.getType();
        final Variable variable = new Variable(LogicLanguageServices
                .getTypeRepository().generalizeType(
                        LogicLanguageServices.getTypeRepository()
                                .generalizeType(semType)));
        final Lambda topSem = new Lambda(variable, variable);
        // TODO: Not sure if we should wrap this.
        final Tower newSemantics =
                (Tower) LambdaWrapped.of(
                        new Tower(topSem, sem));
        return new TowerCategory<>(newSyntax, newSemantics);
    }

    // Flatten a continuation tower.
    public Category<LogicalExpression> lower(
            TowerCategory<LogicalExpression> toLower) {
        TowerSyntax towerSyntax = toLower.getSyntax();
        Tower towerSemantics =
                (Tower) toLower.getSemantics();
        if (towerSemantics.getTop().getComplexType().getDomain()
                instanceof MonadType) {
            return monadicLower(toLower);
        }
        // We can lower when right and base are S (or whatever the monad syntax is)
        Syntax monadSyntax = MonadServices.getMonadSyntax();
        if (towerSyntax.getBase().equals(Syntax.S) || towerSyntax.getBase().equals(monadSyntax)) {
            if ((towerSyntax.getRight().equals(Syntax.S) || towerSyntax.getRight().equals(monadSyntax)) &&
                    towerSyntax.getRight().equals(towerSyntax.getBase())) {
                LogicalExpression newSemantics =
                        categoryServices.apply(towerSemantics.getTop(),
                                towerSemantics.getBottom());
                // TODO: could maybe indicate an error but idk.
                if (newSemantics == null) {
                    return null;
                }
                if (towerSyntax.getLeft() instanceof TowerSyntax) {
                    newSemantics = lambdaToTower(newSemantics);
                }
                return Category.create(towerSyntax.getLeft(), newSemantics);
            }
        } else if (towerSyntax.getBase() instanceof TowerSyntax) {
            TowerCategory<LogicalExpression> baseTower =
                    (TowerCategory) getBase(toLower);
            // Lower the base.
            Category<LogicalExpression> loweredCat = lower(baseTower);
            if (loweredCat != null) {
                TowerCategory<LogicalExpression> withLoweredBase =
                        replaceBase(toLower, loweredCat);
                // If we can lower the base, try lowering with the new base.
                Category<LogicalExpression> loweredTwice =
                        lower(withLoweredBase);
                if (loweredTwice != null) {
                    // If it worked, return that.
                    return loweredTwice;
                } else {
                    // Otherwise, return the tower with lowered base.
                    return withLoweredBase;
                }
            }
        }
        return null;
    }

    // TODO: Refactor all of these because this is a mess
    // Given an input of \k.g[c(k)], do the following:
    //  1. Note that c is a continuation. As such, we can write it as \j.x[j(z)].
    //  2. As such, the input can be written as \k.g[x[k(z)]], or a tower where g[x[]] is the top.
    //  3. Split the tower top to get g[] and x[].
    //  4. Unlower the tower [x[]][z] to give us the tower a.
    //  5. Return  [g[]][a].
    private Set<TowerCategory<LogicalExpression>> towerMonadicUnlower(TowerCategory towerCategory) {
		Set<TowerCategory<LogicalExpression>> ret = new HashSet<>();
		Tower tower = (Tower) towerCategory.getSemantics();
		TowerSyntax towerSyntax = towerCategory.getSyntax();
		Lambda top = tower.getTop();
		ComplexSyntax topSyntax = new ComplexSyntax(towerSyntax.getLeft(), towerSyntax.getRight(), Slash.FORWARD);
        // Split the top into g[] and x[].
		Set<SplittingServices.SplittingPair> pairs = MakeCompositionSplits.of(Category.create(topSyntax, top), categoryServices);
		for (SplittingServices.SplittingPair pair : pairs) {
		    if (pair == null) {
		        continue;
            }
		    ComplexCategory<LogicalExpression> leftPair =
                    (ComplexCategory<LogicalExpression>) pair.getLeft();
		    Syntax monadSyntax = MonadServices.getMonadSyntax();
		    // top must be S/S (or A/A or whatever our monad syntax is)
		    if (!leftPair.getSyntax().getLeft().equals(monadSyntax) ||
                    !leftPair.getSyntax().getRight().equals(monadSyntax) ||
                    !leftPair.getSlash().equals(Slash.FORWARD)) {
		        continue;
            }

		    // We will unlower the tower [x[]][z].
			Tower newTower = new Tower((Lambda) pair.getRight().getSemantics(), tower.getBottom());
			TowerCategory<LogicalExpression> newTowerCategory = new TowerCategory<>(towerSyntax, newTower);
			Set<TowerCategory<LogicalExpression>> results = nonMonadicUnlower(newTowerCategory, false);
			// The result tower will have g[] on top and the unlowered tower on the bottom.
			for (TowerCategory<LogicalExpression> result : results) {
				TowerSyntax resultSyntax = new TowerSyntax(result.getSyntax(), monadSyntax, monadSyntax);
				Tower resultSemantics = new Tower((Lambda) pair.getLeft().getSemantics(), result.getSemantics());
				ret.add(new TowerCategory<>(resultSyntax, resultSemantics));
			}
		}

		return ret;
	}

	private Set<TowerCategory<LogicalExpression>> complexMonadicUnlower(ComplexCategory complexCategory,
																		 StateMonad exp) {
        Set<TowerCategory<LogicalExpression>> ret = new HashSet<>();
        Syntax monadSyntax = MonadServices.getMonadSyntax();
		TowerSyntax newSyntax = new TowerSyntax(complexCategory.getSyntax(), monadSyntax, monadSyntax);
		Lambda lambda = (Lambda) complexCategory.getSemantics();
		// TODO: Is this something we need?
        if (exp.getState().size() > 0) {
            return null;
        }
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
        for (LogicalExpression body : MonadServices.logicalExpressionFromMonad(exp)) {
            Lambda newBottom = new Lambda(lambda.getArgument(), body);
            Tower newTower = new Tower(newTop, newBottom);
            ret.add(new TowerCategory<>(newSyntax, newTower));
		}
        return ret;
	}

	private Set<TowerCategory<LogicalExpression>> monadicUnlower(Category<LogicalExpression> cat) {
		Set<TowerCategory<LogicalExpression>> ret = new HashSet<>();
		LogicalExpression inputSem = cat.getSemantics();
	    if (cat instanceof TowerCategory) {
	        if (cat.height() >= 3) {
	            return ret;
            }
	    	inputSem = towerToLambda(inputSem);
	    	ret.addAll(towerMonadicUnlower((TowerCategory) cat));
		}
	    // Unexec the current monad (if we have one) and store the results in toLower.
	    Set<LogicalExpression> toUnlower = new HashSet<>();
	    toUnlower.add(inputSem);
	    if (cat.getSemantics().getType() instanceof MonadType) {
	        Set<Monad> unexecResults = MonadServices.unexecMonad((Monad) cat.getSemantics());
	        toUnlower.addAll(unexecResults);
	        // TODO: Possibly overgenerating - maybe we only care about
            //  the right part of the binding?
	        for (Monad unexecResult : unexecResults) {
                toUnlower.addAll(MonadServices.unexecMonad(unexecResult));
            }
		}
	    // Iterate over possible expressions to unlower.
	    for (LogicalExpression sem : toUnlower) {
	        // Find all state monads in the expression.
			for (LogicalExpression exp : AllSubExpressions.of(sem)) {
				if (!(exp instanceof StateMonad)) {
				    continue;
				}
				StateMonad monadExp = (StateMonad) exp;
                if (monadExp.getState().size() > 0) {
                    continue;
                }
				// Unlower complex categories.
				if (cat instanceof ComplexCategory) {
					ComplexCategory complexCategory = (ComplexCategory) cat;
					ComplexSyntax complexSyntax = complexCategory.getSyntax();
					if (complexSyntax.getLeft().equals(Syntax.S)) {
						// case 2
						Set<TowerCategory<LogicalExpression>> result =
								complexMonadicUnlower(complexCategory, monadExp);
						if (result.size() != 0) {
							ret.addAll(result);
						}
					}
				}
				// case 1, applicable if monad
                Syntax monadSyntax = MonadServices.getMonadSyntax();
				TowerSyntax newSyntax = new TowerSyntax(monadSyntax, cat.getSyntax(), monadSyntax);

				Variable newVar = new Variable(
						LogicLanguageServices.getTypeRepository()
								.generalizeType(exp.getType()));
				Lambda newTop = new Lambda(newVar, ReplaceExpression.of(sem, exp, newVar));
				for (LogicalExpression body : MonadServices.logicalExpressionFromMonad(monadExp)) {
					ret.add(new TowerCategory<>(newSyntax, new Tower(newTop, body)));
				}
			}
		}
	    return ret;
	}

	// cat is not monad, lambda cat has non-monadic body
	private Set<TowerCategory<LogicalExpression>> nonMonadicUnlower(Category<LogicalExpression> cat, boolean recursive) {
        Syntax syn = cat.getSyntax();
        LogicalExpression sem = cat.getSemantics();
		if (cat instanceof TowerCategory) {
			TowerCategory towerCat = (TowerCategory) cat;
			syn = new ComplexSyntax(towerCat.getSyntax().getLeft(), towerCat.getSyntax().getBase(), Slash.FORWARD);
			sem = towerToLambda(sem);
			recursive = false;
		}
        Type truthType = LogicLanguageServices.getTypeRepository().getTruthValueType();
		List<LogicalExpression> subExps = AllSubExpressions.of(sem);
		subExps.removeIf(exp -> !exp.getType().equals(truthType));
		Set<TowerCategory<LogicalExpression>> ret = new HashSet<>();
		// For each possible split, create a new tower
		Set<Pair<Lambda, LogicalExpression>> semPairs = new HashSet<>();
		for (LogicalExpression subExp : subExps) {
		    Variable v = new Variable(truthType);
		    LogicalExpression replacedExp = ReplaceExpression.of(sem, subExp, v);
		    Lambda top = new Lambda(v, replacedExp);
		    if (recursive) {
		        for (TowerCategory<LogicalExpression> res : nonMonadicUnlower(Category.create(Syntax.S, subExp), false)) {
		            ret.add(new TowerCategory<>(
		                    new TowerSyntax(res.getSyntax(), Syntax.S, Syntax.S),
                            new Tower(top, res.getSemantics())));
                }
            }
		    ret.add(new TowerCategory<>(
		            new TowerSyntax(Syntax.S, cat.getSyntax(), Syntax.S),
                    new Tower(top, subExp)));
        }
		/*
		for (SplittingServices.SplittingPair split : baseSplits) {
		    if (recursive) {
                // Perform recursive unlowering.
                if (split.getLeft() instanceof ComplexCategory) {
                    ComplexCategory<LogicalExpression> leftPair =
                            (ComplexCategory<LogicalExpression>) split.getLeft();
                    if (leftPair.getSyntax().getLeft().equals(Syntax.S) &&
                            leftPair.getSyntax().getRight().equals(Syntax.S) &&
                            leftPair.getSlash().equals(Slash.FORWARD)) {
                        Lambda leftSem = (Lambda) leftPair.getSemantics();
                        if (!leftSem.getArgument().equals(leftSem.getBody())) {
                            for (TowerCategory<LogicalExpression> res : nonMonadicUnlower(split.getRight(), false)) {
                                ret.add(new TowerCategory<>(
                                        new TowerSyntax(res.getSyntax(), Syntax.S, Syntax.S),
                                        new Tower((Lambda) leftPair.getSemantics(), res.getSemantics())
                                ));
                            }
                        }
                    }
                }
            }
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
		// Only add semantic splits, because we only care about those
		for (Pair<Lambda, LogicalExpression> semPair : semPairs) {
			TowerSyntax newTowerSyntax = new TowerSyntax(Syntax.S, cat.getSyntax(), Syntax.S);
			Tower newTowerSemantics = new Tower(
					semPair.first(),
					semPair.second());
			TowerCategory<LogicalExpression> newTower = new TowerCategory<>(newTowerSyntax, newTowerSemantics);
			ret.add(newTower);
		}*/
		return ret;
    }

	@Override
	public Set<TowerCategory<LogicalExpression>> unlower(Category<LogicalExpression> cat) {
	    // We need to use MakeApplicationSplits rather than just getting
		// subexpressions since we need to split into ALL possible
		// pairs such that left(right) = original.
        Syntax syn = cat.getSyntax();
		LogicalExpression sem = cat.getSemantics();
		if (sem instanceof Monad ||
                (sem instanceof Lambda && ((Lambda) sem).getBody() instanceof Monad)) {
            return monadicUnlower(cat);
        } else if (sem instanceof Tower && ((Tower) sem).getTop().getBody() instanceof Monad) {
		    // Two possibilities for towers: Case 4, or non-monadic basic case
		    Set<TowerCategory<LogicalExpression>> ret = new HashSet<>();
		    ret.addAll(towerMonadicUnlower((TowerCategory<LogicalExpression>) cat));
		    ret.addAll(nonMonadicUnlower(cat, true));
		    return ret;
        } else {
		    return nonMonadicUnlower(cat, true);
        }
	}

    // Converts a tower into an equivalent lambda expression.
    public Lambda towerToLambda(LogicalExpression towerExp) {
        Tower tower = (Tower) towerExp;
        Lambda top = tower.getTop();
        LogicalExpression bottom = tower.getBottom();
	    ComplexType topType = top.getComplexType();
	    Type argType = topType.getDomain();
	    Type bottomType = bottom.getType();
	    String kappaLabel = ComplexType.composeString(argType, bottomType,
                null);
	    ComplexType kappaType = ComplexType.create(kappaLabel, bottomType,
                argType, null);
	    Variable kappa = new Variable(LogicLanguageServices
                .getTypeRepository().generalizeType(kappaType));
	    LogicalExpression[] literalArgs = {
	            bottom
	    };
	    LogicalExpression newBody = categoryServices.apply(top,
                new Literal(kappa, literalArgs));
	    return new Lambda(kappa, newBody);
	}

	// Converts a lambda into an equivalent tower,
    // provided that the lambda is continuized.
	public Tower lambdaToTower(LogicalExpression lambdaExp) {
        Lambda lambda = (Lambda) lambdaExp;
        // This one's a little harder - we need to extract the relevant
        // lambda expression from the tower.
        Variable arg = lambda.getArgument();
        List<Literal> literals = GetAllLiterals.of(lambda);
        // correct will hold the literal with the lambda's argument.
        Literal correct = null;
        for (Literal l : literals) {
            if (l.getPredicate().equals(arg)) {
                // we done found it
                correct = l;
                break;
            }
        }
        // This should be true if the lambda is continuized.
        assert(correct != null);
        // The argument of the literal is the tower bottom, by definition.
        LogicalExpression newBottom = correct.getArg(0);
        // We now make a variable for the hole in the tower top.
        Variable continuationVar = new Variable(LogicLanguageServices
                .getTypeRepository().generalizeType(correct.getType()));
        // Replace the literal with the hole variable.
        LogicalExpression replacedLambda = ReplaceExpression.of(
                lambda.getBody(), correct, continuationVar );
        // Parameterize the new expression by the hole variable.
        // This gives us the new tower top.
        Lambda newTop = new Lambda(continuationVar, replacedLambda);
        return new Tower(newTop, newBottom);
    }

    public Category<LogicalExpression> reverseMonadicLift(TowerCategory<LogicalExpression> cat) {
        TowerSyntax syntax = cat.getSyntax();
        Syntax monadSyntax = MonadServices.getMonadSyntax();

        Tower sem = (Tower) cat.getSemantics();
        Lambda top = sem.getTop();
        LogicalExpression bottom = sem.getBottom();
        // empty top case
        if (top.getArgument().equals(top.getBody()) && bottom instanceof StateMonad) {
            return Category.create(syntax.getBase(), sem.getBottom());
        } else if (top.getBody() instanceof Binding) {
            Binding binding = (Binding) top.getBody();
            if (binding.getRight().equals(top.getArgument()) &&
                    bottom.equals(binding.getVariable())) {
                return Category.create(syntax.getBase(), binding.getLeft());
            }
        }
        return null;
    }

    @Override
    public Set<Category<LogicalExpression>> reverseReset(Category<LogicalExpression> cat) {
        Set<Category<LogicalExpression>> ret = new HashSet<>();
        if (!(cat instanceof TowerCategory)) {
            ret.add(cat);
            return ret;
        }

        // Reverse monadic lift, then reverse monadic lower
        Category<LogicalExpression> unlifted = reverseMonadicLift((TowerCategory<LogicalExpression>) cat);
        if (unlifted == null) {
            return ret;
        }
        ret.addAll(monadicUnlower(unlifted));
        return ret;
    }

    // TODO: What if it's not a state monad?
    public Category<LogicalExpression> monadicLower(
            TowerCategory<LogicalExpression> toLower) {
        TowerSyntax towerSyntax = toLower.getSyntax();
        Tower towerSemantics =
                (Tower) toLower.getSemantics();
        Syntax monadSyntax = MonadServices.getMonadSyntax();
        if ((towerSyntax.getBase().equals(monadSyntax) &&
                towerSyntax.getRight().equals(monadSyntax))) {
            // Case 1
            Syntax newSyntax = towerSyntax.getLeft();
            LogicalExpression newSemantics = categoryServices.apply(
                    towerSemantics.getTop(),
                    MonadServices.logicalExpressionToMonad(towerSemantics.getBottom()));
            if (newSemantics instanceof Monad) {
                Monad monadSemantics = (Monad) newSemantics;
                StateMonadParams execResults = (StateMonadParams) monadSemantics.exec(new StateMonadParams());
                newSemantics = new StateMonad(execResults.getOutput(), execResults.getState());
            }
            if (newSyntax instanceof TowerSyntax && newSemantics instanceof Lambda) {
                newSemantics = lambdaToTower((Lambda) newSemantics);
            }
            return Category.create(newSyntax, newSemantics);
        } else if (towerSyntax.getLeft().equals(monadSyntax) &&
                towerSyntax.getRight().equals(monadSyntax)) {
            if (towerSyntax.getBase() instanceof ComplexSyntax) {
                ComplexSyntax complexBase =
                        (ComplexSyntax) towerSyntax.getBase();
                if (complexBase.getLeft().equals(monadSyntax)) {
                    // Case 2
                    Lambda baseSemantics = (Lambda) towerSemantics.getBottom();
                    // Lambda variable will be an argument to base, so the
                    // type needs to be the same
                    Variable lambdaVariable = new Variable(LogicLanguageServices
                        .getTypeRepository().generalizeType(
                                baseSemantics.getComplexType().getDomain()));
                    // Apply the base to the new lambda variable
                    LogicalExpression application = categoryServices.apply(
                            baseSemantics, lambdaVariable);
                    // Monad-ify above expression, and apply tower top to it
                    Lambda newSemantics = new Lambda(lambdaVariable,
                            categoryServices.apply(towerSemantics.getTop(),
                                    MonadServices.logicalExpressionToMonad(application)));
                    return Category.create(complexBase, newSemantics);
                }
            } else if (towerSyntax.getBase() instanceof TowerSyntax) {
                TowerCategory<LogicalExpression> base =
                        (TowerCategory<LogicalExpression>) getBase(toLower);
                Category<LogicalExpression> loweredBase = lower(base);
                Syntax baseLeft = base.getSyntax().getLeft();
                Tower baseTowerSemantics = (Tower) base.getSemantics();
                if (baseLeft instanceof TowerSyntax) {
                    // case 4. sorry
                    assert(loweredBase instanceof TowerCategory);
                    Tower resultSemTower = (Tower) loweredBase.getSemantics();
                    Lambda resultSemLambda = towerToLambda(resultSemTower);
                    Variable lambdaVariable = new Variable(LogicLanguageServices
                        .getTypeRepository().generalizeType(
                                resultSemLambda.getComplexType().getDomain()));
                    // Apply the base to the new lambda variable
                    LogicalExpression application = categoryServices.apply(
                            resultSemLambda, lambdaVariable);
                    // Apply tower top to new expression
                    Lambda newSemantics = new Lambda(lambdaVariable,
                            categoryServices.apply(towerSemantics.getTop(),
                                    application));
                    // Result needs to be a tower
                    Tower newSemanticsTower = lambdaToTower(newSemantics);
                    return Category.create(loweredBase.getSyntax(),
                            newSemanticsTower);
                } else {
                    // case 3
                    LogicalExpression newSemantics =
                            categoryServices.apply(towerSemantics.getTop(), loweredBase.getSemantics());
                    if (newSemantics instanceof Monad) {
                        Monad monadSemantics = (Monad) newSemantics;
                        StateMonadParams execResults = (StateMonadParams) monadSemantics.exec(new StateMonadParams());
                        newSemantics = new StateMonad(execResults.getOutput(), execResults.getState());
                    }
                    return Category.create(loweredBase.getSyntax(), newSemantics);
                }
            }
        }
        return null;
    }
}
