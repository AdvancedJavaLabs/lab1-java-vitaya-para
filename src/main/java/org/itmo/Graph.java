package org.itmo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

class Graph {
    private final int V;
    private int E = 0;
    private final List<List<Integer>> adjList;
    private final AtomicInteger counter = new AtomicInteger(0);


    Graph(int vertices) {
        this.V = vertices;
        this.adjList = new ArrayList<>(vertices);
        for (int i = 0; i < vertices; ++i) {
            adjList.add(new ArrayList<>());
        }
    }

    void addEdge(int src, int dest) {
        if (!adjList.get(src).contains(dest)) {
            adjList.get(src).add(dest);
            E++;
        }
    }

    void bfs(int startVertex) {
        boolean[] visited = new boolean[V];
        LinkedList<Integer> queue = new LinkedList<>();

        visited[startVertex] = true;
        counter.incrementAndGet();
        queue.add(startVertex);

        while (!queue.isEmpty()) {
            int v = queue.poll();

            for (int n : adjList.get(v)) {
                if (!visited[n]) {
                    visited[n] = true;
                    counter.incrementAndGet();
                    queue.add(n);
                }
            }
        }
    }

    void parallelBFS(int startVertex) {
        int threadsAvailable = Math.min(Runtime.getRuntime().availableProcessors(), 4);
        if (V < 1000 || E < V * 5) {
            bfs(startVertex);
            return;
        }

        parallelBFS(startVertex, threadsAvailable);
    }

    void parallelBFS(int startVertex, int numberOfThreads) {
        AtomicIntegerArray visited = new AtomicIntegerArray(V);

        if (!visited.compareAndSet(startVertex, 0, 1)) {
            return;
        }
        counter.incrementAndGet();

        ConcurrentLinkedQueue<Integer> currentLevel = new ConcurrentLinkedQueue<>();
        currentLevel.add(startVertex);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        try {
            while (!currentLevel.isEmpty()) {

                List<List<Integer>> localQueues = new ArrayList<>(numberOfThreads);
                for (int i = 0; i < numberOfThreads; ++i) {
                    localQueues.add(new LinkedList<>());
                }

                CountDownLatch latch = new CountDownLatch(numberOfThreads);
                for (int t = 0; t < numberOfThreads; ++t) {
                    final int idx = t;
                    ConcurrentLinkedQueue<Integer> finalCurrentLevel = currentLevel;
                    executor.execute(() -> {
                        try {
                            Integer v;
                            while ((v = finalCurrentLevel.poll()) != null) {
                                processVertex(v, visited, localQueues.get(idx));
                            }
                        } finally {
                            latch.countDown();
                        }
                    });
                }

                latch.await();
                ConcurrentLinkedQueue<Integer> nextLevel = new ConcurrentLinkedQueue<>();
                for (List<Integer> lq : localQueues) {
                    nextLevel.addAll(lq);
                }
                currentLevel = nextLevel;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processVertex(
        int v,
       AtomicIntegerArray visited,
       List<Integer> localQueue
    ) {
        for (int n : adjList.get(v)) {
            if (visited.compareAndSet(n, 0, 1)) {
                counter.incrementAndGet();
                localQueue.add(n);
            }
        }
    }

    int getVisitedCounterValue() {
        return counter.get();
    }

    int getNumberOfVertices() {
        return V;
    }
}
