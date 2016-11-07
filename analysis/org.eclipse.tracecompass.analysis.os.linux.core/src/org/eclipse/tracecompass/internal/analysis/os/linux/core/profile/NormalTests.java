package org.eclipse.tracecompass.internal.analysis.os.linux.core.profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Random;

/*
 * This class is used to test the concept of tracking a problem by several classifications:
 * */
public class NormalTests {

    // All the executions:
    static class Executions {
        int id;
        int tam = 10;
        ArrayList<Double> distributionET = new ArrayList<>();
        LinkedHashMap<Integer, ArrayList<Double>> hmET = new LinkedHashMap<>();

        ArrayList<Double> distributionCPU = new ArrayList<>();
        LinkedHashMap<Integer, ArrayList<Double>> hmCPU = new LinkedHashMap<>();

        ArrayList<Double> distributionWrite = new ArrayList<>();
        LinkedHashMap<Integer, ArrayList<Double>> hmWrite = new LinkedHashMap<>();

        ArrayList<Double> distributionRandom = new ArrayList<>();

        ArrayList<Run> runs;

        Executions() {
            id = 1;
            // Elapsed Time:
            int stdDev = 15;
            int mean = 20;

            // first Normal:
            stdDev = 15;
            mean = 10;
            fulfil(distributionET, hmET, stdDev, mean, 1);

            // second Normal:
            stdDev = 15;
            mean = 70;
            fulfil(distributionET, hmET, stdDev, mean, 2);

            // CPU Time
            stdDev = 15;
            mean = 20;

            // first Normal:
            stdDev = 15;
            mean = 10;
            fulfil(distributionCPU, hmCPU, stdDev, mean, 1);

            // second Normal:
            stdDev = 15;
            mean = 70;
            fulfil(distributionCPU, hmCPU, stdDev, mean, 2);

            // Write
            stdDev = 15;
            mean = 20;

            // first Normal:
            stdDev = 15;
            mean = 10;
            fulfil(distributionWrite, hmWrite, stdDev, mean, 1);

            // second Normal:
            stdDev = 15;
            mean = 70;
            fulfil(distributionWrite, hmWrite, stdDev, mean, 2);

            runs = createsRuns();
        }

        void fulfil(ArrayList<Double> distribution, LinkedHashMap<Integer, ArrayList<Double>> hm, int stdDev, int mean, int type) {

            Random randomno = new Random();
            Double aux;
            // first Normal:
            for (int i = 0; i < this.tam; i++) {
                aux = randomno.nextGaussian() * stdDev + mean;
                distribution.add(aux);
                hashAdd(hm, type, aux);
                // hm.put(1, aux);
            }
        }

        // This function creates each Run:
        ArrayList<Run> createsRuns() {
            ArrayList<Run> arrayR = new ArrayList<>();
            Run tempR;
            for (int i = 0; i < tam; i++) {
                tempR = new Run(i, distributionET.get(i), distributionCPU.get(i), distributionWrite.get(i));
                arrayR.add(tempR);
            }
            return arrayR;

        }

        // This function finds the run:
        Run findRun(Double number) {
            for (int i = 0; i < tam; i++) {
                Run aux = runs.get(i);
                if (number == aux.getCpu()) {
                    return aux;
                }
                if (number == aux.getElapsed()) {
                    return aux;
                }
                if (number == aux.getWrite()) {
                    return aux;
                }
            }

            return null;
        }

        // This function creates a random distribution:
        void createRandomDist(int max, int min) {
            Random random = new Random();
            for (int i = 0; i < tam; i++) {
                double aux = random.nextInt(max - min + 1) + min;
                distributionRandom.add(aux);
            }
        }

        // Function used to shuffle the runs information:
        void suffle() {
            if (distributionRandom != null) {
                Collections.sort(distributionRandom);
            }

            Collections.sort(distributionET);
            Collections.sort(distributionCPU);
            Collections.sort(distributionWrite);
        }

        // getters of the hash:
        LinkedHashMap<Integer, ArrayList<Double>> getWriteHM() {
            return hmET;
        }

        LinkedHashMap<Integer, ArrayList<Double>> getCpuHM() {
            return hmCPU;
        }

        LinkedHashMap<Integer, ArrayList<Double>> getElapsedHM() {
            return hmWrite;
        }

        // getters of the distributions:
        ArrayList<Double> getWrite() {
            return distributionWrite;
        }

        ArrayList<Double> getCpu() {
            return distributionCPU;
        }

        ArrayList<Double> getElapsed() {
            return distributionET;
        }
    }

    // Each execution:
    static class Run {
        Double cpu;
        Double elapsed;
        Double writes;

        int id;

        Run(int identification, double c, double e, double w) {
            this.cpu = c;
            this.elapsed = e;
            this.writes = w;
            this.id = identification;
        }

        Double getCpu() {
            return cpu;
        }

        Double getElapsed() {
            return elapsed;
        }

        Double getWrite() {
            return writes;
        }
    }

    // Normal distribution test:
    public static void testNormal(int printFlag) {

        System.out.println("Normal Tests");

        Executions a = new Executions();

        // Show:
        if (printFlag > 0) {
            System.out.println("Distributions");
            KMean.showDistribution(a.getCpuHM());
            KMean.showDistribution(a.getWriteHM());
            KMean.showDistribution(a.getElapsedHM());

        }

        ArrayList<ArrayList<Double>> resultET = classification(a.getElapsed(), 0);
        ArrayList<ArrayList<Double>> resultCPU = classification(a.getCpu(), 0);
        ArrayList<ArrayList<Double>> resultWrite = classification(a.getWrite(), 0);

        // print:
        if (printFlag > 0) {
            System.out.println(resultET);
            System.out.println(resultCPU);
            System.out.println(resultWrite);
        }
    }

    // This function classify an array with the best k:
    private static ArrayList<ArrayList<Double>> classification(ArrayList<Double> array, int printFlag) {
        ArrayList<Double> resultSSE = new ArrayList<>();
        // testing all the ks:
        for (int j = 1; j < array.size(); j++) {
            // K, size and arraylist of numbers
            ArrayList<ArrayList<Double>> group = KMean.executeD(j, array);
            resultSSE.add(KMean.ElbowMethodD(group, 0));
        }
        // biggest gap:
        int bestk = (KMean.calculateBestK(resultSSE) + 1);
        if (printFlag > 0) {
            System.out.println("Best number of groups: " + bestk);
        }

        ArrayList<ArrayList<Double>> group = KMean.executeD(bestk, array);

        // return the group:
        return group;
    }

    // HashMap add:
    private static void hashAdd(LinkedHashMap<Integer, ArrayList<Double>> hm, Integer key, Double aux) {

        // contains:
        if (hm.containsKey(key)) {

            ArrayList<Double> array = hm.get(key);
            if (array != null) {
                array.add(aux);
                hm.put(key, array);
            }
        } else {
            ArrayList<Double> array = new ArrayList<>();
            array.add(aux);
            hm.put(key, array);
        }
    }
}
