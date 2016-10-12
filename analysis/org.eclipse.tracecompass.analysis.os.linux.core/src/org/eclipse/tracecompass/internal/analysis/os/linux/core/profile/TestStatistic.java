package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.util.ArrayList;
import java.util.Collections;
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
    // Calculates the correlation of two distributions:
    public static void calculateCorrelation(ArrayList<Double> data1, ArrayList<Double> data2) {

        //Ordering:
        Collections.sort(data1);
        Collections.sort(data2);

        ArrayList<Double> x = new ArrayList<>();
        ArrayList<Double> xx = new ArrayList<>();
        ArrayList<Double> y = new ArrayList<>();
        ArrayList<Double> yy = new ArrayList<>();

        ArrayList<Double> xy = new ArrayList<>();

        Double mean1 = (double) 0;
        Double mean2 = (double) 0;
        Double value = (double) 0;
        Double value2 = (double) 0;

        // total:
        Double total1 = (double) 0;
        Double total2 = (double) 0;
        Double total3 = (double) 0;

        int minSize = (data1.size() < data2.size()) ? data1.size() : data2.size();

        for (int j = 0; j < minSize; j++) {
            mean1 += data1.get(j);
        }
        mean1 /= data1.size();
        for (int j = 0; j < minSize; j++) {
            value = data1.get(j) - mean1;
            x.add(value);
            value2 = value * value;
            xx.add(value2);
            total2 += value2;
        }

        for (int j = 0; j < minSize; j++) {
            mean2 += data2.get(j);
        }
        mean2 /= data2.size();

        for (int j = 0; j < minSize; j++) {
            value = data2.get(j) - mean2;
            y.add(value);
            value2 = value * value;
            yy.add(value2);
            total3 += value2;
        }


        for (int j = 0; j < minSize; j++) {
            xy.add(x.get(j) * y.get(j));
        }
        for (int j = 0; j < xy.size(); j++) {
            total1 += xy.get(j);
        }

        //Showing Pearson correlation:
        Double result = (total1 / (total2 * total3));
        System.out.format(" Correlation %.10f%n", result);

    }
}