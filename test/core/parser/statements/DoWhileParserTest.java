package core.parser.statements;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.STATEMENT;

public class DoWhileParserTest extends JmmTestEnv {

    public DoWhileParserTest() {
        super("", "");
    }

    @Test
    public void testDoWhileWithExpression() {
        setDescription("Test do-while with an expression as its body (no semicolon after expression)");
        parseSnippet("do a + 1 while (x < 10);", STATEMENT);
    }

    @Test
    public void testDoWhileWithInvalidExpression() {
        setDescription("Test do-while with an invalid expression (assignment is not an expression)");
        parseSnippetWithErrors("do x = x + 1 while (x < 10);", STATEMENT);
    }

    @Test
    public void testDoWhileWithStatement() {
        setDescription("Test do-while with a statement as its body (with semicolon after expression)");
        parseSnippet("do x = x + 1; while (x < 10);", STATEMENT);
    }

    @Test
    public void testDoWhileWithBlock() {
        setDescription("Test do-while with a block as its body");
        parseSnippet("do { x = x + 1; } while (x < 10);", STATEMENT);
    }

    @Test
    public void testDoWhileWithImplicitMethodCall() {
        setDescription("Test do-while with an implicit method call expression as its body");
        parseSnippet("do foo() while (cond);", STATEMENT);
    }
}
