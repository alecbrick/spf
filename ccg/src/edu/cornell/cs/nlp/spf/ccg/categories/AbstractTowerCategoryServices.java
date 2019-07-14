package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;

public abstract class AbstractTowerCategoryServices<MR> implements ITowerCategoryServices<MR> {
    public Category<MR> getBase(Category<MR> cat) {
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
