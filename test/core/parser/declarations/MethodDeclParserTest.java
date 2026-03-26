package core.parser.declarations;

import org.junit.Test;
import pt.up.fe.comp.test.env.JmmTestEnv;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.METHOD;

public class MethodDeclParserTest extends JmmTestEnv {

    public MethodDeclParserTest() {
        super("", "");
    }

    @Test
    public void testPublicMethod() {
        setDescription("Test a simple public method");
        parseSnippet("public int foo() { return 0; }", METHOD);
    }

    @Test
    public void testPrivateMethod() {
        setDescription("Test a private method");
        parseSnippet("private void bar() {}", METHOD);
    }

    @Test
    public void testProtectedMethod() {
        setDescription("Test a protected method");
        parseSnippet("protected boolean isEven(int n) { return true; }", METHOD);
    }

    @Test
    public void testStaticMethod() {
        setDescription("Test a public static method (e.g. main)");
        parseSnippet("public static void main(String[] args) {}", METHOD);
    }

    @Test
    public void testMethodWithParams() {
        setDescription("Test a method with multiple parameters");
        parseSnippet("int sum(int a, int b, boolean c) { return a + b; }", METHOD);
    }

    @Test
    public void testMethodWithLocalVars() {
        setDescription("Test a method with local variable declarations");
        parseSnippet("""
                public int foo(int a) {
                    int b;
                    int c = 10;
                    b = a + c;
                    return b;
                }""", METHOD);
    }

    @Test
    public void testMethodWithMultipleStmts() {
        setDescription("Test a method with various statements");
        parseSnippet("""
                void complex() {
                    int i = 0;
                    while (i < 10) {
                        if (i % 2 == 0) {
                            io.println(i);
                        }
                        i++;
                    }
                }""", METHOD);
    }
}
