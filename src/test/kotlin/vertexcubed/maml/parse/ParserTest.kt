package vertexcubed.maml.parse

import vertexcubed.maml.ast.FloatNode
import vertexcubed.maml.ast.IntegerNode
import vertexcubed.maml.parse.parsers.*
import vertexcubed.maml.parse.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {

    val parseEnv = ParseEnv()
    init {
        parseEnv.init()
    }


    private fun <T> resultOrFail(parseResult: ParseResult<T>): T {
        return when(parseResult) {
            is ParseResult.Failure -> throw AssertionError("parseResult failed. ${parseResult.logMessage}")
            is ParseResult.Success -> parseResult.result
        }
    }



    @Test
    fun testIntParser() {
        val expected = IntegerNode(159245, -1)
        val actual = resultOrFail(IntegerParser().parse(Lexer("159245").read(), parseEnv))

        assertEquals(expected.number, actual.number)
    }

    @Test
    fun testFloatParser() {
        val expected = FloatNode(3141.5315f, -1)
        val actual = resultOrFail(FloatParser().parse(Lexer("3141.5315").read(), parseEnv))
        assertEquals(expected.number, actual.number)
    }

    @Test
    fun testCharParser1() {
        val expected = 'a'
        val actual = resultOrFail(CharParser().parse(Lexer("'a'").read(), parseEnv))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser2() {
        val expected = '&'
        val actual = resultOrFail(CharParser().parse(Lexer("'\\o046'").read(), parseEnv))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser3() {
        val expected = ' '
        val actual = resultOrFail(CharParser().parse(Lexer("'\\space'").read(), parseEnv))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser4() {
        val expected = 'l'
        val actual = resultOrFail(CharParser().parse(Lexer("'\\108'").read(), parseEnv))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser5() {
        val expected = 'N'
        val actual = resultOrFail(CharParser().parse(Lexer("'\\x4E'").read(), parseEnv))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCompoundSpecialCharParser() {
        val expected = "+-=-/*"
        val actual = resultOrFail(CompoundSpecialCharParser(expected).parse(Lexer(expected).read(), parseEnv))
        assertEquals(expected, actual)
    }

    @Test
    fun testSpecificIdentifierParser() {
        val expected = "meow"
        val actual = resultOrFail(SpecificIdentifierParser(expected).parse(Lexer(expected).read(), parseEnv))
        assertEquals(expected, actual)
    }

}