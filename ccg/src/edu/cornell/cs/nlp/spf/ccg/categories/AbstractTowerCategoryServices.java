package edu.cornell.cs.nlp.spf.ccg.categories;

import edu.cornell.cs.nlp.spf.ccg.categories.syntax.TowerSyntax;
import edu.cornell.cs.nlp.spf.mr.lambda.Tower;

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

    public Category<MR> getBottom(Category<MR> cat) {
        if (!(cat instanceof TowerCategory)) {
            return cat;
        }
        TowerCategory<MR> towerCat = (TowerCategory) cat;
        return getBottom(getBase(towerCat));
    }

    public Category<MR> setBottom(Category<MR> cat, Category<MR> newBottom) {
        if (!(cat instanceof TowerCategory)) {
            return newBottom;
        }
        return setBase(cat, setBottom(getBase(cat), newBottom));
    }

    public boolean hasTowerResult(Category<MR> cat) {
        if (!(cat instanceof TowerCategory)) {
            return false;
        }
        TowerCategory<MR> tower = (TowerCategory<MR>) cat;
        TowerSyntax syntax = tower.getSyntax();
        if (syntax.getLeft() instanceof TowerSyntax) {
            return true;
        }
        return hasTowerResult(getBase(tower));
    }

}
