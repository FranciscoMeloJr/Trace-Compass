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

    // Calculate the Standard Deviation
    public static Long calculateSTDandCV(ArrayList<Long> array, int type) {

        Long total = (long) 0;
        Long sumTotal = (long) 0;
        Long result = (long) 0;
        Long cov = (long) 0;

        for (int j = 0; j < array.size(); j++) {
            total += array.get(j);
        }

        Long mean = total / array.size();
        for (int j = 0; j < array.size(); j++) {
            sumTotal += (array.get(j) - mean) * (array.get(j) - mean);
        }

        Long Variance = (long) (sumTotal / (double) array.size());

        // In case of STD:
        result = (long) Math.sqrt(Variance);
        if (type == 1) {
            return result;
        }
        // In case of Coefficient of Variation:
        if (type == 2) {
            cov = (long) (result / mean);
            return cov;
        }
        return result;
    }
}