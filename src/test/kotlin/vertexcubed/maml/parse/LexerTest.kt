package vertexcubed.maml.parse

import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class LexerTest {

    @Test
    fun testLexing1() {
        val lexer = Lexer("one two three four", "UNITTEST")
        val firstTest = listOf(
                Token(TokenType.IDENTIFIER, "one", 1),
                Token(TokenType.IDENTIFIER, "two", 1),
                Token(TokenType.IDENTIFIER, "three", 1),
                Token(TokenType.IDENTIFIER, "four", 1),
                Token(TokenType.EOF, "", 1)
        )
        assertEquals(firstTest, lexer.read())
    }

    @Test
    fun testLexing2() {
        val lexer = Lexer("-4213.2792", "UNITTEST")
        val secondTest = listOf(
            Token(TokenType.NUMBER_LITERAL, "-4213", 1),
            Token(TokenType.SPECIAL_CHAR, ".", 1),
            Token(TokenType.NUMBER_LITERAL, "2792", 1),
            Token(TokenType.EOF, "", 1)
        )
        assertEquals(secondTest, lexer.read())
    }
    @Test
    fun testLexing3() {
        val lexer = Lexer("line tests something\nsomething else", "UNITTEST")
        val thirdTest = listOf(
            Token(TokenType.IDENTIFIER, "line", 1),
            Token(TokenType.IDENTIFIER, "tests", 1),
            Token(TokenType.IDENTIFIER, "something", 1),
            Token(TokenType.IDENTIFIER, "something", 2),
            Token(TokenType.KEYWORD, "else", 2),
            Token(TokenType.EOF, "", 2)
        )
        assertEquals(thirdTest, lexer.read())
    }
    @Test
    fun testLexing4() {
        val lexer = Lexer("string \"literals\" and \"2.314\"      also \"ignoring \\\"escaped\\\" characters\"", "UNITTEST")
        val fourthTest = listOf(
            Token(TokenType.IDENTIFIER, "string", 1),
            Token(TokenType.STRING_LITERAL, "literals", 1),
            Token(TokenType.KEYWORD, "and", 1),
            Token(TokenType.STRING_LITERAL, "2.314", 1),
            Token(TokenType.IDENTIFIER, "also", 1),
            Token(TokenType.STRING_LITERAL, "ignoring \\\"escaped\\\" characters", 1),
            Token(TokenType.EOF, "", 1)
        )
        assertEquals(fourthTest, lexer.read())

    }
    @Test
    fun testLexing5() {
        val lexer = Lexer("char tests \'n\' and \'p\' 1245 \'\\o372\' '\\n' 456.21=3 \'\\space\' \' \' ", "UNITTEST")
        val fifthTest = listOf(
            Token(TokenType.IDENTIFIER, "char", 1),
            Token(TokenType.IDENTIFIER, "tests", 1),
            Token(TokenType.CHAR_LITERAL, "n", 1),
            Token(TokenType.KEYWORD, "and", 1),
            Token(TokenType.CHAR_LITERAL, "p", 1),
            Token(TokenType.NUMBER_LITERAL, "1245", 1),
            Token(TokenType.CHAR_LITERAL, "\\o372", 1),
            Token(TokenType.CHAR_LITERAL, "\\n", 1),
            Token(TokenType.NUMBER_LITERAL, "456", 1),
            Token(TokenType.SPECIAL_CHAR, ".", 1),
            Token(TokenType.NUMBER_LITERAL, "21", 1),
            Token(TokenType.SPECIAL_CHAR, "=", 1),
            Token(TokenType.NUMBER_LITERAL, "3", 1),
            Token(TokenType.CHAR_LITERAL, "\\space", 1),
            Token(TokenType.CHAR_LITERAL, " ", 1),
            Token(TokenType.EOF, "", 1)
        )
        assertEquals(fifthTest, lexer.read())

    }
    @Test
    fun testLexing6() {
        val lexer = Lexer("let x = 0x2341 in x + 2.6", "UNITTEST")
        val sixthTest = listOf(
            Token(TokenType.KEYWORD, "let", 1),
            Token(TokenType.IDENTIFIER, "x", 1),
            Token(TokenType.SPECIAL_CHAR, "=", 1),
            Token(TokenType.HEX_LITERAL, "0x2341", 1),
            Token(TokenType.KEYWORD, "in", 1),
            Token(TokenType.IDENTIFIER, "x", 1),
            Token(TokenType.SPECIAL_CHAR, "+", 1),
            Token(TokenType.NUMBER_LITERAL, "2", 1),
            Token(TokenType.SPECIAL_CHAR, ".", 1),
            Token(TokenType.NUMBER_LITERAL, "6", 1),
            Token(TokenType.EOF, "", 1),
        )
        assertEquals(sixthTest, lexer.read())
    }

    @Test
    fun testLexing7() {
        val lexer = Lexer("comment test (* this is a comment! ignore me entirely *) cool", "UNITTEST")
        val expected = listOf(
            Token(TokenType.IDENTIFIER, "comment", 1),
            Token(TokenType.IDENTIFIER, "test", 1),
            Token(TokenType.IDENTIFIER, "cool", 1),
            Token(TokenType.EOF, "", 1),
        )
        assertEquals(expected, lexer.read())
    }

    @Test
    fun testLexing8() {
        val lexer = Lexer("nested comments (* this is a comment! (* this is another comment! *) ignore me entirely *) cool", "UNITTEST")
        val expected = listOf(
            Token(TokenType.IDENTIFIER, "nested", 1),
            Token(TokenType.IDENTIFIER, "comments", 1),
            Token(TokenType.IDENTIFIER, "cool", 1),
            Token(TokenType.EOF, "", 1),
        )
        assertEquals(expected, lexer.read())
    }
}