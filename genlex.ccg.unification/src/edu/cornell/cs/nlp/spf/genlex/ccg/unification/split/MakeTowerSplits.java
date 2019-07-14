package edu.cornell.cs.nlp.spf.genlex.ccg.unification.split;

import edu.cornell.cs.nlp.spf.ccg.categories.*;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.ComplexSyntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Slash;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.Syntax;
import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.genlex.ccg.unification.split.SplittingServices.SplittingPair;
import edu.cornell.cs.nlp.spf.mr.lambda.Binding;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;
import edu.cornell.cs.nlp.spf.mr.lambda.Lambda;
import edu.cornell.cs.nlp.spf.mr.lambda.LogicalExpression;
import edu.cornell.cs.nlp.utils.composites.Pair;
import edu.cornell.cs.nlp.utils.log.ILogger;
import edu.cornell.cs.nlp.utils.log.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class MakeTowerSplits {

    public static final ILogger LOG	= LoggerFactory
                                        .create(MakeTowerSplits.class);
    private MakeTowerSplits() {
    }

    static Set<SplittingPair> ofNew(Category<LogicalExpression> originalCategory,
                                    ICategoryServices<LogicalExpression> categoryServices,
                                    ITowerCategoryServices<LogicalExpression> towerCategoryServices) {
        Set<SplittingPair> ret = new HashSet<>();
        if (!(originalCategory instanceof TowerCategory)) {
            // ideally this would allow for other kinds of splits
            Set<SplittingPair> apps = MakeApplicationSplits.of(originalCategory, categoryServices);
            Set<SplittingPair> comps = MakeCompositionSplits.of(originalCategory, categoryServices);
            ret.addAll(apps);
            ret.addAll(comps);

            Set<Pair<Lambda, LogicalExpression>> splitSemantics = new HashSet<>();

            for (SplittingPair pair : apps) {
                // maybe separate into different function?
                Syntax leftSyntax = pair.left.getSyntax();
                Syntax rightSyntax = pair.right.getSyntax();
                if (leftSyntax instanceof ComplexSyntax &&
                        ((ComplexSyntax) leftSyntax).getRight().equals(rightSyntax) &&
                        pair.left.getSemantics() instanceof Lambda) {
                    splitSemantics.add(Pair.of((Lambda) pair.left.getSemantics(), pair.right.getSemantics()));
                }
            }

            // Each application split gives us a tower.
            for (Pair<Lambda, LogicalExpression> semPair : splitSemantics) {
                Lambda top = semPair.first();
                LogicalExpression bottom = semPair.second();
            }
        }
        return Collections.emptySet();
    }

    // TODO: make it nicer =(
    static Set<SplittingPair> of(Category<LogicalExpression> originalCategory,
                                 ICategoryServices<LogicalExpression> categoryServices,
                                 ITowerCategoryServices<LogicalExpression> towerCategoryServices) {
        if (!(originalCategory instanceof TowerCategory)) {
            return Collections.emptySet();
        }

        final TowerCategory<LogicalExpression> towerCategory = (TowerCategory<LogicalExpression>) originalCategory;

        // 1. Split the top. This should be a composition split - the hole variable should end up attached to the right split.
        //    Actually, this might help with restrictions - only expressions with the hole variable can be extracted.`
        // 2. Split the bottom. (Potentially recursive. Watch out!)
        // 3. The possible tower splits can be created by using these splits, as far as I can tell.
        final TowerSyntax towerSyntax = towerCategory.getSyntax();
        final Syntax towerLeftSyntax = towerSyntax.getLeft();
        final Syntax towerRightSyntax = towerSyntax.getRight();

        final Tower towerSemantics = (Tower) towerCategory.getSemantics();
        final Lambda towerTop = towerSemantics.getTop();

        final ComplexCategory<LogicalExpression> topCategory = (ComplexCategory<LogicalExpression>) Category.create(
                new ComplexSyntax(
                        towerLeftSyntax,
                        towerRightSyntax,
                        Slash.FORWARD
                ),
                (LogicalExpression) towerTop
        );

        final Set<SplittingPair> topSplits = MakeCompositionSplits.of(topCategory, categoryServices);

        final Syntax bottomSyntax = towerSyntax.getBase();
        final Category<LogicalExpression> bottomCategory = Category.create(bottomSyntax, towerSemantics.getBottom());

        final TowerSplitter splitter =
                new TowerSplitter(categoryServices, towerCategoryServices);
        // Split the base with no lowering operations.
        final Set<SplittingPair> bottomSplits = splitter.getRecursiveSplits(bottomCategory);

        Set<SplittingPair> ret = new HashSet<>();
        for (SplittingPair top : topSplits) {
            // Turn the top splitting pairs into tower tops
            ComplexCategory<LogicalExpression> left = (ComplexCategory<LogicalExpression>) top.getLeft();
            ComplexCategory<LogicalExpression> right = (ComplexCategory<LogicalExpression>) top.getRight();

            ComplexSyntax leftSyn = left.getSyntax();
            ComplexSyntax rightSyn = right.getSyntax();

            Syntax leftRes = leftSyn.getLeft();
            Syntax leftArg = leftSyn.getRight();

            Syntax rightRes = rightSyn.getLeft();
            Syntax rightArg = rightSyn.getRight();

            Lambda leftSem = (Lambda) left.getSemantics();
            Lambda rightSem = (Lambda) right.getSemantics();

            for (SplittingPair bot : bottomSplits) {
                // Tower combination
                Category<LogicalExpression> botLeft = bot.getLeft();
                Category<LogicalExpression> botRight = bot.getRight();

                if (!(leftSem.getArgument().equals(leftSem.getBody()) ||
                        rightSem.getArgument().equals(rightSem.getBody()))) {
                    TowerSyntax leftTowerSyn = new TowerSyntax(botLeft.getSyntax(), leftRes, leftArg);
                    Tower leftTowerSem = new Tower(leftSem, botLeft.getSemantics());
                    TowerCategory<LogicalExpression> leftTower = new TowerCategory<>(leftTowerSyn, leftTowerSem);

                    TowerSyntax rightTowerSyn = new TowerSyntax(botRight.getSyntax(), rightRes, rightArg);
                    Tower rightTowerSem = new Tower(rightSem, botRight.getSemantics());
                    TowerCategory<LogicalExpression> rightTower = new TowerCategory<>(rightTowerSyn, rightTowerSem);

                    ret.add(new SplittingPair(leftTower, rightTower));
                }

                // Lift left
                TowerSyntax bigRightTowerSyn = new TowerSyntax(botRight.getSyntax(), towerLeftSyntax, towerRightSyntax);
                Tower bigRightTowerSem = new Tower(towerTop, botRight.getSemantics());
                TowerCategory<LogicalExpression> bigRightTower = new TowerCategory<>(bigRightTowerSyn, bigRightTowerSem);
                ret.add(new SplittingPair(botLeft, bigRightTower));

                // Monadic lift left
                if (leftSem.getBody() instanceof Binding) {
                    Binding leftBinding = (Binding) leftSem.getBody();
                    if (leftBinding.getRight().equals(leftSem.getArgument()) &&
                            leftBinding.getVariable().equals(botLeft.getSemantics()) &&
                            leftRes.equals(leftArg)) {
                        ret.add(new SplittingPair(
                                Category.create(botLeft.getSyntax(), leftBinding.getLeft()),
                                bigRightTower));
                    }
                }

                // Lift right
                TowerSyntax bigLeftTowerSyn = new TowerSyntax(botLeft.getSyntax(), towerLeftSyntax, towerRightSyntax);
                Tower bigLeftTowerSem = new Tower(towerTop, botLeft.getSemantics());
                TowerCategory<LogicalExpression> bigLeftTower = new TowerCategory<>(bigLeftTowerSyn, bigLeftTowerSem);
                ret.add(new SplittingPair(bigLeftTower, botRight));

                // Monadic lift right
                if (rightSem.getBody() instanceof Binding) {
                    Binding rightBinding = (Binding) rightSem.getBody();
                    if (rightBinding.getRight().equals(rightSem.getArgument()) &&
                            rightBinding.getVariable().equals(botRight.getSemantics()) &&
                            rightRes.equals(rightArg)) {
                        ret.add(new SplittingPair(
                                bigLeftTower,
                                Category.create(botRight.getSyntax(), rightBinding.getLeft())));
                    }
                }
            }
        }
        return ret;
    }

}
