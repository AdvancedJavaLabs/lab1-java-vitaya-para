package org.itmo;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

class GraphBenchmarkTest {

    /* --------------------------------------------------------------- */
    private static final int ITERATIONS = 3;
    private static final Random RND = new Random(42);

    private static final int[] VERTICES = {2_000_000, 1_000_000, 100_000, 100_000, 50_000, 50_000, 10_000, 10_000, 10_000, 1_000, 1_000, 1_000};
    private static final int[] EDGES ={10_000_000, 10_000_000, 5_000_000, 2_000_000, 5_000_000, 1_000_000, 5_000_000, 2_000_000, 1_000_000, 900_000, 500_000, 100_000};


    @Test
    void benchmarkFixedThreads() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("tmp/graph_benchmark.csv"))) {
            out.println("vertices,edges,serial_ms,parallel_ms_4,speedup");

            for (int i = 0; i < VERTICES.length; ++i) {
                int v = VERTICES[i];
                int e = EDGES[i];

                long serialSumNs   = 0;
                long parallelSumNs = 0;

                for (int j = 0; j < ITERATIONS; ++j) {
                    Graph g = new RandomGraphGenerator().generateGraph(RND, v, e);

                    serialSumNs   += runSerial(g);
                    parallelSumNs += runParallel(g, 4);      // 4 потока

                    Runtime.getRuntime().gc();
                }

                double serialAvgMs   = (double) serialSumNs / ITERATIONS / 1_000_000.0;
                double parallelAvgMs = (double) parallelSumNs / ITERATIONS / 1_000_000.0;
                double speedup       = serialAvgMs / parallelAvgMs;

                out.printf("%d,%d,%.2f,%.2f,%.2f%n", v, e,
                        serialAvgMs, parallelAvgMs, speedup);
            }
        }
    }

    @Test
    void benchmarkThreadScaling() throws Exception {
        try (PrintWriter out = new PrintWriter(new FileWriter("tmp/thread_scaling.csv"))) {
            out.println("vertices,edges,threads,time_ms,speedup");
            System.out.println("Start");

            for (int i = 0; i < VERTICES.length; ++i) {
                int v = VERTICES[i];
                int e = EDGES[i];
                int maxThreads = Math.max(Runtime.getRuntime().availableProcessors(), 4);
                System.out.println(v + "\t" + e + "\t" + maxThreads );

                double pastAvgMs = 0.0;
                for (int t = 1; t <= maxThreads; ++t) {
                    System.out.println(t + "\t" + i );
                    long sumNs = 0;
                    for (int j = 0; j < ITERATIONS; ++j) {
                        Graph g = new RandomGraphGenerator().generateGraph(RND, v, e);
                        sumNs += runParallel(g, t);
                        Runtime.getRuntime().gc();
                    }

                    double avgMs   = (double) sumNs / ITERATIONS / 1_000_000.0;


                    double speedup = pastAvgMs != 0.0 ? pastAvgMs / avgMs : 1.0 ;
                    pastAvgMs = avgMs;

                    out.printf("%d,%d,%d,%.2f,%.2f%n", v, e, t,
                            avgMs, speedup);
                }
            }
        }
    }

    private static long runSerial(Graph g) {
        long start = System.nanoTime();
        g.bfs(0);
        return System.nanoTime() - start;
    }

    private static long runParallel(Graph g, int threads) {
        long start = System.nanoTime();
        g.parallelBFS(0, threads);
        return System.nanoTime() - start;
    }
}
