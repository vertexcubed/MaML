package vertexcubed.maml.parse

import vertexcubed.maml.parse.ast.FloatNode
import vertexcubed.maml.parse.ast.IntegerNode
import vertexcubed.maml.parse.parsers.CharParser
import vertexcubed.maml.parse.parsers.FloatParser
import vertexcubed.maml.parse.parsers.IntegerParser
import vertexcubed.maml.parse.result.ParseResult
import kotlin.test.Test
import kotlin.test.assertEquals

class ParserTest {

    private fun <T> resultOrFail(parseResult: ParseResult<T>): T {
        return when(parseResult) {
            is ParseResult.Failure -> throw AssertionError("parseResult failed. ${parseResult.logMessage}")
            is ParseResult.Success -> parseResult.result
        }
    }



    @Test
    fun testIntParser() {
        val expected = IntegerNode(159245)
        val actual = resultOrFail(IntegerParser().parse(Lexer("159245").read()))

        assertEquals(expected.number, actual.number)
    }

    @Test
    fun testFloatParser() {
        val expected = FloatNode(3141.5315f)
        val actual = resultOrFail(FloatParser().parse(Lexer("3141.5315").read()))
        assertEquals(expected.number, actual.number)
    }

    @Test
    fun testCharParser1() {
        val expected = 'a'
        val actual = resultOrFail(CharParser().parse(Lexer("'a'").read()))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser2() {
        val expected = '&'
        val actual = resultOrFail(CharParser().parse(Lexer("'\\o046'").read()))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser3() {
        val expected = ' '
        val actual = resultOrFail(CharParser().parse(Lexer("'\\space'").read()))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser4() {
        val expected = 'l'
        val actual = resultOrFail(CharParser().parse(Lexer("'\\108'").read()))
        assertEquals(expected, actual.text)
    }

    @Test
    fun testCharParser5() {
        val expected = 'N'
        val actual = resultOrFail(CharParser().parse(Lexer("'\\x4E'").read()))
        assertEquals(expected, actual.text)
    }


}