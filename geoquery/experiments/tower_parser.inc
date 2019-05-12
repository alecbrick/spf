# Parsing rules
type=rule.composition id=ruleComp
type=rule.application id=ruleApp
type=rule.shifting.pp id=shiftPP
type=rule.typeraise.composition.forward id=ruleRaiseComp
type=rule.thatless id=ruleThatless
type=rule.shift.pluralexists id=rulePluralExists
# Recursive parsing
type=rule.baserules id=baseRules rules=ruleComp,ruleApp,ruleRaiseComp,ruleThatless,rulePluralExists,shiftPP
type=rule.recursive.combination id=ruleCombine baseRules=baseRules
type=rule.recursive.lift.left id=ruleLiftLeft baseRules=baseRules
type=rule.recursive.lift.right id=ruleLiftRight baseRules=baseRules
type=rule.tower id=ruleTower rules=ruleCombine,ruleLiftLeft,ruleLiftRight
# NF constraints
type=nf.eisner id=nfValidator
# Parse filter
type=parsefilter.lambda.simple id=parseFilter syntax=S
# CKY Parser
type=parser.cky.multi id=parser parseFilter=parseFilter beam=%{gBeam} preChartPruning=true pruneLexicalCells=true rules=ruleComp,ruleApp,ruleRaiseComp,ruleThatless,rulePluralExists,shiftPP,ruleTower executor=executor nfValidator=nfValidator wordSkipping=simple
# Parsing filter
type=parser.filter.supervised id=filterFactory
