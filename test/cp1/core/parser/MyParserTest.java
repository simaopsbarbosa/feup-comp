// java
package cp1.core.parser;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.ast.Kind;
import pt.up.fe.comp.test.env.JmmTestEnv;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import static pt.up.fe.comp.cp1.core.parser.RulesNames.*;

public class MyParserTest extends JmmTestEnv {


    public MyParserTest() {
        super("", "");
    }

    @Test
    public void testHelloWorld() {
        var res = parseSnippet("""  
                package hello.world;
                import util.ioPlus;
                class HelloWorld {
                    void main() {
                        int[][] arr = new int[2][2];
                        arr = new int[][] {{1,2}, {3,4}};
                        arr[1] = new int[] {0,0};
                        arr[1][0] = 9;
                        ioPlus.printHelloWorld();
                    }
                }""");
    }
}
