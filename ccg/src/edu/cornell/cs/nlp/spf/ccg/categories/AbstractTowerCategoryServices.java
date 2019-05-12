package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;

public abstract class AbstractTowerCategoryServices<MR> implements ITowerCategoryServices<MR> {
    public Category<MR> getBase(TowerCategory<MR> tower) {
        TowerSyntax syntax = tower.getSyntax();
        MR bottom = getBottomSemantics(tower);
        return Category.create(syntax.getBase(), bottom);
    }

}
