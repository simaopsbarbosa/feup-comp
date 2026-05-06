package pt.up.fe.comp2026.optimization.regAllocation;

import java.util.*;

public class InterferenceNode {

    private final String varName;
    private Set<InterferenceNode> neighbors;
    private int color;

    private boolean removed;

    public InterferenceNode(String varName) {
        this.varName = varName;
        neighbors = new HashSet<>();
        color = -1;
        removed = false;
    }


    public void addNeighbor(InterferenceNode node) {
        if (node == null || node == this || neighbors.contains(node)) return;

        this.neighbors.add(node);
        node.addNeighbor(this);
    }

    public int getNumActiveNeighbors() {
        int count = 0;
        for (InterferenceNode neighbor : neighbors) {
            if (!neighbor.isRemoved()) {
                count++;
            }
        }
        return count;
    }


    public void setColor(int color) {this.color = color;}
    public void setRemoved(boolean removed) {this.removed = removed;}

    public String getVarName() {return varName;}
    public Set<InterferenceNode> getNeighbors() {return neighbors;}
    public int getColor() {return color;}
    public boolean isRemoved() {return removed;}


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


    @Override
    public String toString() {
        return varName;
    }
}
