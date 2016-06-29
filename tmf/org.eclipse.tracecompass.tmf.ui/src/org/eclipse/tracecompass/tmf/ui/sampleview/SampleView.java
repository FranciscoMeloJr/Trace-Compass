package org.eclipse.tracecompass.tmf.ui.sampleview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileVisitor;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.Node;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileTraversal.KeyTree;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.views.callstack.CallStackEntry;
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

/*
 * TimeGraphEntry can have children: then they appear as child in the tree
 * viewer on the left ITimeEvent are the intervals that gets drawn in the time
 * graph view on the right side
 */

public class SampleView extends CallStackView {

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID1 = "org.eclipse.tracecompass.tmf.ui.views.SampleView";
    private Node<ProfileData> fRoot;

    /**
     * The constructor.
     */
    public SampleView() {
        super();

        setHandleTimeSignals(false);
    }

    /**
     * This method will create the Entry List from the node:
     */

    // Override this method:
    @Override
    protected void buildEntryList(final ITmfTrace trace, final ITmfTrace parentTrace, final IProgressMonitor monitor) {
        System.out.println("buildEntryList " + trace.getName());
        Iterable<CCTAnalysisModule> iter = TmfTraceUtils.getAnalysisModulesOfClass(trace, CCTAnalysisModule.class);
        CCTAnalysisModule module = null;

        setStartTime(0);
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

        // Take the tree and put as fRoot:
        Node<ProfileData> root = module.getTree();
        fRoot = root;

        // For the View:
        TraceEntry traceEntry = null;
        Map<ITmfTrace, LevelEntry> levelEntryMap = new HashMap<>();
        Map<LevelEntry, EventEntry> eventEntryMap = new HashMap<>();

        // Map of the nodes:
        Map<KeyTree, Node<ProfileData>> map;

        //long startTime = 0; //fRoot.getProfileData().getStartTime();
        long start = 0; //startTime;
        //setStartTime(Math.min(getStartTime(), startTime));

        if (monitor.isCanceled()) {
            return;
        }
        long end = 20; //fRoot.getProfileData().getEndTime();
        long endTime = end + 1;

        //setEndTime(Math.max(getEndTime(), endTime));
        setEndTime(20);

        traceEntry = new TraceEntry(trace.getName(), 0, 20);//startTime, endTime);
        addToEntryList(parentTrace, Collections.singletonList(traceEntry));


        System.out.println("Tree");
        map = createHash(root);

        // List of levels:
        LevelEntry levelEntryAux1= null;
        LevelEntry levelEntryAux2= null;

        //List of events:
        EventEntry eventEntryAux1 = null;
        EventEntry eventEntryAux2 = null;

        // Creating the LevelEntry (key is the level)
        levelEntryAux1 = new LevelEntry("level 0", 0, 0, 15); //fRoot.getProfileData().getStartTime(), fRoot.getProfileData().getEndTime() + 1);
        levelEntryAux2 = new LevelEntry("level 1", 0, 16, 20); //fRoot.getProfileData().getStartTime(), fRoot.getProfileData().getEndTime() + 1);
        traceEntry.addChild(levelEntryAux1);
        traceEntry.addChild(levelEntryAux2);

        //Put the levels on the trace:
        levelEntryMap.put(trace, levelEntryAux1);
        levelEntryMap.put(trace, levelEntryAux2);

        // Creating a eventEntry
        eventEntryAux1 = new EventEntry("addr=0x4006a3", 1, 1, 10);//fRoot.getProfileData().getStartTime() + 1, fRoot.getProfileData().getEndTime());
        eventEntryAux2 = new EventEntry("addr=0x400629", 2, 11, 14); //fRoot.getProfileData().getStartTime() + 1, fRoot.getProfileData().getEndTime());


        // Put as child
        List<ITimeEvent> eventList = new ArrayList<>(1);
        ITimeEvent event = new EventNode("main", 37, 2, 9);//fRoot.getProfileData().getStartTime() + 1, fRoot.getProfileData().getEndTime() - 1);

        //Put on levelEntry:
        eventEntryMap.put(levelEntryAux1, eventEntryAux1);
        eventEntryMap.put(levelEntryAux1, eventEntryAux2);

        eventList.add(event);
        eventEntryAux1.addEvent(event);

        //Adding the entries on the levels:
        levelEntryAux1.addChild(eventEntryAux1);
        levelEntryAux1.addChild(eventEntryAux2);

        start = end;
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
    protected @Nullable List<ITimeEvent> getEventList(TimeGraphEntry entry, long resolution, IProgressMonitor monitor, Map<KeyTree, Node<ProfileData>> map) {

        LevelEntry queueNodesEntry = (LevelEntry) entry;
        Node<ProfileData> auxNode;

        /*
         * Do not use the startTime or endTime final long realStart =
         * Math.max(startTime, fRoot.getProfileData().getStartTime()); final
         * long realEnd = Math.min(endTime,
         * fRoot.getProfileData().getEndTime());
         *
         * if (realEnd <= realStart) { return null; }
         */

        // Event List:
        List<ITimeEvent> eventList = null;
        eventList = new ArrayList<>();

        int level = queueNodesEntry.getLevel();

        for (KeyTree key : map.keySet()) {
            // Run throughout them and take just the level of this Entry
            if (key.getLevel() == level) {
                auxNode = map.get(key);
                // Adding the event:
                if (auxNode != null) {
                    eventList.add(new TimeEvent(queueNodesEntry, auxNode.getProfileData().getStartTime(), auxNode.getProfileData().getEndTime()));
                }
            }
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

        //Suggestion
        getTimeGraphViewer().setStartFinishTime(0, 20);

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

        public int getNodeId() {
            return fNodeId;
        }
    }

    // This class is the test for an Interval
    private static class EventNode implements ITimeEvent {

        private long fStartTime;
        private long fEndTime;

        int fNodeId;
        String fLabel;

        public EventNode(String label, int nodeId, long startTime, long endTime) {
            fNodeId = nodeId;
            fStartTime = startTime;
            fEndTime = endTime;
            fLabel = label;
        }

        public long getStartTime() {
            return fStartTime;
        }

        public long getEndTime() {
            return fEndTime;
        }

        public int getAttribute() {
            return fNodeId;
        }

        @Override
        public ITimeGraphEntry getEntry() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getTime() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getDuration() {
            return fEndTime - fStartTime;
        }

        @Override
        public ITimeEvent splitBefore(long splitTime) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ITimeEvent splitAfter(long splitTime) {
            // TODO Auto-generated method stub
            return null;
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

        System.out.println(hmap.size());

        for (KeyTree key : hmap.keySet()) {
            System.out.println(key);
        }
        return hmap;
    }

}