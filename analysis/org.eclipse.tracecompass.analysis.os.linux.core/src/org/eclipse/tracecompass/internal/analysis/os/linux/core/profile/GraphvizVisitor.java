package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule.Mode;

/**
 *  This class implements IProfileData to be implemented on the
 *  tests related with Profiling and ECCT
 *  @author francisco
 */

class GraphvizVisitor implements IProfileVisitor<ProfileData> {
    /**
     * result ArrayList of Nodes, which are ProfileData
     */
    public ArrayList<Node<ProfileData>> result = new ArrayList<>();

    @Override
    public void visit(Node<ProfileData> node) {
        result.add(node);
    }

    /**
     * This function reset the visit
     */
    public void reset() {
        result = new ArrayList<>();
    }

    /**
     * This function print on the console the tree
     *
     * @throws Exception
     */
    public void print(String name, Mode mode) throws Exception {

        String content = new String("digraph G { \n");
        if (mode != Mode.COLOR_) {
            if (mode != Mode.ID_) {
                // Edges and nodes:
                for (Node<ProfileData> n : result) {
                    if (n.getParent() != null) {
                        // System.out.print(n.getNodeLabel() + " -> " +
                        // n.getParent().getNodeLabel() + "; \n");
                        content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                    } else {
                        // System.out.print(n.getNodeLabel() + "; \n");
                        content = content.concat(n.getNodeLabel() + "; \n");
                    }
                }
                for (Node<ProfileData> n : result) {
                    content = content.concat(n.getNodeLabel() + " " + "[label = \"" + n.getNodeLabel() + "[" + n.getProfileData().getWeight() + "]\" ]; \n");
                }
            } else {
                // Edges and nodes:
                for (Node<ProfileData> n : result) {
                    if (n.getParent() != null) {
                        // System.out.print(n.getNodeId() + " -> " +
                        // n.getParent().getNodeId() + "; \n");
                        content = content.concat(n.getParent().getNodeId() + " -> " + n.getNodeId() + "; \n");
                    } else {
                        System.out.print(n.getNodeId() + "; \n");
                        content = content.concat(n.getNodeId() + "; \n");
                    }
                }
                for (Node<ProfileData> n : result) {
                    content = content.concat(n.getNodeId() + " " + "[label = \"" + n.getNodeId() + "[" + n.getProfileData().getWeight() + "]\" ]; \n");
                }
            }
        } else {
            for (Node<ProfileData> n : result) {
                if (n.getParent() != null) {
                    // System.out.print(n.getNodeLabel() + " -> " +
                    // n.getParent().getNodeLabel() + "; \n");
                    content = content.concat(n.getParent().getNodeLabel() + " -> " + n.getNodeLabel() + "; \n");
                } else {
                    // System.out.print(n.getNodeLabel() + "; \n");
                    content = content.concat(n.getNodeLabel() + "; \n");
                }
            }
            for (Node<ProfileData> n : result) {
                content = content.concat(n.getNodeLabel() + " " + "[label = \"" + n.getNodeLabel() + "[" + n.getProfileData() + "]\" ]; \n"); // tirei
                                                                                                                                              // o
                                                                                                                                              // color
            }
        }
        content = content.concat("\n }\n");
        writeToFile(name, content);
    }

    /**
     * This function print on a file the output of the tree:
     */
    public void writeToFile(String name, String content) throws Exception {
        try {

            // String content = "This is the content to write into file";
            String fileName = new String("/tmp/"); //$NON-NLS-1$
            fileName = fileName.concat(name); //
            File file = new File(fileName); // "/home/frank/Desktop/tree.gv");

            // if file doesnt exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }

            try (FileWriter fw = new FileWriter(file.getAbsoluteFile())) {
                try (BufferedWriter bw = new BufferedWriter(fw)) {
                    bw.write(content);
                    bw.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
