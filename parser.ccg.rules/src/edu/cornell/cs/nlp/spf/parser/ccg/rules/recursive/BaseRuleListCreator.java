package edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive;

import edu.cornell.cs.nlp.spf.explat.IResourceRepository;
import edu.cornell.cs.nlp.spf.explat.ParameterizedExperiment;
import edu.cornell.cs.nlp.spf.explat.resources.IResourceObjectCreator;
import edu.cornell.cs.nlp.spf.explat.resources.usage.ResourceUsage;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.BinaryRuleSet;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;

import java.util.ArrayList;
import java.util.List;

public class BaseRuleListCreator<MR> implements
        IResourceObjectCreator<BinaryRuleSet<MR>> {

    private String	type;

    public BaseRuleListCreator() {
        this("rule.baserules");
    }

    public BaseRuleListCreator(String type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    @Override
    public BinaryRuleSet<MR> create(ParameterizedExperiment.Parameters params, IResourceRepository repo) {
        List<IBinaryParseRule<MR>> baseRules = new ArrayList<>();
        for (final String id : params.getSplit("rules")) {
            final Object rule = repo.get(id);
            if (rule instanceof IBinaryParseRule) {
                baseRules.add((IBinaryParseRule) rule);
            } else if (rule instanceof BinaryRuleSet) {
                for (IBinaryParseRule<MR> r : (BinaryRuleSet<MR>) rule) {
                    baseRules.add(r);
                }
            }
            // TODO: Unary rules? Need to handle those somehow.
        }
        return new BinaryRuleSet<>(baseRules);
    }

    @Override
    public String type() {
        return type;
    }

    @Override
    public ResourceUsage usage() {
        return ResourceUsage.builder(type, BaseRuleListCreator.class)
                .addParam("rules", IBinaryParseRule.class,
                    "Binary parsing rules.")
                .build();
    }
}
