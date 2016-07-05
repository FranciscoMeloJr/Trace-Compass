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
package org.eclipse.tracecompass.internal.lttng2.ust.ui.analysis.callstack;

import java.text.Format;
import java.text.NumberFormat;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.tracecompass.analysis.timing.ui.views.segmentstore.SubSecondTimeWithUnitFormat;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.ui.symbols.ISymbolProvider;
import org.eclipse.tracecompass.tmf.ui.symbols.SymbolProviderManager;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.model.NullTimeEvent;
import org.eclipse.tracecompass.tmf.ui.widgets.timegraph.widgets.Utils;

import com.google.common.collect.ImmutableMap;

/**
 * Presentation provider for the Flame graph view, based on the generic TMF
 * presentation provider.
 *
 * @author Sonia Farrah
 */
public class FlameGraphPresentationProvider extends TimeGraphPresentationProvider {
    /** Number of colors used for call stack events */
    public static final int NUM_COLORS = 360;

    private FlameGraphView fView;

    private Integer fAverageCharWidth;

    private enum State {
        MULTIPLE(new RGB(100, 100, 100)), EXEC(new RGB(0, 200, 0));

        private final RGB rgb;

        private State(RGB rgb) {
            this.rgb = rgb;
        }
    }

    /**
     * Constructor
     *
     * @since 2.0
     */
    public FlameGraphPresentationProvider() {
    }

    @Override
    public StateItem[] getStateTable() {
        final float saturation = 0.6f;
        final float brightness = 0.6f;
        StateItem[] stateTable = new StateItem[NUM_COLORS + 1];
        stateTable[0] = new StateItem(State.MULTIPLE.rgb, State.MULTIPLE.toString());
        for (int i = 0; i < NUM_COLORS; i++) {
            RGB rgb = new RGB(i, saturation, brightness);
            stateTable[i + 1] = new StateItem(rgb, State.EXEC.toString());
        }
        return stateTable;
    }

    @Override
    public boolean displayTimesInTooltip() {
        return false;
    }

    private static final Format FORMATTER = new SubSecondTimeWithUnitFormat();

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event, long hoverTime) {
        return ImmutableMap.of(
                Messages.FlameGraph_Duration, String.format("%s", FORMATTER.format(event.getDuration())), //$NON-NLS-1$
                Messages.FlameGraph_Percentage, NumberFormat.getPercentInstance().format(((FlamegraphEvent) event).getPercentage()), // $NON-NLS-1$
                Messages.FlameGraph_NbreCalls, NumberFormat.getIntegerInstance().format(((FlamegraphEvent) event).getNbreCalls()) // $NON-NLS-1$
        );
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof FlamegraphEvent) {
            FlamegraphEvent flameGraphEvent = (FlamegraphEvent) event;
            return flameGraphEvent.getValue() + 1;
        } else if (event instanceof NullTimeEvent) {
            return INVISIBLE;
        }
        return State.MULTIPLE.ordinal();
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
        if (fAverageCharWidth == null) {
            fAverageCharWidth = gc.getFontMetrics().getAverageCharWidth();
        }
        if (bounds.width <= fAverageCharWidth) {
            return;
        }
        if (!(event instanceof FlamegraphEvent)) {
            return;
        }
        ITmfTrace activeTrace = TmfTraceManager.getInstance().getActiveTrace();
        if (activeTrace != null) {
            ISymbolProvider x = SymbolProviderManager.getInstance().getSymbolProvider(activeTrace);
            String funcName = x.getSymbolText((((FlamegraphEvent) event).getAddress()));
            gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
            if (funcName != null) {
                Utils.drawText(gc, funcName, bounds.x, bounds.y, bounds.width, bounds.height, true, true);
            } else {
                Utils.drawText(gc, "0x" + Long.toHexString(((FlamegraphEvent) event).getAddress()), bounds.x, bounds.y, bounds.width, bounds.height, true, true); //$NON-NLS-1$
            }
        }
    }

    /**
     * @return The flame graph view
     */
    public FlameGraphView getView() {
        return fView;
    }

    /**
     * @param view
     *            The flame graph view
     */
    public void setView(FlameGraphView view) {
        fView = view;
    }

}
