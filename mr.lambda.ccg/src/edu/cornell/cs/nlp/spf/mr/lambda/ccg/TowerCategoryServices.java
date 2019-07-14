package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.ccg.categories.*;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.GetAllLiterals;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.LambdaWrapped;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.ReplaceExpression;
import edu.cornell.cs.nlp.spf.mr.language.type.ComplexType;
import edu.cornell.cs.nlp.spf.mr.language.type.MonadType;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic.IMonadServices;

import java.util.List;


public class TowerCategoryServices extends AbstractTowerCategoryServices<LogicalExpression> {
    private final ICategoryServices<LogicalExpression> categoryServices;
    private final IMonadServices<LogicalExpression, Monad> monadServices;

    public TowerCategoryServices() {
        this(false, new MonadServices());
    }

    public TowerCategoryServices(boolean doTypeChecking, IMonadServices monadServices) {
        this.categoryServices = new LogicalExpressionCategoryServices(doTypeChecking);
        this.monadServices = monadServices;
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
        // We can lower when right and base are S
        if (towerSyntax.getBase().equals(Syntax.S)) {
            if (towerSyntax.getRight().equals(Syntax.S)) {
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

    // TODO: What if it's not a state monad?
    public Category<LogicalExpression> monadicLower(
            TowerCategory<LogicalExpression> toLower) {
        TowerSyntax towerSyntax = toLower.getSyntax();
        Tower towerSemantics =
                (Tower) toLower.getSemantics();
        if (towerSyntax.getBase().equals(Syntax.S) &&
                towerSyntax.getRight().equals(Syntax.S)) {
            // Case 1
            Syntax newSyntax = towerSyntax.getLeft();
            LogicalExpression newSemantics = categoryServices.apply(
                    towerSemantics.getTop(),
                    monadServices.logicalExpressionToMonad(towerSemantics.getBottom()));
            if (newSemantics instanceof Monad) {
                newSemantics = ((Monad) newSemantics).exec(new StateMonadParams()).getOutput();
            }
            if (newSyntax instanceof TowerSyntax && newSemantics instanceof Lambda) {
                newSemantics = lambdaToTower((Lambda) newSemantics);
            }
            return Category.create(newSyntax, newSemantics);
        } else if (towerSyntax.getLeft().equals(Syntax.S) &&
                towerSyntax.getRight().equals(Syntax.S)) {
            if (towerSyntax.getBase() instanceof ComplexSyntax) {
                ComplexSyntax complexBase =
                        (ComplexSyntax) towerSyntax.getBase();
                if (complexBase.getLeft().equals(Syntax.S)) {
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
                                    monadServices.logicalExpressionToMonad(application)));
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
                    return Category.create(loweredBase.getSyntax(),
                            categoryServices.apply(
                                    towerSemantics.getTop(),
                                    loweredBase.getSemantics()
                            ));
                }
            }
        }
        return null;
    }
}
