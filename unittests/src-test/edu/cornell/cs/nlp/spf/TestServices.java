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
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import edu.cornell.cs.nlp.spf.ccg.categories.ITowerCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.*;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.LogicalExpressionCategoryServices;
import edu.cornell.cs.nlp.spf.mr.lambda.ccg.TowerCategoryServices;
import edu.cornell.cs.nlp.spf.mr.language.type.TypeRepository;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.IRecursiveBinaryParseRule;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.BackwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.application.ForwardApplication;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.BackwardComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.primitivebinary.composition.ForwardComposition;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.combination.Combination;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftLeft;
import edu.cornell.cs.nlp.spf.parser.ccg.rules.recursive.lift.LiftRight;

public class TestServices {

	public static final LogicalExpressionCategoryServices	CATEGORY_SERVICES;
	public static final TowerCategoryServices				TOWER_CATEGORY_SERVICES;

	public static final List<File>							DEFAULT_ONTOLOGY_FILES;
	public static final File								DEFAULT_TYPES_FILE;

	public static final List<IBinaryParseRule<LogicalExpression>> BASE_RULES;
	public static final List<IRecursiveBinaryParseRule<LogicalExpression>> RECURSIVE_RULES;

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
		TOWER_CATEGORY_SERVICES = new TowerCategoryServices(true);

		BASE_RULES = new ArrayList<>();
		BASE_RULES.add(new ForwardApplication<>(CATEGORY_SERVICES));
		BASE_RULES.add(new BackwardApplication<>(CATEGORY_SERVICES));
		BASE_RULES.add(new ForwardComposition<>(CATEGORY_SERVICES, 1, false));
		BASE_RULES.add(new BackwardComposition<>(CATEGORY_SERVICES, 1, false));

		RECURSIVE_RULES = new ArrayList<>();
		RECURSIVE_RULES.add(new Combination<>("C", TOWER_CATEGORY_SERVICES, BASE_RULES));
		RECURSIVE_RULES.add(new LiftLeft<>("^", TOWER_CATEGORY_SERVICES, BASE_RULES));
		RECURSIVE_RULES.add(new LiftRight<>("^", TOWER_CATEGORY_SERVICES, BASE_RULES));
	}

	public static LogicalExpressionCategoryServices getCategoryServices() {
		return CATEGORY_SERVICES;
	}

	public static void init() {
		// Nothing to do.
	}

	public static ITowerCategoryServices<LogicalExpression> getTowerCategoryServices() {
	    return TOWER_CATEGORY_SERVICES;
	}

	public static List<IBinaryParseRule<LogicalExpression>> getBaseRules() {
		return BASE_RULES;
	}

	public static List<IRecursiveBinaryParseRule<LogicalExpression>> getRecursiveRules() {
		return RECURSIVE_RULES;
	}
}
