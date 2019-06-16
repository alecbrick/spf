package edu.cornell.cs.nlp.spf.mr.lambda.ccg;

import edu.cornell.cs.nlp.spf.ccg.categories.*;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.visitor.LambdaWrapped;
import edu.cornell.cs.nlp.spf.mr.language.type.Type;


public class TowerCategoryServices extends AbstractTowerCategoryServices<LogicalExpression> {
    private final ICategoryServices<LogicalExpression> categoryServices;

    public TowerCategoryServices() {
        this(false);
    }

    public TowerCategoryServices(boolean doTypeChecking) {
        this.categoryServices = new LogicalExpressionCategoryServices(doTypeChecking);
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

    // Raise a category into a continuation tower.
    public TowerCategory<LogicalExpression> raise(
            Category<LogicalExpression> toRaise) {
        final Syntax syn = toRaise.getSyntax();
        final LogicalExpression sem = toRaise.getSemantics();

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

        // Some monadic stuff. Might need it later.
        /*
        TowerSyntax towerSyntax = toLower.getSyntax();
        Tower towerSemantics =
                (Tower) toLower.getSemantics();
        if (towerSyntax.getBase().equals(Syntax.S) &&
                towerSyntax.getRight().equals(Syntax.S)) {
            // Case 1
            Syntax newSyntax = towerSyntax.getLeft();
            LogicalExpression newSemantics = categoryServices.apply(
                    towerSemantics.getTop(), towerSemantics.getBottom()
            );
            return Category.create(newSyntax, newSemantics);
        } else if (towerSyntax.getLeft().equals(Syntax.S) &&
                towerSyntax.getRight().equals(Syntax.S)) {
            if (towerSyntax.getBase() instanceof ComplexSyntax) {
                ComplexSyntax complexBase =
                        (ComplexSyntax) towerSyntax.getBase();
                if (complexBase.getLeft().equals(Syntax.S)) {
                    // Case 2
                    Lambda newSemantics = (Lambda) categoryServices.compose(
                            towerSemantics.getTop(),
                            towerSemantics.getBottom(),
                            1
                    );
                    return new ComplexCategory<>(complexBase, newSemantics);
                }
            } else if (towerSyntax.getBase() instanceof TowerSyntax) {
                TowerSyntax towerBase =
                        (TowerSyntax) towerSyntax.getBase();
            }
        }
        return null;
        */
    }
}
