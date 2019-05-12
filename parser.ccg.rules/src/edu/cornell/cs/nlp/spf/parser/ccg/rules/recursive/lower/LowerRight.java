package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lower;

import edu.cornell.cs.nlp.spf.ccg.categories.Category;
import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.ccg.categories.TowerCategory;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;

import java.util.ArrayList;
import java.util.List;

public class LowerRight<MR> extends AbstractLower<MR> {

    public LowerRight(String label, ITowerCategoryServices<MR> towerCategoryServices,
                     BinaryRuleSet<MR> baseRules) {
        super(label + "Right", towerCategoryServices, baseRules);
    }

    // All left-lowering-specific logic goes here
    public List<ParseRuleResult<MR>> applyRecursive(
            Category<MR> left,
            Category<MR> right,
            SentenceSpan span,
            List<IRecursiveBinaryParseRule<MR>> validRules) {
        // Right must be a tower
        if (!(right instanceof TowerCategory)) {
            return new ArrayList<>();
        }
        TowerCategory<MR> rightTower = (TowerCategory<MR>) right;
        Category<MR> loweredRight = towerCategoryServices.lower(rightTower);
        System.out.println(left);
        System.out.println(loweredRight);

        List<ParseRuleResult<MR>> results =
                combineRecursive(left, loweredRight, span, validRules);
        List<ParseRuleResult<MR>> ret = new ArrayList<>();
        for (ParseRuleResult<MR> result : results) {
            Category<MR> resultCategory = result.getResultCategory();
            RuleName newRuleName = createRuleName(result);
            ret.add(new ParseRuleResult<>(newRuleName, resultCategory));
        }
        return ret;
    }
}
