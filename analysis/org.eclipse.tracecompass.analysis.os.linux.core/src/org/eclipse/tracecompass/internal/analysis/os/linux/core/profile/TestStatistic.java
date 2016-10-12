package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.tmf.core.statistics.ITmfStatistics;

public class TestStatistic implements ITmfStatistics {

    ArrayList<Long> arrayList;

    TestStatistic(ArrayList<Long> array) {
        arrayList = array;
    }

    // returns the arrayList
    @Override
    public List<Long> histogramQuery(long start, long end, int nb) {

        return arrayList;
    }

    @Override
    public long getEventsTotal() {
        long nb = arrayList.size();
        return nb;
    }

    @Override
    public Map<@NonNull String, @NonNull Long> getEventTypesTotal() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public long getEventsInRange(long start, long end) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public Map<String, Long> getEventTypesInRange(long start, long end) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void dispose() {
        // TODO Auto-generated method stub

    }

    public int getSize() {
        return arrayList.size();
    }

}