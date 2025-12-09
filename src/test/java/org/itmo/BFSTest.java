package org.itmo;

import org.junit.jupiter.api.Test;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.Buffer;
import java.util.HashSet;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

public class BFSTest {

    @Test
    public void bfsTest() throws IOException {
        int[] sizes = new int[]{2_000_000, 1_000_000, 100_000, 100_000, 50_000, 50_000, 10_000, 10_000, 10_000, 1_000, 1_000, 1_000, 1_000, 100, 100, 10, 10, 10};
        int[] connections = new int[]{10_000_000, 10_000_000, 5_000_000, 2_000_000, 5_000_000, 1_000_000, 5_000_000, 2_000_000, 1_000_000, 900_000, 500_000, 100_000, 50_000, 9_900, 5_000, 90, 50, 10};

        Random r = new Random(42);
        int approximateConst = 5;

        try (FileWriter fw = new FileWriter("tmp/results.txt")) {
            for (int i = 0; i < sizes.length; i++) {
                System.out.println("--------------------------");
                System.out.println("Generating graph of size " + sizes[i] + " ...wait");

                long serialTime = 0;
                long parallelTime = 0;

                for (int j = 0; j < approximateConst; j++) {
                    Graph g = new RandomGraphGenerator().generateGraph(r, sizes[i], connections[i]);
                    System.out.println("Generation " + j + " completed\nStarting bfs");

                    serialTime += executeSerialBfsAndGetTime(g);
                    parallelTime += executeParallelBfsAndGetTime(g);

                    Runtime.getRuntime().gc();
                }

                serialTime /= approximateConst;
                parallelTime /= approximateConst;

                fw.append("Times for " + sizes[i] + " vertices and " + connections[i] + " connections: ");
                fw.append("\nSerial: " + serialTime);
                fw.append("\nParallel: " + parallelTime);
                fw.append("\n--------\n");
            }
            fw.flush();
        }
    }


    private long executeSerialBfsAndGetTime(Graph g) {
        long startTime = System.currentTimeMillis();
        g.bfs(0);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

    private long executeParallelBfsAndGetTime(Graph g) {
        long startTime = System.currentTimeMillis();
        g.parallelBFS(0);
        long endTime = System.currentTimeMillis();
        return endTime - startTime;
    }

}
