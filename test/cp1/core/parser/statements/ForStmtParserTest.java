package cp1.core.parser.statements;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.STATEMENT;

public class ForStmtParserTest extends JmmTestEnv {

    public ForStmtParserTest() {
        super("", "");
    }

    @Test
    public void testForWithMultipleInit() {
        setDescription("Test for loop with multiple assignments in the initialization part");
        parseSnippet("for (i = 0, j = 10; i < j; i = i + 1) {}", STATEMENT);
    }

    @Test
    public void testForWithMultipleUpdate() {
        setDescription("Test for loop with multiple assignments in the update part");
        parseSnippet("for (i = 0; i < 10; i = i + 1, j = j - 1) {}", STATEMENT);
    }

    @Test
    public void testForWithMultipleInitAndUpdate() {
        setDescription("Test for loop with multiple assignments in both initialization and update parts");
        parseSnippet("for (i = 0, j = 10; i < j; i = i + 1, j = j - 1) {}", STATEMENT);
    }

    @Test
    public void testForWithComplexAssignments() {
        setDescription("Test for loop with multiple assignments using complex expressions");
        parseSnippet("for (i = a + b, j = c.foo(); i < j; i = i * 2, j = j + a.bar()) {}", STATEMENT);
    }
}
