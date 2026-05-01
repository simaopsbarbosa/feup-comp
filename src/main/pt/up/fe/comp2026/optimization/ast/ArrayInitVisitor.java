package pt.up.fe.comp2026.optimization.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.type.JmmType;
import pt.up.fe.comp.jmm.analysis.table.type.impls.JmmArrayType;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2026.ast.TypeUtils;
import pt.up.fe.comp2026.jmm.ast.JmmAttributes;
import pt.up.fe.comp2026.jmm.ast.JmmKind;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pt.up.fe.comp2026.jmm.ast.JmmKind.*;

public class ArrayInitVisitor extends AJmmVisitor<Void, Boolean> {

    @Override
    protected void buildVisitor() {
        addVisit(ARRAY_INIT, this::visitArrayInit);

    }

    private boolean process(JmmNode node, JmmNode varRef,ArrayList<Integer> indices, JmmNode targetParent,int pos){
        List<JmmNode> elements= node.getChildren(ARRAY_ELEMENT);

        for(int i=0;i<elements.size();i++){
            JmmNode value= elements.get(i).getChild(0);
            indices.add(i);
            if(value.getKind()==ARRAY_INITIALIZER){

                process(value,varRef,indices,targetParent,pos);
            }
            else {
                JmmNode assignment = new JmmNodeImpl(ASSIGN_STMT);
                JmmNode arrayAccess=new JmmNodeImpl(ARRAY_ACCESS);
                arrayAccess.add(varRef);
                for(int p: indices){
                    JmmNode index=new JmmNodeImpl(INTEGER_LITERAL);
                    index.put("value",String.valueOf(p));
                    arrayAccess.add(index);
                }
                assignment.add(arrayAccess);
                assignment.add(value);
                targetParent.add(assignment,pos);
                pos++;
            }

            indices.removeLast();
        }
        return true;
    }
    private boolean visitArrayInit(JmmNode node, Void unused) {
        JmmNode newArrayExpr = new JmmNodeImpl(NEW_ARRAY);
        newArrayExpr.add(node.getChild(0).copy());
        String sizes= String.valueOf(node.getChild(1).getChildren(JmmKind.ARRAY_ELEMENT).size());
        newArrayExpr.put("size",node.get("dims").toString());
        JmmNode size=new JmmNodeImpl(INTEGER_LITERAL);
        size.put("value",sizes);
        newArrayExpr.add(size);
        JmmNode parent=node.getParent();
        JmmNode varRef=parent.getChild(0).copy();
        JmmNode outerblock=parent.getParent();
        int arrayint=parent.getIndexOfSelf()+1;
        process(node.getChild(1),varRef,new ArrayList<>(), outerblock,arrayint);

        node.replace(newArrayExpr);
        return true;
    }



}
