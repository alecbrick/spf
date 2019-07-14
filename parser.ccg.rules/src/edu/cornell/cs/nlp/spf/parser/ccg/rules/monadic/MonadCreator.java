/*******************************************************************************
 * Copyright (C) 2011 - 2015 Yoav Artzi, All rights reserved.
 * <p>
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * <p>
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package edu.cornell.cs.nlp.spf.parser.ccg.rules.monadic;

import com.google.common.reflect.Parameter;
import edu.cornell.cs.nlp.spf.ccg.categories.ICategoryServices;
import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment.Parameters;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.AbstractApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.BackwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;

import java.util.ArrayList;
import java.util.List;

public class MonadCreator<MR> implements
		IResourceObjectCreator<BinaryRuleSet<MR>> {
	
	private String	type;

	public MonadCreator() {
		this("rule.monadic");
	}

	public MonadCreator(String type) {
		this.type = type;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public BinaryRuleSet<MR> create(Parameters params, IResourceRepository repo) {
        List<IBinaryParseRule<MR>> monadRules = new ArrayList<>();
        for (final String id : params.getSplit("rules")) {
            final Object rule = repo.get(id);
            if (rule instanceof IBinaryParseRule) {
                monadRules.add(new MonadRule((IBinaryParseRule) rule,
                        repo.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE)));
            } else if (rule instanceof BinaryRuleSet) {
                for (IBinaryParseRule<MR> r : (BinaryRuleSet<MR>) rule) {
                    monadRules.add(new MonadRule(r,
                            repo.get(ParameterizedExperiment.CATEGORY_SERVICES_RESOURCE)));
                }
            }
        }
        return new BinaryRuleSet<>(monadRules);
	}
	
	@Override
	public String type() {
		return type;
	}
	
    @Override
    public ResourceUsage usage() {
        return ResourceUsage.builder(type, MonadCreator.class)
                .addParam("rules", IBinaryParseRule.class,
                    "Binary parsing rules.")
                .build();
    }
	
}
