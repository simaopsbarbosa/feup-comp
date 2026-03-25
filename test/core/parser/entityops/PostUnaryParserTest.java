package core.parser.entityops;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.STATEMENT;

public class PostUnaryParserTest extends JmmTestEnv {

    public PostUnaryParserTest() {
        super("", "");
    }

    @Test
    public void testPostIncrement() {
        setDescription("Test parsing of post-increment unary operator (x++)");
        parseSnippet("x++;", STATEMENT);
    }

    @Test
    public void testPostDecrement() {
        setDescription("Test parsing of post-decrement unary operator (x--)");
        parseSnippet("x--;", STATEMENT);
    }

    @Test
    public void testPostIncrementInExpression() {
        setDescription("Test post-increment used in a larger expression");
        parseSnippet("y = x++ + 1;", STATEMENT);
    }

    @Test
    public void testPostDecrementInExpression() {
        setDescription("Test post-decrement used in a larger expression");
        parseSnippet("y = x-- - 1;", STATEMENT);
    }
}
