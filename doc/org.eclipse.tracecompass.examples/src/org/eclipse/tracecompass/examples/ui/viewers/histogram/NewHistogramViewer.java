/*******************************************************************************
 * Copyright (c) 2013, 2014 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Alexandre Montplaisir - Initial API and implementation
 *   Bernd Hufmann - Updated to new TMF chart framework
 *******************************************************************************/
package org.eclipse.tracecompass.examples.ui.viewers.histogram;

import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule;
import org.eclipse.tracecompass.internal.analysis.os.linux.core.profile.CCTAnalysisModule.testStatistics;
import org.eclipse.tracecompass.tmf.core.statistics.ITmfStatistics;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.xycharts.barcharts.TmfBarChartViewer;
import org.swtchart.Chart;
import org.swtchart.IAxis;
import org.swtchart.ISeries;
import org.swtchart.LineStyle;

/**
 * Histogram Viewer implementation based on TmfBarChartViewer.
 *
 * @author Alexandre Montplaisir
 * @author Bernd Hufmann
 */
public class NewHistogramViewer extends TmfBarChartViewer {

    /**
     * Creates a Histogram Viewer instance.
     *
     * @param parent
     *            The parent composite to draw in.
     */
    public NewHistogramViewer(Composite parent) {
        super(parent, null, null, null, TmfBarChartViewer.MINIMUM_BAR_WIDTH);

        Chart swtChart = getSwtChart();

        IAxis xAxis = swtChart.getAxisSet().getXAxis(0);
        IAxis yAxis = swtChart.getAxisSet().getYAxis(0);

        /* Hide the grid */
        xAxis.getGrid().setStyle(LineStyle.NONE);
        yAxis.getGrid().setStyle(LineStyle.NONE);

        /* Hide the legend */
        swtChart.getLegend().setVisible(false);

        addSeries("Number of events", Display.getDefault().getSystemColor(SWT.COLOR_BLUE).getRGB()); //$NON-NLS-1$
    }

    @Override
    protected void readData(final ISeries series, final long start, final long end, final int nb) {
        if (getTrace() != null) {
            int number = 10;
            final double y[] = new double[number];

            Thread thread = new Thread("Histogram viewer update") { //$NON-NLS-1$
                @Override
                public void run() {
                    double x[] = getXAxis(start, end, number);
                    final long yLong[] = new long[number];
                    Arrays.fill(y, 0.0);
                    int size = 0;
                    /* Add the values for each trace */
                    for (ITmfTrace trace : TmfTraceManager.getTraceSet(getTrace())) {
                        /* Retrieve the statistics object */
                        final CCTAnalysisModule statsMod = TmfTraceUtils.getAnalysisModuleOfClass(trace, CCTAnalysisModule.class, CCTAnalysisModule.ID);

                        if (statsMod == null) {
                            /* No statistics module available for this trace */
                            continue;
                        }
                        statsMod.waitForCompletion();
                        final ITmfStatistics stats = statsMod.getStatistics();
                        if (stats == null) {
                            throw new IllegalStateException();
                        }
                        if (stats instanceof testStatistics) {
                            testStatistics tS = (testStatistics) stats;
                            size = tS.getSize();
                        }
                        List<Long> values = stats.histogramQuery(start, end, number);

                        System.out.println("nb " + nb);
                        for (int i = 0; i < size; i++) {
                            long temp = values.get(i);
                            yLong[i] += temp;
                            System.out.println(temp);
                        }
                    }

                    /*
                     * for (int i = 0; i < nb; i++) { y[i] += yLong[i]; /*
                     * casting from long to double }
                     */
                    double xx[] = new double[size];
                    double yy[] = new double[size];

                    for (int i = 0; i < size; i++) {
                        xx[i] += i;
                        yy[i] += yLong[i]; /* casting from long to double */
                    }
                    /* Update the viewer */
                    drawChart(series, xx, yy);
                }
            };
            thread.start();
        }
        return;
    }
}
