package com.concurrency.producerconsumer;


import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class SharedQueuePrimitive implements SharedQueue {
    private int capacity;
    private int size;
    private final LinkedList<String> que;

    public SharedQueuePrimitive(int capacity) {
        this.capacity = capacity;
        this.que = new LinkedList<>();
    }

    @Override
    public synchronized void produce(String message) {
        /*
            `synchronized` must be added to the method; otherwise,
            application won't even run properly.

            `wait()` must be called under `synchronized` context.
            Refer to the explanation provided in `ProducerConsumerApplication`
         */
        while (this.size == this.capacity) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        this.que.addLast(message);
        this.size++;
        this.notifyAll();
    }

    @Override
    public synchronized String consume() {
        while (this.size == 0) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        String result = this.que.pollFirst();
        this.size--;

        this.notifyAll();
        return result;
    }
}

public class ProducerConsumerApplication_2 {

    private static void test1() {
        int maxCapacity = 10;
        SharedQueue sharedQueue = new SharedQueuePrimitive(maxCapacity);

        Thread producerThread = new Thread(new Producer(sharedQueue));
        Thread consumerThread1 = new Thread(new Consumer(sharedQueue));
        Thread consumerThread2 = new Thread(new Consumer(sharedQueue));
        Thread consumerThread3 = new Thread(new Consumer(sharedQueue));

        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            executorService.submit(producerThread);
            executorService.submit(consumerThread1);
            executorService.submit(consumerThread2);
            executorService.submit(consumerThread3);

            executorService.shutdown();
        }
    }

    public static void main(String[] args) {
        test1();
    }
}