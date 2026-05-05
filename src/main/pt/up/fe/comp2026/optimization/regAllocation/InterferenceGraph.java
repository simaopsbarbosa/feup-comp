package pt.up.fe.comp2026.optimization.regAllocation;

import java.util.*;

public class InterferenceGraph {

    Map<String, InterferenceNode> nodes;

    public InterferenceGraph(List<InstructionLiveness> livenessTable) {
        this.nodes = new LinkedHashMap<>();
        this.createNodes(livenessTable);
        this.populateNodes(livenessTable);
    }


    public void createNodes(List<InstructionLiveness> livenessTable) {
        Set<String> allVarNames = new HashSet<>();

        for (InstructionLiveness liveness : livenessTable) {
            allVarNames.addAll(liveness.getIn());
            allVarNames.addAll(liveness.getOut());
        }

        for (String varName : allVarNames) {
            InterferenceNode node = new InterferenceNode(varName);
            nodes.put(varName, node);
        }
    }

    public void populateNodes(List<InstructionLiveness> livenessTable) {

        for (InstructionLiveness liveness : livenessTable) {
            Set<String> in = liveness.getIn();
            Set<String> out = liveness.getOut();

            this.createEdgesInSet(in);
            this.createEdgesInSet(out);
        }
    }


    public void createEdgesInSet(Set<String> vars) {
        List<String> varList = new ArrayList<>(vars);

        for (int i = 0; i < vars.size()-1; i++) {
            InterferenceNode outer = this.nodes.get(varList.get(i));

            for (int j = i+1; j < vars.size(); j++) {
                InterferenceNode inner = this.nodes.get(varList.get(j));

                if (outer != null && inner != null)
                    outer.addNeighbor(inner);
            }
        }
    }
    


    public void printGraph() {
        System.out.println("\n--- Interference Graph ---");
        if (nodes.isEmpty()) {
            System.out.println("Empty Graph (No variables found).");
            return;
        }

        // Header for the table
        System.out.printf("%-15s | %-7s | %s%n", "Variable", "Color", "Interferes With");
        System.out.println("-".repeat(50));

        for (InterferenceNode node : nodes.values()) {
            List<String> neighborNames = new ArrayList<>();
            for (InterferenceNode neighbor : node.getNeighbors()) {
                neighborNames.add(neighbor.getVarName());
            }
            // Sort neighbors for consistent output
            Collections.sort(neighborNames);

            String colorStr = (node.getColor() == -1) ? "None" : String.valueOf(node.getColor());

            System.out.printf("%-15s | %-7s | %s%n",
                    node.getVarName(),
                    colorStr,
                    String.join(", ", neighborNames)
            );
        }
        System.out.println("---------------------------\n");
    }


}
