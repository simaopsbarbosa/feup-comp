package pt.up.fe.comp2026.optimization.regAllocation;

import java.util.*;

public class InterferenceNode {

    private final String varName;
    private Set<InterferenceNode> neighbors;
    private int color;

    public InterferenceNode(String varName) {
        this.varName = varName;
        neighbors = new HashSet<>();
        color = -1;
    }


    public void addNeighbor(InterferenceNode node) {
        if (node == null || node == this || neighbors.contains(node)) return;

        this.neighbors.add(node);
        node.addNeighbor(this);
    }

    public void setColor(int color) {this.color = color;}


    public String getVarName() {return varName;}
    public Set<InterferenceNode> getNeighbors() {return neighbors;}
    public int getColor() {return color;}


    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || this.getClass() != object.getClass()) return false;

        InterferenceNode node = (InterferenceNode) object;

        return Objects.equals(this.getVarName(), node.getVarName());
    }

    @Override
    public int hashCode() {
        return this.varName.hashCode();
    }
}
