package com.concurrency.wordscounter;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WordsCounter {
    /*
        CountDownLatch is used when one thread's execution depends
        on completion of other n-threads.

        Example: let's say there are n (n > 0) threads, each of which
        is given a chunk of a big file. The threads are going to count
        distinct words of that chunk (or any CPU intensive operation).
        Now, the main thread is tasked to give the final count of distinct
        words of the whole big file. The main thread spawns n-different
        threads which can compute word count individually. Once they are
        complete, the main thread computes the aggregation.

        Such kind of use cases is supported by CountDownLatch.

        A thread can await() on some count down latch, wait for the count
        of the latch to go to 0. Once it's 0, the thread enters the critical
        section, does its own task and signals back to the caller that
        it's done by counting down another appropriate latch.

        Remember, a thread only can either initialize the latch to some
        value or it can only count downwards. It cannot restore the
        latch's value.
     */
    private CountDownLatch startSignal;
    private CountDownLatch doneSignal;

    public WordsCounter(int numThreads) {
        this.startSignal = new CountDownLatch(1);
        this.doneSignal = new CountDownLatch(numThreads);
    }

    public void giveStartSignal() {
        System.out.println("Start signa given ...");
        this.startSignal.countDown();
    }

    public void awaitCompletion () {
        try {
            this.doneSignal.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    class ChunkWorker implements Runnable {
        private CountDownLatch startSignal;
        private CountDownLatch doneSignal;
        public ChunkWorker(CountDownLatch startSignal, CountDownLatch doneSignal) {
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;
        }

        @Override
        public void run() {
            try {
                this.startSignal.await(); // Wait for the value of startSignal go to 0
                System.out.println(Thread.currentThread().getName() + " computing distinct count ...");
                Thread.sleep(new Random().nextInt(2000));

                this.doneSignal.countDown();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }

        }
    }

    public void runMyLatch(int workerCount) {
        try (ExecutorService executorService = Executors.newFixedThreadPool(workerCount * 2)) {
            for (int i = 0; i < workerCount; i++) {
                Thread workerThread = new Thread(new ChunkWorker(
                        this.startSignal,
                        this.doneSignal));
                executorService.submit(workerThread);
            }

            this.giveStartSignal();
            this.awaitCompletion();
            System.out.println("Done with all the workers :) Bye.");
        }
    }

    private static void test1() {
        int workerCount = 5;
        WordsCounter wordsCounter = new WordsCounter(workerCount);
        wordsCounter.runMyLatch(workerCount);
    }

    public static void main(String[] args) {
        test1();
    }
}
