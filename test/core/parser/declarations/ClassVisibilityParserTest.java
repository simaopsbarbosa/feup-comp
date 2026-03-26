package core.parser.declarations;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.CLASS;

public class ClassVisibilityParserTest extends JmmTestEnv {

    public ClassVisibilityParserTest() {
        super("", "");
    }

    @Test
    public void testPublicClass() {
        setDescription("Test parsing of a public class");
        parseSnippet("public class A {}", CLASS);
    }

    @Test
    public void testPrivateClass() {
        setDescription("Test parsing of a private class");
        parseSnippet("private class A {}", CLASS);
    }

    @Test
    public void testProtectedClass() {
        setDescription("Test parsing of a protected class");
        parseSnippet("protected class A {}", CLASS);
    }

    @Test
    public void testPublicClassWithSuper() {
        setDescription("Test parsing of a public class with a superclass");
        parseSnippet("public class A extends B {}", CLASS);
    }

    @Test
    public void testPublicClassWithFieldsAndMethods() {
        setDescription("Test parsing of a public class with fields and methods");
        parseSnippet("""
                public class A {
                    private int x;
                    public void foo() {}
                }""", CLASS);
    }
}
