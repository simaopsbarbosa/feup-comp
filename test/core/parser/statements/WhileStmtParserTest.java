package core.parser.statements;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.STATEMENT;

public class WhileStmtParserTest extends JmmTestEnv {

    public WhileStmtParserTest() {
        super("", "");
    }

    @Test
    public void testSimpleWhile() {
        setDescription("Test a simple while loop with a block");
        parseSnippet("while (a < 10) { a = a + 1; }", STATEMENT);
    }

    @Test
    public void testWhileWithoutBlock() {
        setDescription("Test a while loop without a block");
        parseSnippet("while (a < 10) a = a + 1;", STATEMENT);
    }

    @Test
    public void testWhileWithComplexCondition() {
        setDescription("Test a while loop with a complex condition");
        parseSnippet("while (a < 10 && b != false) { x = x * 2; }", STATEMENT);
    }

    @Test
    public void testNestedWhile() {
        setDescription("Test nested while loops");
        parseSnippet("""
                while (i < 10) {
                    while (j < 10) {
                        j = j + 1;
                    }
                    i = i + 1;
                }""", STATEMENT);
    }
}
