package core.parser.arrays;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.STATEMENT;

public class NestedArrayInitializerParserTest extends JmmTestEnv {

    public NestedArrayInitializerParserTest() {
        super("", "");
    }

    @Test
    public void testNestedArrayInitializer() {
        setDescription("Test parsing of a multidimensional array initializer");
        parseSnippet("new int[][] {{1, 2}, {3, 4}};", STATEMENT);
    }

    @Test
    public void testDeeplyNestedArrayInitializer() {
        setDescription("Test parsing of a 3D array initializer");
        parseSnippet("new int[][][] {{{1}, {2}}, {{3}, {4}}};", STATEMENT);
    }

    @Test
    public void testIrregularArrayInitializer() {
        setDescription("Test parsing of an irregular (jagged) array initializer");
        parseSnippet("new int[][] {{1, 2, 3}, {4, 5}};", STATEMENT);
    }

    @Test
    public void testArrayInitializerWithComplexExpressions() {
        setDescription("Test array initializer with complex expressions as elements");
        parseSnippet("new int[] {a + 1, b.foo(), c[0]};", STATEMENT);
    }

    @Test
    public void testEmptyNestedArrayInitializer() {
        setDescription("Test parsing of an empty nested array initializer");
        parseSnippet("new int[][] {{}, {}};", STATEMENT);
    }
}
