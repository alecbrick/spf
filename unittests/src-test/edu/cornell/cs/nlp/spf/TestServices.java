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
package edu.cornell.cs.nlp.spf;

import java.io.File;
import java.io.IOException;
import java.util.*;

import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.MonadCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.TowerCategoryServices;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.*;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.BackwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.application.ForwardReversibleApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleCombination;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleLiftLeft;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.lambda.tower.ReversibleLiftRight;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.BackwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.BackwardComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.ForwardComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.combination.Combination;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftLeft;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftRight;
import edu.cornell.cs.nlp.utils.log.Log;

public class TestServices {

	public static final LogicalExpressionCategoryServices	CATEGORY_SERVICES;
	public static final TowerCategoryServices				TOWER_CATEGORY_SERVICES;
	public static final MonadCategoryServices MONAD_SERVICES;

	public static final List<File>							DEFAULT_ONTOLOGY_FILES;
	public static final File								DEFAULT_TYPES_FILE;

	public static final BinaryRuleSet<LogicalExpression> BASE_RULES;
	public static final List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> RECURSIVE_RULES;
	public static final List<IBinaryReversibleParseRule<LogicalExpression>> BASE_REVERSIBLE_RULES;

	private TestServices() {
		// Private ctor. Nothing to init. Service class.
	}


	static {
		DEFAULT_TYPES_FILE = new File("resources-test/geo.types");
		DEFAULT_ONTOLOGY_FILES = new LinkedList<File>();
		DEFAULT_ONTOLOGY_FILES.add(new File("resources-test/geo.consts.ont"));
		DEFAULT_ONTOLOGY_FILES.add(new File("resources-test/geo.preds.ont"));

		// //////////////////////////////////////////
		// Init typing system.
		// //////////////////////////////////////////

		// Init the logical expression type system
		try {
			LogicLanguageServices
					.setInstance(new LogicLanguageServices.Builder(
							new TypeRepository(DEFAULT_TYPES_FILE),
							new FlexibleTypeComparator()).closeOntology(false)
							.setUseOntology(true)
							.addConstantsToOntology(DEFAULT_ONTOLOGY_FILES)
							.setNumeralTypeName("i").build());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		// //////////////////////////////////////////////////
		// Init system for skolem terms IDs.
		// //////////////////////////////////////////////////

		SkolemServices.setInstance(new SkolemServices.Builder(
				LogicLanguageServices.getTypeRepository().getType("id"),
				LogicalConstant.read("na:id")).build());

		// //////////////////////////////////////////////////
		// Category services for logical expressions.
		// //////////////////////////////////////////////////

		// CCG LogicalExpression category services for handling categories
		// with LogicalExpression as semantics
		CATEGORY_SERVICES = new LogicalExpressionCategoryServices(true);
		MONAD_SERVICES = new MonadCategoryServices();
		TOWER_CATEGORY_SERVICES = new TowerCategoryServices(true);

		// //////////////////////////////////////////////////
		// Initialize monad services
		// //////////////////////////////////////////////////
		MonadServices.setInstance(new MonadServices.Builder().build());

		List<IBinaryParseRule<LogicalExpression>> baseRules = new ArrayList<>();
		baseRules.add(new ForwardApplication<>(CATEGORY_SERVICES));
		baseRules.add(new BackwardApplication<>(CATEGORY_SERVICES));
		baseRules.add(new ForwardComposition<>(CATEGORY_SERVICES, 1, false));
		baseRules.add(new BackwardComposition<>(CATEGORY_SERVICES, 1, false));
		BASE_RULES = new BinaryRuleSet<>(baseRules);

		List<IBinaryReversibleParseRule<LogicalExpression>> baseReversibleRules = new ArrayList<>();
		Set<String> attributes = new HashSet<>();
		attributes.add("sg");
		attributes.add("pl");
		ForwardReversibleApplication forwardApp = new ForwardReversibleApplication(
				CATEGORY_SERVICES, 3, 9, true, attributes);
		baseReversibleRules.add(forwardApp);
		baseReversibleRules.add(new BackwardReversibleApplication(
				CATEGORY_SERVICES, 3, 9, true, attributes));
		BASE_REVERSIBLE_RULES = baseReversibleRules;


		RECURSIVE_RULES = new ArrayList<>();
		RECURSIVE_RULES.add(new ReversibleCombination("C", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));
		RECURSIVE_RULES.add(new ReversibleLiftLeft("^", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));
		RECURSIVE_RULES.add(new ReversibleLiftRight("^", TOWER_CATEGORY_SERVICES, BASE_RULES, new HashSet<>(baseReversibleRules), forwardApp));

		for (IBinaryReversibleRecursiveParseRule<LogicalExpression> rule : RECURSIVE_RULES) {
			for (IBinaryReversibleRecursiveParseRule<LogicalExpression> toAdd : RECURSIVE_RULES) {
				rule.addRecursiveRule(toAdd);
			}
		}
	}

	public static LogicalExpressionCategoryServices getCategoryServices() {
		return CATEGORY_SERVICES;
	}

	public static void init() {
		// Nothing to do.
	}

	public static TowerCategoryServices getTowerCategoryServices() {
	    return TOWER_CATEGORY_SERVICES;
	}

	public static MonadCategoryServices getMonadServices() {
		return MONAD_SERVICES;
	}

	public static BinaryRuleSet<LogicalExpression> getBaseRules() {
		return BASE_RULES;
	}

	public static List<IBinaryReversibleRecursiveParseRule<LogicalExpression>> getRecursiveRules() {
		return RECURSIVE_RULES;
	}

	public static List<IBinaryReversibleParseRule<LogicalExpression>> getBaseReversibleRules() {
		return BASE_REVERSIBLE_RULES;
	}
}
