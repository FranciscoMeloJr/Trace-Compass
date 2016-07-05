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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.common.core.NonNullUtils;
import org.eclipse.tracecompass.tmf.ui.viewers.tree.TmfTreeViewerEntry;

/**
 * Class to calculate the call stack segment store statistics
 *
 * @author Sonia Farrah
 */
public class CallStackStatistics {
    // Statistics for the aggregation by address
    private HashMap<Long, FunctionInfos> AdressStatistics = new HashMap<>();
    // Statistics for the aggregation by address and depth
    private HashMap<Integer, HashMap<Long, FunctionInfos>> Depthstatistics = new HashMap<>();
    private HashMap<Integer,Long> AdressPositions=new HashMap<>();
    private HashMap<Integer,ArrayList<Long>> DepthPositions=new HashMap<>();
    /**
     * Constructor
     */
    public CallStackStatistics() {
    }

    /**
     * Calculate and add the statistics of a function into the statistics store
     *
     * @param function
     *            A call stack event
     */
    public void calculateStatistics(CalledFunction function) {
        FunctionInfos infos = AdressStatistics.get(function.getAddr());
        Long Duration = function.getEnd() - function.getStart();
        if (infos != null) {
            infos.setTotalTime(infos.getTotalTime() + Duration);
            infos.setTotalSelfTime(infos.getTotalSelfTime() + function.getFinalSelfTime());
            infos.incrementNbreCalls();
            infos.setDepth(function.getDepth());
            for (CalledFunction child : function.getChildren()) {
                infos.addCallee(Long.toString(child.getAddr()));
            }
            if (function.getParent() != null) {
                infos.addCaller(Long.toString(function.getParent().getAddr()));
            } else {
                infos.addCaller("null"); //$NON-NLS-1$
            }

            AdressStatistics.replace(function.getAddr(), infos);

        } else {
            infos = new FunctionInfos(Duration, function.getFinalSelfTime(), function.getAddr());
            infos.incrementNbreCalls();
            infos.setDepth(function.getDepth());
            for (CalledFunction child : function.getChildren()) {
                infos.addCallee("0x" + Long.toHexString(child.getAddr())); //$NON-NLS-1$
            }
            if (function.getParent() != null) {
                infos.addCaller(Long.toHexString(function.getParent().getAddr())); // $NON-NLS-1$
            } else {
                infos.addCaller("null"); //$NON-NLS-1$
            }
            AdressStatistics.put(function.getAddr(), infos);
            AdressPositions.put(AdressStatistics.size(),function.getAddr());
        }
        updateDepthStatistics(function);
    }

    private void updateDepthStatistics(CalledFunction function) {

        FunctionInfos infos = null;
        Long functionAdress = function.getAddr();
        int functionDepth = function.getDepth();
        Long Duration = function.getEnd() - function.getStart();
        if (!Depthstatistics.containsKey(function.getDepth())) {
            Depthstatistics.put(functionDepth, new HashMap<Long, FunctionInfos>());
            DepthPositions.put(functionDepth, new ArrayList<Long>());
        }
        if (!NonNullUtils.checkNotNull(Depthstatistics.get(functionDepth)).containsKey(functionAdress)) {
            infos = new FunctionInfos(Duration, function.getFinalSelfTime(), functionAdress);
            infos.incrementNbreCalls();
            infos.setDepth(functionDepth);
            for (CalledFunction child : function.getChildren()) {
                infos.addCallee("0x" + Long.toHexString(child.getAddr())); //$NON-NLS-1$
            }
            if (function.getParent() != null) {
                infos.addCaller("0x" + Long.toHexString(function.getParent().getAddr())); //$NON-NLS-1$
            } else {
                infos.addCaller("null"); //$NON-NLS-1$
            }
            NonNullUtils.checkNotNull(Depthstatistics.get(functionDepth)).put(functionAdress, infos);
            NonNullUtils.checkNotNull(DepthPositions.get(functionDepth)).add(functionAdress);
        } else {
            infos = NonNullUtils.checkNotNull(Depthstatistics.get(functionDepth)).get(functionAdress);
            if (infos != null) {
                infos.setTotalTime(infos.getTotalTime() + Duration);
                infos.setTotalSelfTime(infos.getTotalSelfTime() + function.getFinalSelfTime());
                infos.incrementNbreCalls();
                infos.setDepth(functionDepth);
                for (CalledFunction child : function.getChildren()) {
                    infos.addCallee("0x" + Long.toHexString(child.getAddr())); //$NON-NLS-1$
                }
                if (function.getParent() != null) {
                    infos.addCaller("0x" + Long.toHexString(function.getParent().getAddr())); //$NON-NLS-1$
                } else {
                    infos.addCaller("null"); //$NON-NLS-1$
                }
                NonNullUtils.checkNotNull(Depthstatistics.get(functionDepth)).replace(functionAdress, infos);
            }
        }
    }

    /**
     * @param address
     *            The address of a function
     * @return The total self time of the function
     */
    public Long getTotalSelfTime(Long address) {
        FunctionInfos infos = AdressStatistics.get(address);
        if (infos != null) {
            return infos.getTotalSelfTime();
        }
        return (long) 0;
    }

    /**
     * @param address
     *            The address of a function
     * @return The total time of the function
     */
    public Long getTotalfTime(Long address) {
        FunctionInfos infos = AdressStatistics.get(address);
        if (infos != null) {
            return infos.getTotalTime();
        }
        return (long) 0;
    }

    /**
     * Class storing the statistics for one function
     *
     * @author Sonia Farrah
     *
     */
    public class FunctionInfos extends TmfTreeViewerEntry {
        private Long TotalTime;
        private Long TotalSelfTime;
        private Long Address;
        private int nbreCalls = 0;
        private int depth = 0;
        private HashMap<String, Integer> callers = new HashMap<>();
        private HashMap<String, Integer> callees = new HashMap<>();

        FunctionInfos(Long TotTime, Long SelfTime, Long fAddress) {
            super(fAddress.toString());
            TotalTime = TotTime;
            TotalSelfTime = SelfTime;
            Address = fAddress;
        }

        /**
         * @return The total time of a function
         */
        public Long getTotalTime() {
            return TotalTime;
        }

        /**
         * @param totalTime
         *            The total time of a function
         */
        public void setTotalTime(Long totalTime) {
            TotalTime = totalTime;
        }

        /**
         * @return The total self time of a function
         */
        public Long getTotalSelfTime() {
            return TotalSelfTime;
        }

        /**
         * @param totalSelfTime
         *            The total self time
         */
        public void setTotalSelfTime(Long totalSelfTime) {
            TotalSelfTime = totalSelfTime;
        }

        @Nullable
        Comparator<?> getComparator() {
            return null;
        }

        /**
         * @return The address of a function
         */
        public Long getAddress() {
            return Address;
        }

        /**
         * @param address
         *            The address of a function
         */
        public void setAddress(Long address) {
            Address = address;
        }

        /**
         * @return The number of calls of a function
         */
        public int getNbreCalls() {
            return nbreCalls;
        }

        /**
         * @param calls
         *            The number of calls of a function
         */
        public void setNbreCalls(int calls) {
            nbreCalls = calls;
        }

        /**
         * Increment the number of calls of a function
         */
        public void incrementNbreCalls() {
            nbreCalls++;
        }

        /**
         * @return The callers of a function
         */
        public HashMap<String, Integer> getCallers() {
            return callers;
        }

        /**
         * @param callersList
         *            The list of callers
         */
        public void setCallers(HashMap<String, Integer> callersList) {
            callers = callersList;
        }

        /**
         * @return The callees of a function
         */
        public HashMap<String, Integer> getCallees() {
            return callees;
        }

        /**
         * @param callees
         *            The list of callees
         */
        public void setCallees(HashMap<String, Integer> Callees) {
            callees = Callees;
        }

        /**
         * Add The caller into the list of callers and calculate its occurrence
         *
         * @param Caller
         *            The caller to add in the callers list
         */
        public void addCaller(String Caller) {
            if (!callers.containsKey(Caller)) {
                callers.put(Caller, 1);
            } else {
                callers.replace(Caller, NonNullUtils.checkNotNull(callers.get(Caller)) + 1);
            }
        }

        /**
         * Add The callee into the list of callees and calculate its occurrence
         *
         *
         * @param Callee
         *            The callee to add to a in the callees list
         */
        public void addCallee(String Callee) {
            if (!callees.containsKey(Callee)) {
                callees.put(Callee, 1);
            } else {
                callees.replace(Callee, NonNullUtils.checkNotNull(callees.get(Callee)) + 1);
            }
        }

        /**
         * @return The depth of a function
         */
        public int getDepth() {
            return depth;
        }

        /**
         * @param Depth
         *            The depth of a function
         */
        public void setDepth(int Depth) {
            depth = Depth;
        }
    }

    /**
     * @return The statistics of a specific function
     */
    public HashMap<Long, FunctionInfos> getStatistics() {
        return AdressStatistics;
    }

    /**
     * @return The depth agregation's statistics
     */
    public HashMap<Integer, HashMap<Long, FunctionInfos>> getDepthstatistics() {
        return Depthstatistics;
    }

    /**
     * @param depthstatistics
     *            The depth agregation's statistics
     */
    public void setDepthstatistics(HashMap<Integer, HashMap<Long, FunctionInfos>> depthstatistics) {
        Depthstatistics = depthstatistics;
    }

    /**
     * @return A dictionnary
     */
    public HashMap<Integer, Long> getAdressPositions() {
        return AdressPositions;
    }

    /**
     * @param positions
     *            Functions's positions in the statistics map
     */
    public void setDepthPositions(HashMap<Integer,ArrayList<Long>> positions) {
        DepthPositions = positions;
    }

    /**
     * @return Functions's positions in the statistics map
     *
     */
    public HashMap<Integer,ArrayList<Long>> getDepthPositions() {
        return DepthPositions;
    }

    /**
     * @param positions
     */
    public void setAdressPositions(HashMap<Integer, Long> positions) {
        AdressPositions = positions;
    }
}