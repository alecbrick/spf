package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftLeft;

import java.util.ArrayList;
import java.util.List;

public class LowerLeft<MR> extends AbstractLower<MR> {

    public LowerLeft(String label, ITowerCategoryServices<MR> towerCategoryServices,
                    BinaryRuleSet<MR> baseRules) {
        super(label + "Left", towerCategoryServices, baseRules);
    }

    // All left-lowering-specific logic goes here
    public List<ParseRuleResult<MR>> applyRecursive(
            Category<MR> left,
            Category<MR> right,
            SentenceSpan span,
            List<IBinaryRecursiveParseRule<MR>> validRules) {
        // Left must be a tower
        if (!(left instanceof TowerCategory)) {
            return new ArrayList<>();
        }
        if (!towerCategoryServices.hasMonadicBaseArg(right)) {
            return new ArrayList<>();
        }
        TowerCategory<MR> leftTower = (TowerCategory<MR>) left;
        Category<MR> loweredLeft = towerCategoryServices.lower(leftTower);
        if (loweredLeft == null) {
            return new ArrayList<>();
        }
        System.out.println(loweredLeft);

        List<IBinaryRecursiveParseRule<MR>> newValidRules =
                new ArrayList<>(validRules);
        newValidRules.removeIf(r -> r instanceof LiftLeft);

        List<ParseRuleResult<MR>> results =
                combineRecursive(loweredLeft, right, span, newValidRules);
        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        for (ParseRuleResult<MR> result : results) {
            Category<MR> resultCategory = result.getResultCategory();
            RuleName newRuleName = createRuleName(result);
            ret.add(new ParseRuleResult<>(newRuleName, resultCategory));
        }
        return ret;
    }
}
