package core.parser.statements;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.STATEMENT;

public class ReturnStmtParserTest extends JmmTestEnv {

    public ReturnStmtParserTest() {
        super("", "");
    }

    @Test
    public void testReturnVoid() {
        setDescription("Test a return statement with no expression");
        parseSnippet("return;", STATEMENT);
    }

    @Test
    public void testReturnExpression() {
        setDescription("Test a return statement with an expression");
        parseSnippet("return a + b;", STATEMENT);
    }

    @Test
    public void testReturnLiteral() {
        setDescription("Test a return statement with a literal");
        parseSnippet("return 10;", STATEMENT);
    }

    @Test
    public void testReturnBoolean() {
        setDescription("Test a return statement with a boolean literal");
        parseSnippet("return true;", STATEMENT);
    }

    @Test
    public void testReturnComplexExpression() {
        setDescription("Test a return statement with a complex expression");
        parseSnippet("return a.foo(b) + c[0];", STATEMENT);
    }
}
