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

        // Constructors:
        Executions() {
            id = 1;

            fulfilDistributionET();

            fulfilDistributionCPU();

            fulfilDistributionWrite();

            runs = createsRuns();
        }

        Executions(int i) {
            id = 1;

            fulfilNotRandom(distributionET, hmET, 3);

            fulfilDistributionCPU();

            fulfilDistributionWrite();

            runs = createsRuns();
        }

        private void fulfilNotRandom(ArrayList<Double> distribution, LinkedHashMap<Integer, ArrayList<Double>> hm, int ngroups) {

            //create 2 groups distribution:
            if (ngroups == 2) {
                double[] aux = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 60, 70, 80, 90, 100, 110, 120, 130, 140, 150 };
                int[] type = { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2 };
                // first Normal:
                for (int i = 0; i < this.tam * 2; i++) {
                    distribution.add(aux[i]);
                    hashAdd(hm, type[i], aux[i]);
                    // hm.put(1, aux);
                }
            }
            else{//create 3 groups distribution:
                double[] aux = { 1, 2, 3, 4, 5, 6, 7, 600, 700, 800, 900,1000,1100, 1200,1300, 1200000, 1300000, 1400000, 1500000, 1600000};
                int[] type = { 1, 1, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 3, 3, 3, 3, 3, 3 };
                // first Normal:
                for (int i = 0; i < this.tam * 2; i++) {
                    distribution.add(aux[i]);
                    hashAdd(hm, type[i], aux[i]);
                    // hm.put(1, aux);
                }
            }
        }

        private void fulfilDistributionET() {
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
        }

        private void fulfilDistributionCPU() {
            // first Normal:
            int stdDev = 15;
            int mean = 10;
            fulfil(distributionCPU, hmCPU, stdDev, mean, 1);

            // second Normal:
            stdDev = 15;
            mean = 70;
            fulfil(distributionCPU, hmCPU, stdDev, mean, 2);

        }

        private void fulfilDistributionWrite() {
            // first Normal:
            int stdDev = 15;
            int mean = 10;
            fulfil(distributionWrite, hmWrite, stdDev, mean, 1);

            // second Normal:
            stdDev = 15;
            mean = 70;
            fulfil(distributionWrite, hmWrite, stdDev, mean, 2);

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

            ArrayList<Integer> arrayLocation1 = new ArrayList<>();
            ArrayList<Integer> arrayLocation2 = new ArrayList<>();
            ArrayList<Integer> arrayLocation3 = new ArrayList<>();

            // put
            for (int i = 0; i < tam * 2; i++) {
                arrayLocation1.add(i);
                arrayLocation2.add(i);
                arrayLocation3.add(i);
            }

            // shuffle
            Collections.shuffle(arrayLocation1);
            Collections.shuffle(arrayLocation2);
            Collections.shuffle(arrayLocation3);

            for (int i = 0; i < tam * 2; i++) {
                tempR = new Run(i, distributionCPU.get(arrayLocation1.get(i)), distributionET.get(arrayLocation2.get(i)), distributionWrite.get(arrayLocation3.get(i)));
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

        // This function returns the runs:
        ArrayList<Run> getRuns() {
            return runs;
        }

        // Function used to shuffle the runs information:
        void shuffle() {
            if (distributionRandom != null) {
                Collections.sort(distributionRandom);
            }

            Collections.shuffle(distributionET);
            Collections.shuffle(distributionCPU);
            Collections.shuffle(distributionWrite);
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

        // This function run through the runs and connect the distributions (cpu
        // time and elapsed time):
        public void connect1() {

            distributionET = new ArrayList<>();
            // run:
            for (int i = 0; i < runs.size(); i++) {
                Run eachRun = runs.get(i);
                Double cpu = eachRun.getCpu();
                eachRun.setElapsed(cpu * 2);
                distributionET.add(cpu * 2);

                // hashAdd(hm, type, aux);

            }

        }

        // This function run through the runs and connect the distributions
        // (write
        // time and elapsed time):
        public void connect2() {

            distributionET = new ArrayList<>();
            // run:
            for (int i = 0; i < runs.size(); i++) {
                Run eachRun = runs.get(i);
                Double write = eachRun.getWrite();
                eachRun.setElapsed(write * 2);
                distributionET.add(write * 2);

                // hashAdd(hm, type, aux);

            }

        }
    }

    // Each execution:
    static class Run {
        Double cpu;
        Double elapsed;
        Double writes;

        int id;

        int GCpu;
        int GElapsed;
        int GWrite;

        Run(int identification, double c, double e, double w) {
            this.cpu = c;
            this.elapsed = e;
            this.writes = w;
            this.id = identification;
        }

        // getters:
        Double getCpu() {
            return cpu;
        }

        Double getElapsed() {
            return elapsed;
        }

        Double getWrite() {
            return writes;
        }

        int getId() {
            return id;
        }

        // setters:
        int getGroupCpu() {
            return GCpu;
        }

        int getGroupElapsed() {
            return GElapsed;
        }

        int getGroupWrite() {
            return GWrite;
        }

        // setters:
        void setGroupCpu(int a) {
            GCpu = a;
        }

        void setGroupElapsed(int a) {
            GElapsed = a;
        }

        void setGroupWrite(int a) {
            GWrite = a;
        }

        void setCpu(Double c) {
            this.cpu = c;
        }

        void setWrite(Double w) {
            this.writes = w;
        }

        void setElapsed(Double e) {
            this.elapsed = e;
        }
    }
    // This function correlates the groups:
    private static void createHistograms(ArrayList<Run> array, int gCPU, int gWrite) {
        System.out.println("Create histograms");
        System.out.println("Run id \t\t elapsed cpu write");

        int[] histogramGroupsCPU = new int[gCPU + 1];
        int[] histogramGroupsWrite = new int[gWrite + 1];

        for (int i = 0; i < array.size(); i++) {
            Run eachRun = array.get(i);
            System.out.println("Run id \t" + eachRun.getId() + " \t " + eachRun.getGroupElapsed() + "  \t " + eachRun.getGroupCpu() + " \t " + eachRun.getGroupWrite());
            int groupC = eachRun.getGroupCpu();
            int groupW = eachRun.getGroupWrite();

            // Do a histogram according to the group:
            histogramGroupsCPU[groupC] += 1;
            histogramGroupsWrite[groupW] += 1;

        }

        System.out.println("Cpu");
        for (int i = 1; i < histogramGroupsCPU.length; i++) {
            double perc = 0.0;
            if (histogramGroupsCPU[i] > 0) {
                perc = ((double) histogramGroupsCPU[i] * 100 / array.size());
            }
            System.out.println(perc + "% is in group " + i);
        }
        System.out.println("Write");
        for (int i = 1; i < histogramGroupsWrite.length; i++) {
            double perc = 0.0;
            if (histogramGroupsWrite[i] > 0) {
                perc = ((double) histogramGroupsWrite[i] * 100 / array.size());
            }
            System.out.println(perc + "% is in group " + i);
        }

    }

    // This function classify an array with the best k:
    private static ArrayList<ArrayList<Double>> classification(ArrayList<Double> array, int printFlag) {
        ArrayList<Double> resultSSE = new ArrayList<>();
        // learn the best k - problem when j starts with 1:
        for (int j = 1; j < array.size(); j++) {
            // K, size and arraylist of numbers
            ArrayList<ArrayList<Double>> group = KMean.executeD(j, array);
            resultSSE.add(KMean.ElbowMethodD(group, 0));
        }
        // heuristic validation in learning:
        int bestk = (KMean.calculateBestK(resultSSE) + 1);
        if (printFlag > 0) {
            System.out.println("Best number of groups: " + bestk);
        }
        // applies the best k
        ArrayList<ArrayList<Double>> group = KMean.executeD(bestk, array);

        // return the group:
        return group;
    }

    // HashMap add:
    private static int findGroup(ArrayList<ArrayList<Double>> array, double number) {
        ArrayList<Double> temp;
        for (int i = 0; i < array.size(); i++) {
            temp = array.get(i);
            for (int j = 0; j < temp.size(); j++) {
                if (temp.get(j) == number) {
                    return i + 1;
                }
            }
        }
        return -1;
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

    // Normal distribution test:
    public static void testNormal(int printFlag) {

        System.out.println("Normal Tests");

        Executions a = new Executions(1);
        // Shuffle the runs:
        a.shuffle();
        // link of cpu time and elapsed time:
        //a.connect1();

        ArrayList<ArrayList<Run>> merge = new ArrayList<>();
        ArrayList<Run> temp;

        ArrayList<ArrayList<Double>> resultET;
        ArrayList<ArrayList<Double>> resultCPU;
        ArrayList<ArrayList<Double>> resultWrite;

        int ngET;
        int ngCPU;
        int ngW;

        ArrayList<Run> Runs;

        // Show:
        if (printFlag > 0) {
            System.out.println("Distributions");
            KMean.showDistribution(a.getCpuHM());
            KMean.showDistribution(a.getWriteHM());
            KMean.showDistribution(a.getElapsedHM());

        }

        resultET = classification(a.getElapsed(), 0);
        resultCPU = classification(a.getCpu(), 0);
        resultWrite = classification(a.getWrite(), 0);

        // size of each group:
        ngET = resultET.size();
        ngCPU = resultCPU.size();
        ngW = resultWrite.size();

        // print:
        if (printFlag > 0) {
            System.out.println(resultET);
            System.out.println(resultCPU);
            System.out.println(resultWrite);
        }

        Runs = a.getRuns();

        // Set the groups for the runs:
        for (int i = 0; i < Runs.size(); i++) {
            Run eachRun = Runs.get(i);

            int groupET = findGroup(resultET, eachRun.getElapsed());
            int groupCPU = findGroup(resultCPU, eachRun.getCpu());
            int groupWrite = findGroup(resultWrite, eachRun.getWrite());

            eachRun.setGroupElapsed(groupET);
            eachRun.setGroupCpu(groupCPU);
            eachRun.setGroupWrite(groupWrite);
        }

        // Histogram:
        for (int j = 0; j <= ngET; j++) {
            temp = new ArrayList<>();
            for (int i = 0; i < Runs.size(); i++) {
                Run eachRun = Runs.get(i);
                // fast:
                if (eachRun.getGroupElapsed() == j) {
                    temp.add(eachRun);
                }
            }
            merge.add(temp);
        }

        // Print:
        if (printFlag > 0) {
            // System.out.println("Run ids \t elapsed cpu write \t");
            for (int i = 0; i < Runs.size(); i++) {
                Run eachRun = Runs.get(i);
                System.out.println("Run id \t" + eachRun.getId() + " \t " +
                        eachRun.getGroupElapsed() + " \t " + eachRun.getGroupCpu() +
                        " \t " + eachRun.getGroupWrite());
            }
        }

        // See the relationship in each group:
        for (int j = 0; j <= ngET; j++) {
            createHistograms(merge.get(j), ngCPU, ngW);
        }

    }

}
