package cp2.extensions.ollir.arrays;

import org.junit.Test;
import org.specs.comp.ollir.ArrayOperand;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.inst.AssignInstruction;
import org.specs.comp.ollir.inst.CallInstruction;
import org.specs.comp.ollir.type.ArrayType;
import pt.up.fe.comp.test.env.OllirTestEnv;

public class MultidimensionalArrayInitOllirTest extends OllirTestEnv {
    private static final String BASE_PATH = "cp2/extensions/ollir/arrays/jmm/";
    private static final String RESOURCES_LOCATION = "test";

    public MultidimensionalArrayInitOllirTest() {
        super(BASE_PATH, RESOURCES_LOCATION);
    }

    public ClassUnit toOllir(String resourceName) {
        var ollirResult = jmmToOllir(resourceName);
        var classUnit = ollirResult.getOllirClass();
        assertNotNull("Ollir class unit should not be null", classUnit);
        assertEquals("Class name not what was expected", resourceName.replace(".jmm", ""), classUnit.getClassName());
        return classUnit;
    }

    public Method toOllir(String resourceName, String methodName) {
        var classUnit = toOllir(resourceName);
        var method = classUnit.getMethods().stream()
                .filter(m -> m.getMethodName().equals(methodName))
                .findFirst();
        assertTrue("Method '" + methodName + "' should exist", method.isPresent());
        return method.orElseThrow();
    }

    @Test
    public void testMultiDimArrayInit() {
        var method = toOllir("MultiDimArrayInit.jmm", "method");

        var calls = assertInstExists(CallInstruction.class, method);
        boolean hasNewArray = calls.stream().anyMatch(c ->
                c instanceof org.specs.comp.ollir.inst.NewInstruction &&
                        ((org.specs.comp.ollir.inst.NewInstruction) c).getReturnType() instanceof ArrayType);

        assertTrue("Method should contain a new array initialization instruction", hasNewArray);
    }

    @Test
    public void testMultiDimArrayInitList() {
        var method = toOllir("MultiDimArrayInitList.jmm", "method");

        var calls = assertInstExists(CallInstruction.class, method);
        boolean hasNewArray = calls.stream().anyMatch(c ->
                c instanceof org.specs.comp.ollir.inst.NewInstruction &&
                        ((org.specs.comp.ollir.inst.NewInstruction) c).getReturnType() instanceof ArrayType);

        assertTrue("Method should contain a new array initialization instruction", hasNewArray);
    }

    @Test
    public void testMultiDimArrayRead() {
        var method = toOllir("MultiDimArrayRead.jmm", "method");

        var assigns = assertInstExists(AssignInstruction.class, method);

        long arrayReads = assigns.stream()
                .flatMap(assign -> getElements(assign.getRhs()).stream())
                .filter(element -> element instanceof ArrayOperand)
                .count();

        assertTrue("Method should contain an array read operation", arrayReads >= 1);
    }

    @Test
    public void testMultiDimArrayWrite() {
        var method = toOllir("MultiDimArrayWrite.jmm", "method");

        var assigns = assertInstExists(AssignInstruction.class, method);

        long arrayStores = assigns.stream()
                .filter(assign -> assign.getDest() instanceof ArrayOperand)
                .count();

        assertTrue("Method should contain an array store operation", arrayStores >= 1);
    }
}