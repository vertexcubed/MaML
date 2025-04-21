package vertexcubed.maml.parse

import vertexcubed.maml.ast.AppNode
import vertexcubed.maml.ast.AstNode
import vertexcubed.maml.ast.ModuleStructNode
import vertexcubed.maml.ast.VariableNode
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.parsers.*
import vertexcubed.maml.parse.preprocess.Associativity
import vertexcubed.maml.parse.preprocess.InfixRule
import java.util.*

class ParseEnv() {

    val infixMap = mutableMapOf<Int, Pair<Associativity, MutableList<String>>>()
    val externalFuncs = arrayListOf<String>()
    val modules = mutableMapOf<String, ModuleStructNode>()

    //default values
    fun init() {
        infixMap[0] = Pair(Associativity.RIGHT, arrayListOf("||"))
        infixMap[1] = Pair(Associativity.RIGHT, arrayListOf("&&"))
        infixMap[2] = Pair(Associativity.LEFT, arrayListOf("=", "!=", "<=", ">=", "<", ">"))
        infixMap[3] = Pair(Associativity.LEFT, arrayListOf("+", "-"))
        infixMap[4] = Pair(Associativity.LEFT, arrayListOf("*", "/", "%"))
    }


    fun addAllFrom(other: ParseEnv) {
        infixMap.putAll(other.infixMap)
        externalFuncs.addAll(other.externalFuncs)
        modules.putAll(other.modules)
    }

    fun copy(): ParseEnv {
        val ret = ParseEnv()
        ret.infixMap.putAll(infixMap)
        ret.externalFuncs.addAll(externalFuncs)
        ret.modules.putAll(modules)
        return ret
    }

    fun addModule(module: ModuleStructNode) {
        modules[module.name] = module
    }

    fun lookupModule(binding: MIdentifier): Optional<ModuleStructNode> {
        var lastEnv = this
        for(i in binding.path.indices) {
            val cur = lastEnv.modules[binding.path[i]]
            if(cur != null) {
                lastEnv = cur.parseEnv
            }
            else {
                return Optional.empty()
            }
            if(i == binding.path.lastIndex) {
                return Optional.of(cur)
            }
        }
        throw AssertionError("Should not happen!")
    }

    fun allExternalFuncs(): List<MIdentifier> {
        val ret = arrayListOf<MIdentifier>()
        for((name, mod) in modules) {
            val modFuncs = mod.parseEnv.allExternalFuncs()
            ret.addAll(modFuncs.map { iden -> MIdentifier(listOf(name) + iden.path) })
        }
        ret.addAll(externalFuncs.map { s -> MIdentifier(s) })
        return ret
    }

    fun addExternalFunc(name: String) {
        externalFuncs.add(name)
    }

    fun addInfixRule(rule: InfixRule) {
        val (assoc, names) = infixMap.computeIfAbsent(rule.precedence, { _ -> Pair(rule.assoc, arrayListOf())})
        if(rule.assoc != assoc) throw IllegalArgumentException("Cannot have multiple associativities of same precedence level!")
        if(rule.assoc == Associativity.NONE && names.size > 0) throw IllegalArgumentException("Only one nonfix operator per precedence level!")
        names.add(rule.name)
    }

    fun allStrings(): List<String> {
        val out = ArrayList<String>()
        for((_, v) in infixMap) {
            out.addAll(v.second)
        }
        return out
    }

    fun choiceNameParsers(): Parser<MIdentifier> {
        val list = ArrayList<Parser<MIdentifier>>()
        for((name, mod) in modules) {
            list.add(SpecificConstructorParser(name).lCompose(SpecialCharParser(".")).bind { first ->
                mod.parseEnv.choiceNameParsers().map { second ->
                    MIdentifier(listOf(first) + second.path)
                }
            })
        }
        val sortedMap = infixMap.toSortedMap().reversed()


        for((_, v) in sortedMap) {
            val (_, names) = v
            val nameParser = ChoiceParser(names.map { name ->
                CompoundSpecialCharParser(name).disjoint(SpecificIdentifierParser(name))
            })
            list.add(nameParser.map { str -> MIdentifier(str) })
        }
        return ChoiceParser(list)

    }

    fun infixParser(base: Parser<AstNode>): Parser<AstNode> {
        val sortedMap = infixMap.toSortedMap().reversed()
        val parserList = arrayListOf(base)
        var i = 0
        for((_, v) in sortedMap) {
            val (assoc, names) = v
            val nameParser = ChoiceParser(names.map { name ->
                CompoundSpecialCharParser(name).disjoint(SpecificIdentifierParser(name))
            })
            //Need to use a list because closures are weird.
            parserList.add(newParser(assoc, nameParser, parserList, i))
            i++
        }
        return parserList.last
    }

    /**
     * Need to move to helper function due to function closure weirdness. "i" needs to be the same within the bind anonymous function and within the initial call
     */
    private fun newParser(assoc: Associativity, nameParser: Parser<String>, list: List<Parser<AstNode>>, i: Int): Parser<AstNode> {
        return list[i].bind { first ->
            ZeroOrMore(AndParser(nameParser, list[i])).map { secondList ->
                if (secondList.isEmpty()) {
                    first
                } else {
                    if (assoc == Associativity.NONE && secondList.size > 1) throw ParseException(
                        first.line,
                        "Cannot chain ${secondList[0].first} operations without parentheses"
                    )

                    when (assoc) {
                        Associativity.NONE -> {
                            AppNode(AppNode(VariableNode(secondList[0].first, secondList[0].second.line), first, first.line), secondList[0].second, secondList[0].second.line)
                        }

                        Associativity.LEFT -> {
                            leftAssocParser(first, secondList)
                        }

                        Associativity.RIGHT -> {
                            rightAssocParser(first, secondList)
                        }
                    }
                }
            }
        }
    }



    //this shoooooould work?
    fun leftAssocParser(first: AstNode, secondList: List<Pair<String, AstNode>>): AstNode {

        var node = AppNode(AppNode(VariableNode(secondList[0].first, secondList[0].second.line), first, first.line), secondList[0].second, secondList[0].second.line)
        for(i in 1..<secondList.size) {
            val (op, next) = secondList[i]
            node = AppNode(AppNode(VariableNode(op, next.line), node, node.line), next, next.line)
        }
        return node
    }

    //shoooould also work?
    fun rightAssocParser(first: AstNode, secondList: List<Pair<String, AstNode>>): AstNode {
        //Convert 2, [(+ 5), (+ 3), (+ 7)] into [(2 +), (5 +), (3 +)], 7
        val newList = ArrayList<Pair<AstNode, String>>()
        newList.add(Pair(first, secondList[0].first))
        for(i in 1..<secondList.size) {
            newList.add(Pair(secondList[i-1].second, secondList[i].first))
        }


        //Do same thing as left associative
        val lastIdx = secondList.lastIndex
        val last = secondList[lastIdx].second
//        var node = BinaryOpNode(newList[lastIdx].second, newList[lastIdx].first, last, tokens[index].line)
        var node = AppNode(AppNode(VariableNode(newList[lastIdx].second, newList[lastIdx].first.line), newList[lastIdx].first, newList[lastIdx].first.line), last, last.line)


        for(i in 1..<newList.size) {
            val (next, op) = newList[i]
//            node = BinaryOpNode(pair.second, pair.first, node, tokens[index].line)
            node = AppNode(AppNode(VariableNode(op, next.line), next, next.line), node, node.line)
        }

        return node
    }
}