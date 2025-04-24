package vertexcubed.maml.parse

import vertexcubed.maml.ast.*
import vertexcubed.maml.core.MIdentifier
import vertexcubed.maml.core.ParseException
import vertexcubed.maml.parse.parsers.*
import vertexcubed.maml.parse.preprocess.Associativity
import vertexcubed.maml.parse.preprocess.InfixRule
import java.util.*

class ParseEnv() {

    val infixMap = mutableMapOf<Int, Pair<Associativity, MutableList<String>>>()
    val modules = mutableMapOf<String, ModuleStructNode>()

    //default values
    fun init() {
        infixMap[0] = Associativity.RIGHT to arrayListOf("||")
        infixMap[1] = Associativity.RIGHT to arrayListOf("&&")
        infixMap[2] = Associativity.LEFT to arrayListOf("=", "!=", "<=", ">=", "<", ">")
        infixMap[3] = Associativity.RIGHT to arrayListOf("::")
        infixMap[4] = Associativity.LEFT to arrayListOf("+", "-")
        infixMap[5] = Associativity.LEFT to arrayListOf("*", "/", "%")
    }


    fun addAllFrom(other: ParseEnv) {
        infixMap.putAll(other.infixMap)
        modules.putAll(other.modules)
    }

    fun copy(): ParseEnv {
        val ret = ParseEnv()
        ret.infixMap.putAll(infixMap)
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
        val node = secondList.fold(first) { acc, n ->
            AppNode(AppNode(VariableNode(n.first, n.second.line), acc, acc.line), n.second, n.second.line)
//            AppNode(acc, n.second, n.second.line)
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
        val last = secondList.last().second


        val node = newList.foldRight(last) {n, acc ->

            AppNode(AppNode(VariableNode(n.second, n.first.line), n.first, n.first.line), acc, acc.line)
//            AppNode(n.first, acc, acc.line)
        }
        return node
    }
}