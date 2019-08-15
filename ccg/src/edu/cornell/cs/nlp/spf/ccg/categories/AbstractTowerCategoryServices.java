package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;

public abstract class AbstractTowerCategoryServices<MR> implements ITowerCategoryServices<MR> {
    public Category<MR> getBottom(Category<MR> cat) {
        if (cat instanceof TowerCategory) {
            TowerCategory<MR> tower = (TowerCategory<MR>) cat;
            TowerSyntax syntax = tower.getSyntax();
            MR bottom = getBottomSemantics(tower);
            return Category.create(syntax.getBase(), bottom);
        } else {
            return cat;
        }
    }



}
