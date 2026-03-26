package core.parser.statements;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.STATEMENT;

public class IfStmtParserTest extends JmmTestEnv {

    public IfStmtParserTest() {
        super("", "");
    }

    @Test
    public void testSimpleIf() {
        setDescription("Test a simple if statement with a block");
        parseSnippet("if (a < 10) { x = 1; }", STATEMENT);
    }

    @Test
    public void testIfWithoutBlock() {
        setDescription("Test an if statement without a block");
        parseSnippet("if (a < 10) x = 1;", STATEMENT);
    }

    @Test
    public void testIfElse() {
        setDescription("Test an if-else statement with blocks");
        parseSnippet("if (a < 10) { x = 1; } else { x = 2; }", STATEMENT);
    }

    @Test
    public void testIfElseWithoutBlocks() {
        setDescription("Test an if-else statement without blocks");
        parseSnippet("if (a < 10) x = 1; else x = 2;", STATEMENT);
    }

    @Test
    public void testNestedIfElse() {
        setDescription("Test nested if-else statements (else-if structure)");
        parseSnippet("""
                if (a < 10) {
                    x = 1;
                } else if (a < 20) {
                    x = 2;
                } else {
                    x = 3;
                }""", STATEMENT);
    }

    @Test
    public void testIfWithComplexCondition() {
        setDescription("Test an if statement with a complex boolean condition");
        parseSnippet("if (a < 10 && b || !c) { x = 1; }", STATEMENT);
    }
}
