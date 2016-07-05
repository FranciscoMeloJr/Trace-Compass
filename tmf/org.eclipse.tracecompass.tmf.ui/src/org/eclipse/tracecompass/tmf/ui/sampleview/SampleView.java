package org.eclipse.tracecompass.tmf.ui.sampleview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.IProfileVisitor;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.Node;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileData;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.ProfileTraversal.KeyTree;
import org.eclipse.tracecompass.internal.tmf.ui.Activator;
import org.eclipse.tracecompass.internal.tmf.ui.Messages;
import org.eclipse.tracecompass.tmf.core.analysis.IAnalysisModule;
import org.eclipse.tracecompass.tmf.core.signal.TmfWindowRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.ITmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimeRange;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestampDelta;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.ui.views.timegraph.AbstractTimeGraphView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.ITimeGraphTimeListener;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphContentProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphTimeEvent;
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

public class SampleView extends AbstractTimeGraphView {// extends CallStackView
                                                       // { //

    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID1 = "org.eclipse.tracecompass.tmf.ui.views.SampleView";
    private Node<ProfileData> fRoot;

    // Map of the tree:
    Map<KeyTree, Node<ProfileData>> fMap = null;

    // Related with presentation provider:
    private final Map<ITmfTrace, ISymbolProvider> fSymbolProviders = new HashMap<>();

    // Messages:
    private static final String[] COLUMN_NAMES1 = new String[] {
            Messages.CallStackView_FunctionColumn,
            Messages.CallStackView_DepthColumn,
            Messages.CallStackView_EntryTimeColumn,
            Messages.CallStackView_ExitTimeColumn,
            Messages.SampleView_A
    };

    private static final String[] FILTER_COLUMN_NAMES = new String[] {
            Messages.SampleView_NameColumn
    };

    /**
     * The constructor.
     */
    public SampleView() {
        // super();
        super(ID1, new SampleViewPresentationProvider());
        ((SampleViewPresentationProvider) getPresentationProvider()).setSampleView(this);
        setTreeColumns(COLUMN_NAMES1);
        setTreeLabelProvider(new SampleViewTreeLabelProvider());
        setEntryComparator(new SampleViewComparator());
        setFilterColumns(FILTER_COLUMN_NAMES);
        setFilterContentProvider(new SampleViewFilterContentProvider());
        setFilterLabelProvider(new SampleViewTreeLabelProvider());

        setHandleTimeSignals(false);

        // redraw();
        // Maybe setting the start and end time:
        // setStartTime(0);
        // setEndTime(15);
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

        // Get the data from the module
        Node<ProfileData> root = module.getTree();
        fRoot = root;
        // Find the beginning and end of this view and set it using (i think)
        // getTimeGraphWrapper or something.getTimeGraphViewer.setStart...
        // getTimeGraphViewer().setSelectionRange(0, 25, true);

        // Build your entries

        // For each entry, get the list of events

        // enjoy the show

        // For the View:
        TraceEntry traceEntry = null;
        Map<ITmfTrace, LevelEntry> levelEntryMap = new HashMap<>();
        Map<LevelEntry, EventEntry> eventEntryMap = new HashMap<>();

        // Map of the nodes:
        Map<KeyTree, Node<ProfileData>> map;

        // Symbols:
        ISymbolProvider provider = fSymbolProviders.get(trace);
        if (provider == null) {
            provider = SymbolProviderManager.getInstance().getSymbolProvider(trace);
            provider.loadConfiguration(monitor);
            fSymbolProviders.put(trace, provider);
        }

        long startTime = 0; // fRoot.getProfileData().getStartTime();
        long start = startTime;
        setStartTime(0);

        if (monitor.isCanceled()) {
            return;
        }
        long end = 15;// fRoot.getProfileData().getEndTime();
        long endTime = end + 1;

        setEndTime(16);

        traceEntry = new TraceEntry(trace.getName(), startTime, endTime);
        addToEntryList(parentTrace, Collections.singletonList(traceEntry));

        System.out.println("Tree");
        map = createHash(root);

        // Making it global:
        fMap = map;

        LevelEntry levelEntryAux = null;
        LevelEntry levelEntryAux1 = null;

        EventEntry eventEntryAux = null;
        EventEntry eventEntryAux1 = null;

        // Creating the LevelEntry (key is the level)
        levelEntryAux = new LevelEntry("level 0", 57, 0, 10);

        // Creating the LevelEntry (key is the level)
        levelEntryAux1 = new LevelEntry("level 1", 57, 0, 10);

        // Creating a eventEntry
        eventEntryAux = new EventEntry("function 1", 37, 1, 15);
        eventEntryAux1 = new EventEntry("function 2", 25, 9, 15);

        // Put as child
        List<ITimeEvent> eventList = new ArrayList<>(4);
        ITimeEvent event = new TimeEvent(eventEntryAux, 1, 13, 1); //so it will end at 8
        ITimeEvent event1 = new TimeEvent(eventEntryAux1, 1, 5, 1); //so it will end at 14
        ITimeEvent event2 = new TimeEvent(eventEntryAux1, 7, 7, 1);

        eventEntryMap.put(levelEntryAux, eventEntryAux);
        eventEntryMap.put(levelEntryAux1, eventEntryAux1);

        eventList.add(event);
        eventList.add(event1);
        eventList.add(event2);

        eventEntryAux.addEvent(event);
        eventEntryAux1.addEvent(event2);
        eventEntryAux1.addEvent(event1);

        levelEntryAux.addChild(eventEntryAux);
        levelEntryAux1.addChild(eventEntryAux1);

        traceEntry.addChild(levelEntryAux);
        traceEntry.addChild(levelEntryAux1);
        levelEntryMap.put(trace, levelEntryAux);
        levelEntryMap.put(trace, levelEntryAux1);

        // Consumer:
        /*
         * Consumer omitted
         */

        if (parentTrace == getTrace()) {
            synchronized (this) {
                setStartTime(0);
                setEndTime(16);
            }
            synchingToTime(0);//getTimeGraphViewer().getSelectionBegin());
            refresh();
            // getTimeGraphViewer().refresh();
        }
        // start = end;
        // refresh();
    }

    /**
     * This method creates the status of the Events
     *
     * @param entry
     *            Level Entry
     * @param startTime
     * @param endTime
     * @param resolution
     * @param monitor
     * @param root
     * @return
     */

    // This method is called when you zoom:
    @Override
    protected List<ITimeEvent> getEventList(TimeGraphEntry entry, long startTime, long endTime, long resolution, IProgressMonitor monitor) {

        System.out.println("Entry name: " + entry.getName());
        List<ITimeEvent> eventList = null;
        if (entry instanceof EventEntry) {
            // Event List:
            eventList = new ArrayList<>();

            // Used to run throughout the tree:
            Node<ProfileData> auxNode;

            // This map takes from fMap:
            Map<KeyTree, Node<ProfileData>> map = fMap;

            int level = 0; // queueNodesEntry.getLevel();

            for (KeyTree key : map.keySet()) {
                // Run throughout them and take just the level of this Entry
                if (key.getLevel() == level) {
                    auxNode = map.get(key);
                    // Adding the event:
                    if (auxNode != null) {
                        eventList.add(new TimeEvent(entry, 1, 5, 1));// auxNode.getProfileData().getStartTime(),
                                                                     // auxNode.getProfileData().getEndTime()));
                    }
                }
            }
            eventList.add(new TimeEvent(entry, 1, 5, 1));
        }
        return eventList;

    }

    /*
     * private void buildStatusEvents(ITmfTrace trace, EventEntry
     * entry, @NonNull IProgressMonitor monitor, long start, long end) {
     *
     * long resolution = Math.max(1, (end - 0) / getDisplayWidth());
     * List<ITimeEvent> eventList = getEventList(entry, start, end + 1,
     * resolution, monitor); if (eventList != null) {
     * entry.setEventList(eventList); System.out.println(entry); } if (trace ==
     * getTrace()) { redraw(); } }
     */
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

    /*
     * Maybe change this method here:
     *
     * @Override public void createPartControl(Composite parent) {
     * super.createPartControl(parent);
     *
     * }
     *
     */
    // This function creates the widgets on the screen
    @Override
    public void createPartControl(Composite parent) {
        // This will give problem, because the sample view extends call stack
        // view
        super.createPartControl(parent);

        TmfTimeRange range = new TmfTimeRange(TmfTimestamp.fromNanos(0), TmfTimestamp.fromNanos(15));
        broadcast(new TmfWindowRangeUpdatedSignal(SampleView.this, range));
        getTimeGraphViewer().setStartFinishTime(0, 15);

        // Changes on the fTimeGraphWrapper
        // getTimeGraphViewer().

        getTimeGraphViewer().addTimeListener(new ITimeGraphTimeListener() {
            @Override
            public void timeSelected(TimeGraphTimeEvent event) {
                synchingToTime(0);
            }
        });
    }

    // TraceEntry is a trace
    protected static class TraceEntry extends TimeGraphEntry {
        public TraceEntry(String name, long startTime, long endTime) {
            super(name, startTime, endTime);
        }

        @Override
        public boolean hasTimeEvents() {
            return false;
        }
    }

    // LevelEntry is an Level on the tree
    protected static class LevelEntry extends TimeGraphEntry implements Comparable<LevelEntry> {

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

        @Override
        public int compareTo(LevelEntry obj) {
            if (this.fLevelId == obj.fLevelId) {
                return 0;
            }

            return Integer.compare(this.fLevelId, obj.fLevelId);

        }
    }

    // EventEntry is an Node on the tree
    protected static class EventEntry extends TimeGraphEntry implements Comparable<EventEntry> {

        private final int fNodeId;
        private final int fDepth = 2;

        public EventEntry(String name, int nodeId, long startTime, long endTime) {
            super(name, startTime, endTime);
            fNodeId = nodeId;
        }

        public int getNodeId() {
            return fNodeId;
        }

        public int getDepth() {
            return fDepth;
        }

        @Override
        public int compareTo(EventEntry obj) {
            if (this.fNodeId == obj.fNodeId) {
                return 0;
            }

            return Integer.compare(this.fNodeId, obj.fNodeId);

        }
    }

    // This class is the test for an Interval
    protected static class EventNode implements ITimeEvent {

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

    // getFunctionName:
    String getFunctionName() { // (ITmfTrace trace, int processId, long
                               // timestamp, ITmfStateValue nameValue) {
        /*
         * long address = Long.MAX_VALUE; String name = ""; //$NON-NLS-1$ try {
         *
         * if (nameValue.getType() == Type.STRING) { name =
         * nameValue.unboxStr(); try { address = Long.parseLong(name, 16); }
         * catch (NumberFormatException e) { // ignore } } else if
         * (nameValue.getType() == Type.INTEGER) { name = "0x" +
         * Integer.toUnsignedString(nameValue.unboxInt(), 16); //$NON-NLS-1$
         * address = nameValue.unboxInt(); } else if (nameValue.getType() ==
         * Type.LONG) { name = "0x" +
         * Long.toUnsignedString(nameValue.unboxLong(), 16); //$NON-NLS-1$
         * address = nameValue.unboxLong(); } } catch (StateValueTypeException
         * e) { } if (address != Long.MAX_VALUE) { ISymbolProvider provider =
         * fSymbolProviders.get(trace); if (provider != null) { String symbol =
         * provider.getSymbolText(processId, timestamp, address); if (symbol !=
         * null) { name = symbol; } } }
         */
        return "0xfunction";
    }

    // Copied methods:
    private static final Image PROCESS_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/process_obj.gif"); //$NON-NLS-1$
    private static final Image LEVEL_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/thread_obj.gif"); //$NON-NLS-1$
    private static final Image EVENT_STACKFRAME_IMAGE = Activator.getDefault().getImageFromPath("icons/obj16/stckframe_obj.gif"); //$NON-NLS-1$

    // Lavel Provider
    private static class SampleViewTreeLabelProvider extends TreeLabelProvider {

        @Override
        public Image getColumnImage(Object element, int columnIndex) {
            if (columnIndex == 0) {
                if (element instanceof TraceEntry) {
                    return PROCESS_IMAGE;
                } else if (element instanceof LevelEntry) {
                    return LEVEL_IMAGE;
                } else if (element instanceof EventEntry) {
                    EventEntry entry = (EventEntry) element;
                    if (entry.getName().length() > 0) {
                        return EVENT_STACKFRAME_IMAGE;
                    }
                }
            }
            return null;
        }

        @Override
        public String getColumnText(Object element, int columnIndex) {
            if (element instanceof EventEntry) {
                EventEntry entry = (EventEntry) element;
                if (columnIndex == 0) {
                    return entry.getName();
                } else if (columnIndex == 1 && entry.getName().length() > 0) {
                    int depth = entry.getDepth();
                    return Integer.toString(depth);
                } else if (columnIndex == 2 && entry.getName().length() > 0) {
                    ITmfTimestamp ts = TmfTimestamp.fromNanos(entry.getStartTime());
                    return ts.toString();
                } else if (columnIndex == 3 && entry.getName().length() > 0) {
                    ITmfTimestamp ts = TmfTimestamp.fromNanos(entry.getEndTime());
                    return ts.toString();
                } else if (columnIndex == 4 && entry.getName().length() > 0) {
                    ITmfTimestamp ts = new TmfTimestampDelta(entry.getEndTime() - entry.getStartTime(), ITmfTimestamp.NANOSECOND_SCALE);
                    return ts.toString();
                }
            } else if (element instanceof ITimeGraphEntry) {
                if (columnIndex == 0) {
                    return ((ITimeGraphEntry) element).getName();
                }
            }
            return ""; //$NON-NLS-1$
        }

    }

    // Sample View Comparator
    private class SampleViewComparator implements Comparator<ITimeGraphEntry> {
        @Override
        public int compare(ITimeGraphEntry o1, ITimeGraphEntry o2) {
            // return o1.compareTo(o2);

            if (o1 instanceof EventEntry && o2 instanceof EventEntry) {
                EventEntry first = (EventEntry) o1;
                EventEntry second = (EventEntry) o1;
                return first.compareTo(second);
            } else if (o1 instanceof LevelEntry && o2 instanceof LevelEntry) {
                LevelEntry first = (LevelEntry) o1;
                LevelEntry second = (LevelEntry) o1;
                return first.compareTo(second);
            }
            return 0;
        }
    }

    // SampleView Filter Content:
    private class SampleViewFilterContentProvider extends TimeGraphContentProvider {
        @Override
        public boolean hasChildren(Object element) {
            if (element instanceof TraceEntry) {
                return super.hasChildren(element);
            }
            return false;
        }

        @Override
        public ITimeGraphEntry[] getChildren(Object parentElement) {
            if (parentElement instanceof TraceEntry) {
                return super.getChildren(parentElement);
            }
            return new ITimeGraphEntry[0];
        }
    }

}
