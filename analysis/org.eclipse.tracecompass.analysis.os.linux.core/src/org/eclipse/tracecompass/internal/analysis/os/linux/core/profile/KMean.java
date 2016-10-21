package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Collections;

//from:https://radixcode.com/k-mean-clustering-algorithm-implementation-in-c-java/

/**
 * @author francisco
 * This Class defines the k-means + Elbow method + max gap heuristic
 * This heuristic does not guarantee the best k
 */

public class KMean {

    int k;
    int noOfItems;
    static ArrayList<Integer> dataItems;
    static ArrayList<Integer> cz;
    static ArrayList<Integer> oldCz;
    static ArrayList<Integer> row;
    static ArrayList<ArrayList<Integer>> groups;

    /**
     * @param k
     * @param noOfItems1
     * @param numbers
     * @return
     */
    public static ArrayList<ArrayList<Double>> KMeanD(int numberGroups, int numberItems, ArrayList<Double> numbers) {

        ArrayList<Double> dataItemsD = new ArrayList<>();
        ArrayList<Double> czD = new ArrayList<>();
        ArrayList<Double> oldCzD = new ArrayList<>();
        ArrayList<Double> rowD = new ArrayList<>();
        ArrayList<ArrayList<Double>> groupsD = new ArrayList<>();

        int k1 = numberGroups;
        int noOfItems1 = numberItems;

        for (int i = 0; i < k1; i++) {
            groupsD.add(new ArrayList<>());
        }
        dataItemsD = numbers;
        for (int i = 0; i < noOfItems1; i++) {
            // System.out.println("Enter Value for: " + (i + 1) + " item");
            if (i < k1) {
                czD.add(dataItemsD.get(i));
                // System.out.println("C" + (i + 1) + " is " + cz.get(i));
            }
        }
        int iter = 1;
        do {
            for (double aItem : dataItemsD) {
                for (Double c : czD) {
                    rowD.add(abs(c - aItem));
                }
                groupsD.get(rowD.indexOf(Collections.min(rowD))).add(aItem);
                rowD.removeAll(rowD);
            }
            for (int i = 0; i < k1; i++) {
                if (iter == 1) {
                    oldCzD.add(czD.get(i));
                } else {
                    oldCzD.set(i, czD.get(i));
                }
                if (!groupsD.get(i).isEmpty()) {
                    czD.set(i, averageD(groupsD.get(i)));
                }
            }
            if (!czD.equals(oldCzD)) {
                for (int i = 0; i < groupsD.size(); i++) {
                    groupsD.get(i).removeAll(groupsD.get(i));
                }
            }
            iter++;
        } while (!czD.equals(oldCzD));

        // Group Double
        return groupsD;
    }

    public KMean(int k, int noOfItems, ArrayList<Integer> numbers) {
        this.k = k;
        this.noOfItems = noOfItems;
        dataItems = new ArrayList<>();
        cz = new ArrayList<>();
        oldCz = new ArrayList<>();
        row = new ArrayList<>();
        groups = new ArrayList<>();

        for (int i = 0; i < k; i++) {
            groups.add(new ArrayList<>());
        }
        dataItems = numbers;
        for (int i = 0; i < noOfItems; i++) {
            // System.out.println("Enter Value for: " + (i + 1) + " item");
            if (i < k) {
                cz.add(dataItems.get(i));
                // System.out.println("C" + (i + 1) + " is " + cz.get(i));
            }
        }
        int iter = 1;
        do {
            for (int aItem : dataItems) {
                for (int c : cz) {
                    row.add(abs(c - aItem));
                }
                groups.get(row.indexOf(Collections.min(row))).add(aItem);
                row.removeAll(row);
            }
            for (int i = 0; i < k; i++) {
                if (iter == 1) {
                    oldCz.add(cz.get(i));
                } else {
                    oldCz.set(i, cz.get(i));
                }
                if (!groups.get(i).isEmpty()) {
                    cz.set(i, average(groups.get(i)));
                }
            }
            if (!cz.equals(oldCz)) {
                for (int i = 0; i < groups.size(); i++) {
                    groups.get(i).removeAll(groups.get(i));
                }
            }
            iter++;
        } while (!cz.equals(oldCz));
    }

    // Get Groups:
    public static ArrayList<ArrayList<Integer>> getGroups() {
        return groups;

    }
    // Function to with a given array of doubles:
    public static int test(ArrayList<Double> numbers) {
        ArrayList<Double> resultSSE = new ArrayList<>();
        ArrayList<ArrayList<Double>> group;
        // testing all the ks:
        for (int j = 1; j < numbers.size(); j++) {
            // K, size and arraylist of numbers
            group = KMean.KMeanD(j, numbers.size(), numbers);
            //Elbow method:
            resultSSE.add(KMean.ElbowMethodD(group));
        }

        System.out.println(resultSSE);

        return calculateBestK(resultSSE);
    }

    // Function to test the OpK-means: k-means + elbow + max gap heuristic
    public static void test() {
        ArrayList<Integer> testValues = new ArrayList<>();
        ArrayList<Double> resultSSE = new ArrayList<>();

        // test 1
        testValues.add(10);
        testValues.add(11);
        testValues.add(12);
        testValues.add(13);
        testValues.add(100);
        testValues.add(101);
        testValues.add(102);
        testValues.add(103);
        testValues.add(1003);
        testValues.add(1003);
        testValues.add(1003);
        testValues.add(1003);

        // testing all the ks:
        for (int j = 1; j < testValues.size(); j++) {
            // K, size and arraylist of numbers
            KMean.execute(j, testValues.size(), testValues);
            ArrayList<ArrayList<Integer>> group = KMean.getGroups();
            resultSSE.add(KMean.ElbowMethod(group));
        }
        // biggest gap:
        System.out.println("Best number of groups: " + calculateBestK(resultSSE));

        // test 2
        ArrayList<Double> testDouble = new ArrayList<>();
        testDouble.add(10.1);
        testDouble.add(11.1);
        testDouble.add(12.2);
        testDouble.add(132.2);
        testDouble.add(100.2);
        testDouble.add(101.2);
        testDouble.add(102.2);
        testDouble.add(103.2);
        testDouble.add(1003.2);
        testDouble.add(1003.5);
        testDouble.add(1003.8);
        testDouble.add(1003.28);

        // testing all the ks:
        for (int j = 1; j < testDouble.size(); j++) {
            // K, size and arraylist of numbers
            System.out.println(KMean.KMeanD(j, testDouble.size(), testDouble));

        }
    }

    public static void execute(int givenK, int nItems, ArrayList<Integer> numbers) {
        // K and number of items:
        int k = givenK;
        int noOfItems = nItems; // input.nextInt();
        new KMean(k, noOfItems, numbers);
    }
    //Execute and returns the groups:
    public static ArrayList<ArrayList<Double>> executeD(int givenK, ArrayList<Double> numbers) {
        // K and number of items:
        return KMean.KMeanD(givenK, numbers.size(), numbers);
    }

    public static int average(ArrayList<Integer> list) {
        int sum = 0;
        for (Integer value : list) {
            sum = sum + value;
        }
        return sum / list.size();
    }

    public static double averageD(ArrayList<Double> list) {
        double sum = 0;
        for (Double value : list) {
            sum = sum + value;
        }
        return sum / list.size();
    }

    // Evalution:
    public static Double ElbowMethod(ArrayList<ArrayList<Integer>> ClassificationGroup) {

        Double SSE = (double) 0;


            // Calculate the SSE for each cluster and then sum them:
            ArrayList<Double> Centroids = new ArrayList<>();

            Double eachDistance;

            // For each group, calculates the centroid:
            // System.out.println("Size" + groups.size());
            for (int i = 0; i < ClassificationGroup.size(); i++) {
                ArrayList<Integer> x = ClassificationGroup.get(i);
                Double temp = (double) 0;
                int j;
                for (j = 0; j < x.size(); j++) {
                    temp += x.get(j);
                }
                Centroids.add(temp /= j);
                // System.out.println(Centroids.get(i));
            }

            // SSE Calculation:
            // The SSE is defined as the sum of the squared distance between
            // each member of the cluster and its centered. Mathematically:
            for (int i = 0; i < ClassificationGroup.size(); i++) {
                // each group:
                ArrayList<Integer> x = ClassificationGroup.get(i);
                int j;
                for (j = 0; j < x.size(); j++) {
                    eachDistance = (x.get(j) - Centroids.get(i)) * (x.get(j) - Centroids.get(i));
                    SSE += eachDistance;
                }
            }
            System.out.println("K : " + ClassificationGroup.size() + " SSE " + SSE);

        return SSE;
    }

    //This function calculates the ElbowMethod:
    public static Double ElbowMethodD(ArrayList<ArrayList<Double>> ClassificationGroup){
            Double SSE = (double) 0;
            // Calculate the SSE for each cluster and then sum them:
            ArrayList<Double> Centroids = new ArrayList<>();

            Double eachDistance;

            // For each group, calculates the centroid:
            // System.out.println("Size" + groups.size());
            for (int i = 0; i < ClassificationGroup.size(); i++) {
                ArrayList<Double> x = ClassificationGroup.get(i);
                Double temp = (double) 0;
                int j;
                for (j = 0; j < x.size(); j++) {
                    temp += x.get(j);
                }
                Centroids.add(temp /= j);
                // System.out.println(Centroids.get(i));
            }

            // SSE Calculation:
            // The SSE is defined as the sum of the squared distance between
            // each member of the cluster and its centered. Mathematically:
            for (int i = 0; i < ClassificationGroup.size(); i++) {
                // each group:
                ArrayList<Double> x = ClassificationGroup.get(i);
                int j;
                for (j = 0; j < x.size(); j++) {
                    eachDistance = (x.get(j) - Centroids.get(i)) * (x.get(j) - Centroids.get(i));
                    SSE += eachDistance;
                }
            }
            System.out.println("K : " + ClassificationGroup.size() + " SSE " + SSE);

        return SSE;
    }

    // Heuristic to get the best k: maximum gap distance
    public static int calculateBestK(ArrayList<Double> resultSSE) {

        Double gap = (double) 0;
        Double current;
        int maxp1 = 0;
        int minp1 = 0;
        int i;

        // Heuristic max:
        for (i = 1; i < resultSSE.size(); i++) {
            current = resultSSE.get(i - 1) - resultSSE.get(i);
            if (current > gap) {
                gap = current;
                maxp1 = i;
            }
        }
        // Heuristic min:
        gap = (double) 0;
        for (i = 1; i < resultSSE.size(); i++) {
            current = resultSSE.get(i - 1) - resultSSE.get(i);
            if (current < gap) {
                gap = current;
                minp1 = i;
            }
        }
        // return minp1;

        System.out.println("best k " + maxp1);
        //return minp1;
        return maxp1;
    }
}