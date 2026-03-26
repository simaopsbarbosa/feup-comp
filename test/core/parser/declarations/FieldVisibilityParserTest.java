package core.parser.declarations;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.CLASS;

public class FieldVisibilityParserTest extends JmmTestEnv {

    public FieldVisibilityParserTest() {
        super("", "");
    }

    @Test
    public void testPrivateField() {
        setDescription("Test parsing of a private field");
        parseSnippet("class A { private int a; }", CLASS);
    }

    @Test
    public void testProtectedField() {
        setDescription("Test parsing of a protected field");
        parseSnippet("class A { protected int a; }", CLASS);
    }

    @Test
    public void testPublicField() {
        setDescription("Test parsing of a public field");
        parseSnippet("class A { public int a; }", CLASS);
    }

    @Test
    public void testPrivateFieldWithInitializer() {
        setDescription("Test parsing of a private field with initializer");
        parseSnippet("class A { private int a = 10; }", CLASS);
    }

    @Test
    public void testMultipleFieldsWithDifferentVisibilities() {
        setDescription("Test parsing of multiple fields with various visibility modifiers");
        parseSnippet("""
                class A {
                    private int a;
                    protected boolean b;
                    public String s;
                    int d; // package-protected
                }""", CLASS);
    }
}
