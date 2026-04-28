package cp2.extensions.ollir.calls;

import org.junit.Test;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.LiteralElement;
import org.specs.comp.ollir.inst.InvokeVirtualInstruction;
import org.specs.comp.ollir.inst.ReturnInstruction;
import org.specs.comp.ollir.type.BuiltinKind;
import org.specs.comp.ollir.type.BuiltinType;
import pt.up.fe.comp.test.env.OllirTestEnv;

public class ImplicitMethodCallOllirTest extends OllirTestEnv {
    private static final String BASE_PATH = "cp2/extensions/ollir/calls/jmm/";
    private static final String RESOURCES_LOCATION = "test";

    public ImplicitMethodCallOllirTest() {
        super(BASE_PATH, RESOURCES_LOCATION);
    }

    public ClassUnit toOllir(String resourceName) {
        var ollirResult = jmmToOllir(resourceName);
        var classUnit = ollirResult.getOllirClass();
        assertNotNull("Ollir class unit should not be null", classUnit);
        assertEquals("Class name not what was expected", resourceName.replace(".jmm", ""), classUnit.getClassName());
        return classUnit;
    }

    @Test
    public void testSimpleImplicitMethodCall() {
        var classUnit = toOllir("ImplicitMethodCallSimple.jmm");
        var className = "cp2.extensions.ollir.calls.jmm.ImplicitMethodCallSimple";

        var method = classUnit.getMethods().stream()
                .filter(m -> m.getMethodName().equals("method"))
                .findFirst()
                .orElseThrow();

        var calls = assertInstExists(InvokeVirtualInstruction.class, method);
        assertTrue("Method should contain an invokevirtual call", !calls.isEmpty());

        var call = calls.getFirst();
        assertTrue("Method name should be 'compute'", call.getMethodName() instanceof LiteralElement le && le.getLiteral().equals("compute"));
        assertEquals("Call should have no arguments", 0, call.getArguments().size());
        assertTrue("Call return type should be int", BuiltinType.is(call.getReturnType(), BuiltinKind.INT32));

        assertInstExists(ReturnInstruction.class, method);
    }

    @Test
    public void testImplicitMethodCallWithArgs() {
        var classUnit = toOllir("ImplicitMethodCallWithArgs.jmm");
        var method = classUnit.getMethods().stream()
                .filter(m -> m.getMethodName().equals("method"))
                .findFirst()
                .orElseThrow();

        var calls = assertInstExists(InvokeVirtualInstruction.class, method);
        assertTrue("Method should contain an invokevirtual call", !calls.isEmpty());

        var call = calls.getFirst();
        assertTrue("Method name should be 'add'", call.getMethodName() instanceof LiteralElement le && le.getLiteral().equals("add"));
        assertEquals("Call should have two arguments", 2, call.getArguments().size());
        assertTrue("Call return type should be int", BuiltinType.is(call.getReturnType(), BuiltinKind.INT32));
    }

    @Test
    public void testImplicitMethodCallInExpression() {
        var classUnit = toOllir("ImplicitMethodCallInExpression.jmm");
        var method = classUnit.getMethods().stream()
                .filter(m -> m.getMethodName().equals("method"))
                .findFirst()
                .orElseThrow();

        var calls = assertInstExists(InvokeVirtualInstruction.class, method);
        assertTrue("Method should contain two invokevirtual calls", calls.size() >= 2);

        var addCall = calls.stream()
                .filter(c -> c.getMethodName() instanceof LiteralElement le && le.getLiteral().equals("add"))
                .findFirst();
        assertTrue("Should contain call to 'add'", addCall.isPresent());

        var mulCall = calls.stream()
                .filter(c -> c.getMethodName() instanceof LiteralElement le && le.getLiteral().equals("multiply"))
                .findFirst();
        assertTrue("Should contain call to 'multiply'", mulCall.isPresent());
    }


}