/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Author:
 *     Sonia Farrah
 *******************************************************************************/
package org.eclipse.tracecompass.internal.analysis.timing.ui.flamegraph;

import java.awt.Event;
import java.awt.Window;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuDetectEvent;
import org.eclipse.swt.events.MenuDetectListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.AggregatedCalledFunction;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.CallGraphAnalysis;
import org.eclipse.tracecompass.internal.analysis.timing.core.callgraph.ThreadNode;
import org.eclipse.tracecompass.internal.analysis.timing.ui.Activator;
import org.eclipse.tracecompass.internal.analysis.timing.ui.callgraph.CallGraphAnalysisUI;
import org.eclipse.tracecompass.internal.tmf.ui.ITmfImageConstants;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.tmf.core.signal.TmfSelectionRangeUpdatedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalHandler;
import org.eclipse.tracecompass.tmf.core.signal.TmfSignalManager;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceClosedSignal;
import org.eclipse.tracecompass.tmf.core.signal.TmfTraceSelectedSignal;
import org.eclipse.tracecompass.tmf.core.timestamp.TmfTimestamp;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.editors.ITmfTraceEditor;
import org.eclipse.tracecompass.tmf.ui.sampleview.SampleViewPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.symbols.TmfSymbolProviderUpdatedSignal;
import org.eclipse.tracecompass.tmf.ui.views.TmfView;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphViewer;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.TimeGraphControl;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.PlatformUI;

import com.google.common.annotations.VisibleForTesting;

/**
 * View to display the flame graph .This uses the flameGraphNode tree generated
 * by CallGraphAnalysisUI.
 *
 * @author Sonia Farrah with changes of Francisco M.
 */

public class FlameGraphView extends TmfView {

    /**
     *
     */
    public static final String ID = FlameGraphView.class.getPackage().getName() + ".flamegraphView"; //$NON-NLS-1$

    private static final String SORT_OPTION_KEY = "sort.option"; //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_NAME_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_NAME_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_alpha_rev.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num.gif"); //$NON-NLS-1$
    private static final ImageDescriptor SORT_BY_ID_REV_ICON = Activator.getDefault().getImageDescripterFromPath("icons/etool16/sort_num_rev.gif"); // $NON-NLS-0$

    protected static ArrayList<Integer> Dif = null;

    private TimeGraphViewer fTimeGraphViewer;

    private FlameGraphContentProvider fTimeGraphContentProvider;

    private TimeGraphPresentationProvider fPresentationProvider;
    // Mod:
    private SampleViewPresentationProvider SampleViewPP;

    private ITmfTrace fTrace;

    private final @NonNull MenuManager fEventMenuManager = new MenuManager();
    private Action fSortByNameAction;
    private Action fSortByIdAction;
    /**
     * A plain old semaphore is used since different threads will be competing
     * for the same resource.
     */
    private final Semaphore fLock = new Semaphore(1);
    private Action fResetScaleAction;
    private Action fInvertion;

    private IStructuredSelection fRoots;

    // Mod:
    static CallGraphAnalysis callGraphAnalysis;

    /**
     * Constructor
     */
    public FlameGraphView() {
        super(ID);
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        fTimeGraphViewer = new TimeGraphViewer(parent, SWT.NONE);
        fTimeGraphContentProvider = new FlameGraphContentProvider();
        // Mod presentation provider:
        SampleViewPP = new SampleViewPresentationProvider();

        // fPresentationProvider = new FlameGraphPresentationProvider();
        fTimeGraphViewer.setTimeGraphContentProvider(fTimeGraphContentProvider);
        fTimeGraphViewer.setTimeGraphProvider(SampleViewPP);
        IEditorPart editor = getSite().getPage().getActiveEditor();
        if (editor instanceof ITmfTraceEditor) {
            ITmfTrace trace = ((ITmfTraceEditor) editor).getTrace();
            if (trace != null) {
                traceSelected(new TmfTraceSelectedSignal(this, trace));
            }
        }
        contributeToActionBars();
        loadSortOption();
        TmfSignalManager.register(this);
        getSite().setSelectionProvider(fTimeGraphViewer.getSelectionProvider());
        createTimeEventContextMenu();
        fTimeGraphViewer.getTimeGraphControl().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDoubleClick(MouseEvent e) {
                TimeGraphControl timeGraphControl = getTimeGraphViewer().getTimeGraphControl();
                ISelection selection = timeGraphControl.getSelection();
                if (selection instanceof IStructuredSelection) {
                    for (Object object : ((IStructuredSelection) selection).toList()) {
                        if (object instanceof FlamegraphEvent) {
                            FlamegraphEvent event = (FlamegraphEvent) object;
                            long startTime = event.getTime();
                            long endTime = startTime + event.getDuration();
                            getTimeGraphViewer().setStartFinishTime(startTime, endTime);
                            break;
                        }
                    }
                }
            }
        });
    }

    /**
     * Get the time graph viewer
     *
     * @return the time graph viewer
     */
    @VisibleForTesting
    public TimeGraphViewer getTimeGraphViewer() {
        return fTimeGraphViewer;
    }

    /**
     * Handler for the trace selected signal
     *
     * @param signal
     *            The incoming signal
     */
    @TmfSignalHandler
    public void traceSelected(final TmfTraceSelectedSignal signal) {
        fTrace = signal.getTrace();
        if (fTrace != null) {
            CallGraphAnalysis flamegraphModule = TmfTraceUtils.getAnalysisModuleOfClass(fTrace, CallGraphAnalysis.class, CallGraphAnalysisUI.ID);
            buildFlameGraph(flamegraphModule);
        }
    }

    /**
     * Get the necessary data for the flame graph and display it
     *
     * @param callGraphAnalysis
     *            the callGraphAnalysis
     */
    @VisibleForTesting
    public void buildFlameGraph(CallGraphAnalysis callGraphAnalysis) {
        /*
         * Note for synchronization:
         *
         * Acquire the lock at entry. then we have 4 places to release it
         *
         * 1- if the lock failed
         *
         * 2- if the data is null and we have no UI to update
         *
         * 3- if the request is cancelled before it gets to the display
         *
         * 4- on a clean execution
         */
        try {
            fLock.acquire();
        } catch (InterruptedException e) {
            Activator.getDefault().logError(e.getMessage(), e);
            fLock.release();
        }
        if (callGraphAnalysis == null) {
            fTimeGraphViewer.setInput(null);
            fLock.release();
            return;
        }
        fTimeGraphViewer.setInput(callGraphAnalysis.getSegmentStore());
        callGraphAnalysis.schedule();
        Job j = new Job(Messages.CallGraphAnalysis_Execution) {

            @Override
            protected IStatus run(IProgressMonitor monitor) {
                if (monitor.isCanceled()) {
                    fLock.release();
                    return Status.CANCEL_STATUS;
                }
                callGraphAnalysis.waitForCompletion(monitor);
                Display.getDefault().asyncExec(() -> {

                    // CallGraphAnalysis tests:

                    // This gets the thread nodes:
                    fTimeGraphViewer.setInput(callGraphAnalysis.getThreadNodes());
                    // reset the start and finish time:
                    fTimeGraphViewer.resetStartFinishTime();
                    fLock.release();
                    experiences(callGraphAnalysis);

                    Traversal(callGraphAnalysis);
                    experiments(callGraphAnalysis);
                });
                return Status.OK_STATUS;
            }
        };
        j.schedule();
    }

    // Function to the traversal, allowing the operations:
    private static void experiments(CallGraphAnalysis callGraphAn) {
        @NonNull
        List<ThreadNode> listThreads = callGraphAn.getThreadNodes();

        System.out.println("Size listThreads " + listThreads.size());

        // run over the threads
        // their own list <aggregated functions>
        for (ThreadNode eachThreadNode : listThreads) {

            /*
             * @NonNull Collection<@NonNull AggregatedCalledFunction> x =
             * eachThreadNode.getChildren();
             * System.out.println(eachThreadNode.getSymbol() + " " + x.size());
             */
            levelOrderTraversal(eachThreadNode);
        }

    }

    // Mod Level Order traversal:
    public static LinkedList<AggregatedCalledFunction> levelOrderTraversal(ThreadNode root) {
        System.out.print("levelOrderTraversal ");
        LinkedList<AggregatedCalledFunction> queue = new LinkedList<>();
        LinkedList<AggregatedCalledFunction> result = new LinkedList<>();

        queue.add(root);
        while (!queue.isEmpty()) {
            AggregatedCalledFunction current = queue.poll();
            System.out.print(current.toString());
            for (AggregatedCalledFunction child : current.getChildren()) {
                queue.add(child);
                System.out.println(child.getDepth());
            }
            result.add(current);

        // run over the threads
        // each thread has a list of aggregated called functions, which have
        // their own list <aggregated functions>
        for (ThreadNode eachThreadNode : listThreads) {

            /*
             * @NonNull Collection<@NonNull AggregatedCalledFunction> x =
             * eachThreadNode.getChildren();
             * System.out.println(eachThreadNode.getSymbol() + " " + x.size());
             */
            CallGraphAnalysis.levelOrderTraversal(eachThreadNode);
        }
    }

    /**
     * Await the next refresh
     *
     * @throws InterruptedException
     *             something took too long
     */
    @VisibleForTesting
    public void waitForUpdate() throws InterruptedException {
        /*
         * wait for the semaphore to be available, then release it immediately
         */
        fLock.acquire();
        fLock.release();
    }

    /**
     * Trace is closed: clear the data structures and the view
     *
     * @param signal
     *            the signal received
     */
    @TmfSignalHandler
    public void traceClosed(final TmfTraceClosedSignal signal) {
        if (signal.getTrace() == fTrace) {
            fTimeGraphViewer.setInput(null);
        }
    }

    @Override
    public void setFocus() {
        fTimeGraphViewer.setFocus();
    }

    // ------------------------------------------------------------------------
    // Helper methods
    // ------------------------------------------------------------------------

    private void createTimeEventContextMenu() {
        fEventMenuManager.setRemoveAllWhenShown(true);
        TimeGraphControl timeGraphControl = fTimeGraphViewer.getTimeGraphControl();
        final Menu timeEventMenu = fEventMenuManager.createContextMenu(timeGraphControl);

        timeGraphControl.addTimeGraphEntryMenuListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent event) {
                /*
                 * The TimeGraphControl will call the TimeGraphEntryMenuListener
                 * before the TimeEventMenuListener. We need to clear the menu
                 * for the case the selection was done on the namespace where
                 * the time event listener below won't be called afterwards.
                 */
                timeGraphControl.setMenu(null);
                event.doit = false;
            }
        });
        timeGraphControl.addTimeEventMenuListener(new MenuDetectListener() {
            @Override
            public void menuDetected(MenuDetectEvent event) {
                Menu menu = timeEventMenu;
                if (event.data instanceof FlamegraphEvent) {
                    timeGraphControl.setMenu(menu);
                    return;
                }
                timeGraphControl.setMenu(null);
                event.doit = false;
            }
        });

        fEventMenuManager.addMenuListener(new IMenuListener() {
            @Override
            public void menuAboutToShow(IMenuManager manager) {
                fillTimeEventContextMenu(fEventMenuManager);
                fEventMenuManager.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));
            }
        });
        getSite().registerContextMenu(fEventMenuManager, fTimeGraphViewer.getSelectionProvider());
    }

    /**
     * Fill context menu
     *
     * @param menuManager
     *            a menuManager to fill
     */
    protected void fillTimeEventContextMenu(@NonNull IMenuManager menuManager) {
        ISelection selection = getSite().getSelectionProvider().getSelection();
        if (selection instanceof IStructuredSelection) {
            for (Object object : ((IStructuredSelection) selection).toList()) {
                if (object instanceof FlamegraphEvent) {
                    final FlamegraphEvent flamegraphEvent = (FlamegraphEvent) object;
                    menuManager.add(new Action(Messages.FlameGraphView_GotoMaxDuration) {
                        @Override
                        public void run() {
                            ISegment maxSeg = flamegraphEvent.getStatistics().getMaxSegment();
                            TmfSelectionRangeUpdatedSignal sig = new TmfSelectionRangeUpdatedSignal(this, TmfTimestamp.fromNanos(maxSeg.getStart()), TmfTimestamp.fromNanos(maxSeg.getEnd()));
                            broadcast(sig);
                        }
                    });

                    menuManager.add(new Action(Messages.FlameGraphView_GotoMinDuration) {
                        @Override
                        public void run() {
                            ISegment minSeg = flamegraphEvent.getStatistics().getMinSegment();
                            TmfSelectionRangeUpdatedSignal sig = new TmfSelectionRangeUpdatedSignal(this, TmfTimestamp.fromNanos(minSeg.getStart()), TmfTimestamp.fromNanos(minSeg.getEnd()));
                            broadcast(sig);
                        }
                    });
                }
            }
        }
    }

    private void contributeToActionBars() {
        IActionBars bars = getViewSite().getActionBars();
        fillLocalToolBar(bars.getToolBarManager());
        fillLocalMenu(bars.getMenuManager());
    }

    private void fillLocalToolBar(IToolBarManager manager) {
        manager.add(getSortByNameAction());
        manager.add(getSortByIdAction());
        manager.add(new Separator());

    }

    private Action getSortByNameAction() {
        if (fSortByNameAction == null) {
            fSortByNameAction = new Action(Messages.FlameGraph_SortByThreadName, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    SortOption sortOption = fTimeGraphContentProvider.getSortOption();
                    if (sortOption == SortOption.BY_NAME) {
                        setSortOption(SortOption.BY_NAME_REV);
                    } else {
                        setSortOption(SortOption.BY_NAME);
                    }
                }
            };
            fSortByNameAction.setToolTipText(Messages.FlameGraph_SortByThreadName);
            fSortByNameAction.setImageDescriptor(SORT_BY_NAME_ICON);
        }
        return fSortByNameAction;
    }

    // Mod:
    // This function is for a small selection menu - size is hard coded:
    protected void fillLocalMenu(IMenuManager manager) {
        // super.fillLocalMenu(manager);

        manager.add(getSortByUnknown());

        MenuManager itemA = new MenuManager("Select Execution A: ");
        // fFlatAction = createFlatAction();
        // fFlatAction = createFlatAction();

        // Test just to put information on the
        // System.out.println(FUNCTION_NAMES.size());
        int size = 10;
        if (fRoots != null) {
            size = fRoots.size();
        }
        for (int i = 0; i < size; i++) {
            itemA.add(createTreeSelection(Integer.toString(i), 1));
        }
        manager.add(new Separator());
        manager.add(itemA);
        // ItemB
        MenuManager itemB = new MenuManager("Select Execution B: ");

        // Test just to put information on the
        for (int i = 0; i < size; i++) {
            itemB.add(createTreeSelection(Integer.toString(i), 2));
        }

        manager.add(new Separator());
        manager.add(itemB);

        // Threshold:
        MenuManager itemTh = new MenuManager("Select threshold:");

        // Test just to put information on the
        int sizeT = 10;
        for (int i = 0; i <= sizeT; i++) {
            itemTh.add(selectThreshold(i));
        }

        manager.add(new Separator());
        manager.add(itemTh);
        // Merger:
        manager.add(new Separator());
        manager.add(getMergeAction());

        // Delimiters
        manager.add(new Separator());

        // Classification
        manager.add(getClassificationAction());

    }

    private Action getSortByIdAction() {
        if (fSortByIdAction == null) {
            fSortByIdAction = new Action(Messages.FlameGraph_SortByThreadId, IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    SortOption sortOption = fTimeGraphContentProvider.getSortOption();
                    if (sortOption == SortOption.BY_ID) {
                        setSortOption(SortOption.BY_ID_REV);
                    } else {
                        setSortOption(SortOption.BY_ID);
                    }
                }
            };
            fSortByIdAction.setToolTipText(Messages.FlameGraph_SortByThreadId);
            fSortByIdAction.setImageDescriptor(SORT_BY_ID_ICON);
        }
        return fSortByIdAction;
    }

    private void setSortOption(SortOption sortOption) {
        // reset defaults
        getSortByNameAction().setChecked(false);
        getSortByNameAction().setImageDescriptor(SORT_BY_NAME_ICON);
        getSortByIdAction().setChecked(false);
        getSortByIdAction().setImageDescriptor(SORT_BY_ID_ICON);

        if (sortOption.equals(SortOption.BY_NAME)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_NAME);
            getSortByNameAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_NAME_REV)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_NAME_REV);
            getSortByNameAction().setChecked(true);
            getSortByNameAction().setImageDescriptor(SORT_BY_NAME_REV_ICON);
        } else if (sortOption.equals(SortOption.BY_ID)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_ID);
            getSortByIdAction().setChecked(true);
        } else if (sortOption.equals(SortOption.BY_ID_REV)) {
            fTimeGraphContentProvider.setSortOption(SortOption.BY_ID_REV);
            getSortByIdAction().setChecked(true);
            getSortByIdAction().setImageDescriptor(SORT_BY_ID_REV_ICON);
        }
        saveSortOption();
        fTimeGraphViewer.refresh();
    }

    private void saveSortOption() {
        SortOption sortOption = fTimeGraphContentProvider.getSortOption();
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            section = settings.addNewSection(getClass().getName());
        }
        section.put(SORT_OPTION_KEY, sortOption.name());
    }

    private void loadSortOption() {
        IDialogSettings settings = Activator.getDefault().getDialogSettings();
        IDialogSettings section = settings.getSection(getClass().getName());
        if (section == null) {
            return;
        }
        String sortOption = section.get(SORT_OPTION_KEY);
        if (sortOption == null) {
            return;
        }
        setSortOption(SortOption.fromName(sortOption));
    }

    /**
     * Symbol map provider updated
     *
     * @param signal
     *            the signal
     */
    @TmfSignalHandler
    public void symbolMapUpdated(TmfSymbolProviderUpdatedSignal signal) {
        if (signal.getSource() != this) {
            fTimeGraphViewer.refresh();
        }
    }

    /// Mods:
    private static boolean fDiff;
    int threshold;
    private Action fSortByUnknown;

    private Action fInvertionAction;

    // Mod:
    // This function is for a small selection menu - size is hard coded:
    protected void fillLocalMenu(IMenuManager manager) {
        // super.fillLocalMenu(manager);

        manager.add(getReset());

        manager.add(getInvertion());
        manager.add(new Separator());
        manager.add(getDifferential());

        MenuManager itemA = new MenuManager("Select Execution A: ");
        // fFlatAction = createFlatAction();
        // fFlatAction = createFlatAction();

        // Test just to put information on the
        // System.out.println(FUNCTION_NAMES.size());
        int size = 10;
        List<ThreadNode> listThreads = callGraphAnalysis.getThreadNodes();

        if (listThreads != null) {
            size = listThreads.size();
        }

        for (int i = 0; i < size; i++) {
            itemA.add(createTreeSelection(Integer.toString(i), 1));
        }
        // manager.add(new Separator());
        manager.add(itemA);
        // ItemB
        MenuManager itemB = new MenuManager("Select Execution B: ");

        // Test just to put information on the
        for (int i = 0; i < size; i++) {
            itemB.add(createTreeSelection(Integer.toString(i), 2));
        }

        manager.add(itemB);

        // Threshold:
        MenuManager itemTh = new MenuManager("Select threshold:");

        // Test just to put information on the
        int sizeThreshold = 10;
        for (int i = 0; i <= sizeThreshold; i++) {
            itemTh.add(selectThreshold(i));
        }

        manager.add(itemTh);
        // Merger:
        manager.add(new Separator());
        manager.add(getMergeAction());

        // Delimiters
        manager.add(new Separator());

        // Classification
        manager.add(getClassificationAction());

    }

    // this function is related with the threshold comparison:
    private IAction selectThreshold(int i) {
        // IAction action1 = new Action(Integer.toString(i),
        // IAction.AS_CHECK_BOX){ };
        // IAction.AS_RADIO_BUTTON
        IAction action = new Action(Integer.toString(i), IAction.AS_RADIO_BUTTON) {
            @Override
            public void run() {
                threshold = i;
            }

        };
        action.setToolTipText("Select the threshold for comparison");
        if ((!fDiff) && (i == 0)) {
            action.setChecked(true);
        }
        return action;

    }

    private static Action getMergeAction() {
        Action mergeButton = null;
        mergeButton = new Action() {
            @Override
            public void run() {
                System.out.println("Automatic merge");
                List<ThreadNode> result = callGraphAnalysis.mergeFL();
                callGraphAnalysis.setThreadNodes(result);
            }
        };
        mergeButton.setText("Grouping selection");
        mergeButton.setToolTipText("This button will automatically merge similiar executions");
        mergeButton.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_CONFLICT));

        return mergeButton;
    }

    // ------------------------------------------------------------------------
    // Mods:
    // ------------------------------------------------------------------------

    private Action getSortByUnknown() {
        if (fSortByUnknown == null) {
            fSortByUnknown = new Action("Differential", IAction.AS_CHECK_BOX) {
                @Override
                public void run() {
                    SampleViewPresentationProvider SampleViewPP = new SampleViewPresentationProvider();
                    fTimeGraphViewer.setTimeGraphProvider(SampleViewPP);
                }
            };
            fSortByUnknown.setToolTipText("Differential");
        }
        return fSortByUnknown;
    }

    private Action getDifferential() {
        if (fDifferential == null) {
            fDifferential = new Action("Execute Differential", IAction.AS_PUSH_BUTTON) {
                @Override
                public void run() {
                    // Change the presentation provider:
                    // SampleViewPresentationProvider SampleViewPP = new
                    // SampleViewPresentationProvider();
                    // fTimeGraphViewer.setTimeGraphProvider(SampleViewPP);
                    // Redraw ():
                    System.out.println("Differential");

                    List<ThreadNode> result = callGraphAnalysis.differential(Dif);
                    callGraphAnalysis.setThreadNodes(result);
                }
            };
            fSortByUnknown.setToolTipText("Differential");
        }
        return fSortByUnknown;
    }

    private Action getInvertion() {
        if (fInvertion == null) {
            fInvertion = new Action("Invertion", IAction.AS_PUSH_BUTTON) {
                @Override
                public void run() {

                }
            };
            fInvertion.setToolTipText("Invertion");
        }
        return fInvertion;
    }


    private static IAction createTreeSelection(String name, int i) {
        IAction action = new Action(name, IAction.AS_CHECK_BOX) { // AS_DROP_DOWN_MENU
            @Override
            public void run() {
                if (i == 1) {
                    // System.out.println("Taking the tree A:" + name);
                    // Call the differential function
                    if (Dif == null) {
                        Dif = new ArrayList<>();
                        Dif.add(Integer.parseInt(name));

                    } else {
                        Dif.add(Integer.parseInt(name));
                    }

                } else {
                    System.out.println("Taking the tree B:" + name + " " + "(" + Dif.get(0) + " " + Dif.get(0) + ") ");
                    // Call the differential function
                }
            }

        };
        action.setToolTipText("Selection of execution for comparison");
        return action;
    }

    // test with bookmark:
    private Action getDelimitationActionDialog(String labelText, String initialLabel, int kind) {
        Action fToggleBookmarkAction = null;
        fToggleBookmarkAction = new Action() {

            @Override
            public void runWithEvent(Event event) {
                final AddDelimiterDialog dialog = new AddDelimiterDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), initialLabel);
                if (dialog.open() == Window.OK) {
                    final String label = dialog.getValue();
                    System.out.println(label + dialog.fBegin);
                    String BeginDelimiter;
                    String EndDelimiter;
                    // final RGBA rgba = dialog.getColorValue();
                    // IMarkerEvent bookmark = new MarkerEvent(null, time,
                    // duration, IMarkerEvent.BOOKMARKS, rgba, label, true);
                    if (kind == 0) {
                        BeginDelimiter = label;
                    } else {
                        EndDelimiter = label;
                    }
                    // resetAnalysis(BeginDelimiter, EndDelimiter);

                }
            }
        };
        fToggleBookmarkAction.setText(labelText);
        fToggleBookmarkAction.setToolTipText(Messages.TmfTimeGraphViewer_DelimitationText);
        // fToggleBookmarkAction.setImageDescriptor(ADD_BOOKMARK);

        return fToggleBookmarkAction;
    }

    public Action getClassificationAction() {
        // resetScale
        fInvertionAction = new Action() {
            @Override
            public void run() {
                System.out.println("Simulation");
                // CCTAnalysisModule.classificationTest();
                // Calling the Variation Classification
                System.out.println("Test");
                // test
                ArrayList<Integer> A = new ArrayList<>();
                A.add(10);
                A.add(11);
                A.add(12);
                A.add(13);
                A.add(100);
                A.add(101);
                A.add(102);
                A.add(103);

                // Run over the tree:
                // CCTAnalysisModule.RunClassification();

            }
        };
        fInvertionAction.setText("Classification");
        fInvertionAction.setToolTipText("Classification using variation method");
        fInvertionAction.setImageDescriptor(Activator.getDefault().getImageDescripterFromPath(ITmfImageConstants.IMG_UI_NODE_START));
        return fInvertionAction;
    }

    // Reset function:
    public Action getReset() {

        if (fResetScaleAction == null) {
            fResetScaleAction = new Action("Reset", IAction.AS_PUSH_BUTTON) {
                @Override
                public void run() {
                    reset();
                }

                // Reset mod:
                private void reset() {
                    // Find the longest thread and set it as time range
                }
            };
            fResetScaleAction.setToolTipText("Reset");
        }
        return fResetScaleAction;
    }

}
