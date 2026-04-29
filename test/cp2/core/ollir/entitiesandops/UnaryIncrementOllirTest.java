package cp2.core.ollir.entitiesandops;

import org.junit.Test;
import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Method;
import org.specs.comp.ollir.inst.*;
import pt.up.fe.comp.test.env.OllirTestEnv;

public class UnaryIncrementOllirTest extends OllirTestEnv {
    private static final String BASE_PATH = "cp2/core/ollir/entitiesandops/jmm/";
    private static final String RESOURCES_LOCATION = "test";

    public UnaryIncrementOllirTest() {
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
        var method = classUnit.getMethods().stream().filter(m -> m.getMethodName().equals(methodName)).findFirst();
        assertEquals("Method '" + methodName + "' should exist", true, method.isPresent());
        return method.orElseThrow();
    }

    @Test
    public void testPreIncrement() {
        setDescription("Test pre-increment operator (++x) generates OLLIR");
        var method = toOllir("PreIncrement.jmm", "method");
        assertAtLeast(AssignInstruction.class, 1, method);
    }

    @Test
    public void testPreDecrement() {
        setDescription("Test pre-decrement operator (--x) generates OLLIR");
        var method = toOllir("PreDecrement.jmm", "method");
        assertAtLeast(AssignInstruction.class, 1, method);
    }

    @Test
    public void testPostIncrement() {
        setDescription("Test post-increment operator (x++) generates OLLIR");
        var method = toOllir("PostIncrement.jmm", "method");
        assertAtLeast(AssignInstruction.class, 1, method);
    }

    @Test
    public void testPostDecrement() {
        setDescription("Test post-decrement operator (x--) generates OLLIR");
        var method = toOllir("PostDecrement.jmm", "method");
        assertAtLeast(AssignInstruction.class, 1, method);
    }

    @Test
    public void testPreIncrementAssign() {
        setDescription("Test pre-increment used in assignment generates OLLIR");
        var method = toOllir("PreIncrementAssign.jmm", "method");
        assertAtLeast(AssignInstruction.class, 2, method);
    }

    @Test
    public void testPreIncrementMultiple() {
        setDescription("Test multiple pre-increments in expression generates OLLIR");
        var method = toOllir("PreIncrementMultiple.jmm", "method");
        assertAtLeast(AssignInstruction.class, 2, method);
    }

    @Test
    public void testPreDecrementMultiple() {
        setDescription("Test multiple pre-decrements in expression generates OLLIR");
        var method = toOllir("PreDecrementMultiple.jmm", "method");
        assertAtLeast(AssignInstruction.class, 2, method);
    }

    @Test
    public void testPostIncrementMultiple() {
        setDescription("Test multiple post-increments in expression generates OLLIR");
        var method = toOllir("PostIncrementMultiple.jmm", "method");
        assertAtLeast(AssignInstruction.class, 2, method);
    }

    @Test
    public void testPostDecrementMultiple() {
        setDescription("Test multiple post-decrements in expression generates OLLIR");
        var method = toOllir("PostDecrementMultiple.jmm", "method");
        assertAtLeast(AssignInstruction.class, 2, method);
    }

    @Test
    public void testMixedPrePostIncrement() {
        setDescription("Test mixed pre and post increment generates OLLIR");
        var method = toOllir("MixedPrePostIncrement.jmm", "method");
        assertAtLeast(AssignInstruction.class, 2, method);
    }

    @Test
    public void testIncrementInCondition() {
        setDescription("Test increment in if condition generates OLLIR");
        var method = toOllir("IncrementInCondition.jmm", "method");
        assertAtLeast(AssignInstruction.class, 1, method);
    }

    @Test
    public void testIncrementWithArithmetic() {
        setDescription("Test increment combined with arithmetic generates OLLIR");
        var method = toOllir("IncrementWithArithmetic.jmm", "method");
        assertAtLeast(AssignInstruction.class, 1, method);
    }

    @Test
    public void testIncrementInWhile() {
        setDescription("Test increment in while loop generates OLLIR");
        var method = toOllir("IncrementInWhile.jmm", "method");
        assertAtLeast(AssignInstruction.class, 1, method);
    }

    @Test
    public void testIncrementComplexExpression() {
        setDescription("Test complex expression with multiple increments generates OLLIR");
        var method = toOllir("IncrementComplexExpression.jmm", "method");
        assertAtLeast(AssignInstruction.class, 3, method);
    }
}