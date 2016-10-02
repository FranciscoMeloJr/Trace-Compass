package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import static java.lang.Math.abs;

import java.util.ArrayList;
import java.util.Collections;

//from:https://radixcode.com/k-mean-clustering-algorithm-implementation-in-c-java/

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
     * @param noOfItems
     * @param numbers
     */
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
            //System.out.println("Enter Value for: " + (i + 1) + " item");
            if (i < k) {
                cz.add(dataItems.get(i));
                //System.out.println("C" + (i + 1) + " is " + cz.get(i));
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
        for (int i = 0; i < cz.size(); i++) {
           // System.out.println("New C" + (i + 1) + " " + cz.get(i));
        }
        for (int i = 0; i < groups.size(); i++) {
            //System.out.println("Group " + (i + 1));
            //System.out.println(groups.get(i).toString());
        }
        //System.out.println("Number of Itrations: " + iter);
    }

    //Function to test the Kmeans + elbow:
    public static void test() {
        ArrayList<Integer> testValues = new ArrayList<>();
        ArrayList<Double> resultSSE = new ArrayList<>();

        //test 1
        testValues.add(1);
        testValues.add(2);
        testValues.add(3);
        testValues.add(4);
        testValues.add(5);
        testValues.add(6);
        testValues.add(7);
        testValues.add(8);
        testValues.add(9);
        testValues.add(10);

        //testing all the ks:
        for(int j = 1; j< testValues.size(); j++){
          //K, size and arraylist of numbers
            KMean.execute(j, testValues.size(), testValues);
            resultSSE.add(KMean.ElbowMethod());
        }
        //biggest gap:
        System.out.println("Result " + calculateGap(resultSSE));
    }
    public static void execute(int givenK, int nItems, ArrayList<Integer> numbers) {
        //K and number of items:
        int k = givenK;
        int noOfItems = nItems; //input.nextInt();
        new KMean(k, noOfItems, numbers);
    }

    public static int average(ArrayList<Integer> list) {
        int sum = 0;
        for (Integer value : list) {
            sum = sum + value;
        }
        return sum / list.size();
    }

    // Evalution:
    public static Double ElbowMethod() {

        // Calculate the SSE for each cluster and then sum them:
        ArrayList<Double> Centroids = new ArrayList<>();
        Double SSE = (double) 0;
        Double eachDistance;

        // For each group, calculates the centroid:
        //System.out.println("Size" + groups.size());
        for (int i = 0; i < groups.size(); i++) {
            ArrayList<Integer> x = groups.get(i);
            Double temp = (double) 0;
            int j;
            for (j = 0; j < x.size(); j++) {
                temp += x.get(j);
            }
            Centroids.add(temp /= j);
            //System.out.println(Centroids.get(i));
        }

        //SSE Calculation:
        //The SSE is defined as the sum of the squared distance between each member of the cluster and its centered. Mathematically:
        for (int i = 0; i < groups.size(); i++) {
            //each group:
            ArrayList<Integer> x = groups.get(i);
            int j;
            for (j = 0; j < x.size(); j++) {
                eachDistance = (x.get(j) - Centroids.get(i))*(x.get(j) - Centroids.get(i));
                SSE += eachDistance;
            }
        }
        System.out.println("K : "+ groups.size() +" SSE " + SSE);
        return SSE;
    }

    //Function to select the biggest gap:
    public static int calculateGap(ArrayList<Double> resultSSE){

        Double gap = (double) 0;
        Double current;
        int maxp1 = 0, maxp2 = 1;
        int minp1 = 0, minp2 = 1;
        int i;

        for(i = 1; i< resultSSE.size();i++ ){
            current = resultSSE.get(i-1) - resultSSE.get(i);
            if(current > gap ){
                gap = current;
                maxp1 = i;
                maxp2 = i-1;
            }
        }
        //Heuristic:
        for(i = 1; i< resultSSE.size();i++ ){
            current = resultSSE.get(i-1) - resultSSE.get(i);
            if(current < gap ){
                gap = current;
                minp1 = i;
                minp2 = i-1;
            }
        }
        System.out.println("gap" + gap);
        return minp1;
    }
}