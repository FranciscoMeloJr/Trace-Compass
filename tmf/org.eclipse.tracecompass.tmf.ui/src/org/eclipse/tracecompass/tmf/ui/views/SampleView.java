package org.eclipse.tracecompass.tmf.ui.views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileVisitor;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.Node;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileTraversal.KeyTree;
import org.eclipse.tracecompass.statesystem.core.exceptions.StateSystemDisposedException;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.TimeGraphEntry;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 *
 * @since 2.0
 */

public class SampleView extends CallStackView {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID1 = "org.eclipse.tracecompass.tmf.ui.views.SampleView";

    /**
     * The constructor.
     */
    public SampleView() {
        super();
    }

    /**
     * This method will create the Entry List from the node:
     */

    @SuppressWarnings("restriction")
    // Override this method:
    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        // Load the module:
        System.out.println("buildEntryList " + trace.getName());
        Iterable<CCTAnalysisModule> iter = TmfTraceUtils.getAnalysisModulesOfClass(trace, CCTAnalysisModule.class);
        CCTAnalysisModule module = null;

        // Map of the nodes:
        Map<KeyTree, Node<ProfileData>> map;

        // Maps for the Entries
        Map<ITmfTrace, TraceEntry> traceEntryMap = new HashMap<>();
        Map<ITmfTrace, LevelEntry> levelEntryMap = new HashMap<>();
        Map<LevelEntry, EventEntry> eventEntryMap = new HashMap<>();

        // Selects only the CCTAnalysis module
        for (IAnalysisModule mod : iter) {
            if (mod instanceof CCTAnalysisModule) {
                module = (CCTAnalysisModule) mod;
                System.out.println("Module" + module);
                break;
            }
        }

        if (module == null) {
            return;
        }
        // Modules:
        module.schedule();
        module.waitForCompletion();

        // Read the tree but we will not use it
        Node<ProfileData> root = module.getTree(); // how to solve this problem?
                                                   // ProfileData class?
        long startTime = 0;
        long endTime = 100;

        TraceEntry traceEntry = traceEntryMap.get(trace);
        if (traceEntry == null) {
            traceEntry = new TraceEntry(trace.getName(), startTime, endTime);
            traceEntryMap.put(trace, traceEntry);
            addToEntryList(parentTrace, Collections.singletonList(traceEntry));
        }

        /*
         * TimeGraphEntry can have children: then they appear as child in the
         * tree viewer on the left ITimeEvent are the intervals that gets drawn
         * in the time graph view on the right side
         */

        System.out.println("Tree");
        map = createHash(root);

        LevelEntry levelAux = null;
        EventEntry eventAux = null;

        // Creating the LevelEntry:
        for (KeyTree key : map.keySet()) {
            System.out.println(key);
            // Add to the HashMap
            if (!levelEntryMap.containsKey(key)) {
                levelAux = new LevelEntry(Integer.toString(key.getLevel()), key.getLevel(), startTime, endTime);
                levelEntryMap.put(trace, levelAux);
            }

            // Creating the Events:
            long timeX = 0;
            // for (KeyTree key : map.keySet()) {
            Node<ProfileData> node = map.get(key);
            if (node != null) {
                eventAux = new EventEntry(node.getNodeLabel(), node.getNodeId(), timeX, timeX + 10);
                if (levelAux != null) {
                    eventEntryMap.put(levelAux, eventAux);
                }
            }
            timeX += 10;
        }

        // Standard
        long resolution = Math.max(1, (endTime - startTime) / getDisplayWidth());
        for (ITimeGraphEntry child : traceEntry.getChildren()) {
            if (monitor.isCanceled()) {
                return;
            }
            if (child instanceof TimeGraphEntry) {
                for (ITimeGraphEntry queueEntry : child.getChildren()) {
                    if (queueEntry instanceof TimeGraphEntry) {
                        TimeGraphEntry eachEntry = (TimeGraphEntry) queueEntry;
                        List<ITimeEvent> eventList = getEventList(eachEntry, startTime, endTime, resolution, monitor, map);
                        if (eventList != null) {
                            for (ITimeEvent event : eventList) {
                                eachEntry.addEvent(event);
                            }
                        }
                        redraw();
                    }
                }
            }
        }
        startTime = endTime;

    }

    /**
     * This method creates the status of the Events
     *
     * @param entry:
     *            Level Entry
     * @param startTime
     * @param endTime
     * @param resolution
     * @param monitor
     * @param root
     * @return
     */
    protected @Nullable List<ITimeEvent> getEventList(TimeGraphEntry entry,
            long startTime, long endTime, long resolution, IProgressMonitor monitor, Map<KeyTree, Node<ProfileData>> map) {

        LevelEntry queueNodesEntry = (LevelEntry) entry;
        Node<ProfileData> auxNode;

        final long realStart = Math.max(startTime, 0);
        final long realEnd = Math.min(endTime,data.getWeight() + 1);

        if (realEnd <= realStart) {
            return null;
        }

        // Event List:
        List<ITimeEvent> eventList = null;
        eventList = new ArrayList<>();

        try {
            int level = queueNodesEntry.getLevel();

            for (KeyTree key : map.keySet()) {
                // Run throughout them and take just the level of this Entry
                if (key.getLevel() == level) {
                    auxNode = map.get(key);
                    // Adding the event:
                    eventList.add(new TimeEvent(queueNodesEntry, auxNode.getProfileData().getStartTime(), auxNode.getProfileData().getEndTime()));
                }
            }

        } catch (StateSystemDisposedException e) {
            /* Ignored */
        }
        return eventList;

    }

    /**
     * This function makes the levelOrderTraversal of a tree, which contains a
     * generic node
     *
     * @param root
     *            a tree first node to be traversed
     * @param visitor
     *            a visitor pattern implementation
     * @return the queue with the level order traversal
     */
    public static <T extends IProfileData> void levelOrderTraversal(Node<T> root, IProfileVisitor<T> visitor) {
        LinkedList<Node<T>> queue = new LinkedList<>();

        queue.add(root);
        while (!queue.isEmpty()) {
            Node<T> current = queue.poll();
            for (Node<T> child : current.getChildren()) {
                queue.add(child);
            }
            visitor.visit(current);
            System.out.println(current);
        }

    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
    }

    // TraceEntry is a trace
    private static class TraceEntry extends TimeGraphEntry {
        public TraceEntry(String name, long startTime, long endTime) {
            super(name, startTime, endTime);
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }
    }

    // LevelEntry is an Level on the tree
    private static class LevelEntry extends TimeGraphEntry {

        private final int fLevelId;

        public LevelEntry(String name, int levelId, long startTime, long endTime) {
            super(name, startTime, endTime);
            fLevelId = levelId;
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }

        public int getLevel() {
            return fLevelId;
        }
    }

    // EventEntry is an Node on the tree
    private static class EventEntry extends TimeGraphEntry {

        private final int fNodeId;

        public EventEntry(String name, int nodeId, long startTime, long endTime) {
            super(name, startTime, endTime);
            fNodeId = nodeId;
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }
    }

    // This function creates a HashMap of level(label) x Node
    private static Map<KeyTree, Node<ProfileData>> createHash(Node<ProfileData> root) {

        Map<KeyTree, Node<ProfileData>> hmap = new HashMap<>();
        Node<ProfileData> current = null;
        Node<ProfileData> pointerParent = null;

        LinkedList<Node<ProfileData>> queue = new LinkedList<>();

        int level = 0;

        queue.add(root);
        while (!queue.isEmpty()) {
            current = queue.poll();
            level = 0;
            pointerParent = current.getParent();
            if (pointerParent != null) {
                while (pointerParent != null) {
                    pointerParent = pointerParent.getParent();
                    level++;
                }
            }
            String label = current.getNodeLabel();
            KeyTree aux = new KeyTree(label, level);

            hmap.put(aux, current);
            for (Node<ProfileData> child : current.getChildren()) {
                queue.add(child);
            }
        }

        return hmap;
    }
}
