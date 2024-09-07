package com.concurrency.producerconsumer;


import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.*;

interface SharedQueue {
    void produce(String message);
    String consume();
}

class SharedQueueImpl implements SharedQueue {
    private final int capacity;
    private int size;
    private final LinkedList<String> que;
    private final Lock lock;
    private final Condition producerWaiting;
    private final Condition consumerWaiting;

    public SharedQueueImpl(int capacity) {
        this.capacity = capacity;
        this.que = new LinkedList<>();
        this.lock = new ReentrantLock();
        this.producerWaiting = this.lock.newCondition();
        this.consumerWaiting = this.lock.newCondition();
    }

    @Override
    public void produce(String message) {
        this.lock.lock();

        try {
            while (this.size == this.capacity) {
                try {
                    this.producerWaiting.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            this.que.addLast(message);
            this.size++;

        } finally {
            this.consumerWaiting.signalAll();
            this.lock.unlock();
        }
    }

    @Override
    public String consume() {
        this.lock.lock();

        String result = "";
        try {
            while (this.size == 0) {
                try {
                    /*
                        `lock.lock()` must be called before any thread calls `condition.await()`.
                        Why so?
                        Let's say that we have a consumer thread - `Thread C` which is waiting for
                        an item to be added to the shared queue.
                        Let's assume there is also one more thread - `Thread P`, which is preparing
                        to add some items to the shared queue.
                        Now, since the current size of the queue is 0, `Thread C` find the `while()`
                        loop condition `this.size == 0` to be true. So, naturally, in the next statement,
                        it's supposed to call `condition.await()`. But, before it calls the
                        `condition.await()` method, let's say, the producer thread adds an item
                        to the queue.

                        At this point in time, the consumer `Thread C` is unaware of this event. It
                        goes ahead and calls `condition.await()` and keeps on waiting for an item
                        to be added to the queue. So, the consumer thread is waiting for an event
                        (adding an item to the queue) that has already happened.

                        To prevent such conflicts, the consumer thread (or any incoming thread)
                        must acquire the lock before calling `condition.await()`. `condition.await()`
                        has internal atomic mechanism, which ensures that when this method is called:
                        - the incoming thread releases the lock and
                        - it starts waiting on the condition object's queue.
                        Both of the above operations happen atomically. Hence, once this atomic
                        operation is done, lock is available for the other thread like `Thread P`,
                        which can now add items to the queue and signal back the waiting
                        threads for consumption on the condition object.

                        Condition objects are specifically designed to resolve such conflicts and
                        make synchronization among threads transparent.

                        More info: https://stackoverflow.com/questions/32774399/why-lock-condition-await-must-hold-the-lock

                 */
                    this.consumerWaiting.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

                result = this.que.pollFirst();
                this.size--;

            } finally {
                this.producerWaiting.signalAll();
                this.lock.unlock();
            }

        return result;
    }
}

class Producer implements Runnable {
    private final SharedQueue sharedQueue;

    public Producer(SharedQueue sharedQueue) {
        this.sharedQueue = sharedQueue;
    }

    @Override
    public void run() {
        while (true) {
            String someRandomMessage = "Random Message = " + new Random().nextInt(1000000);
            System.out.println("Producing message = " + someRandomMessage);

            this.sharedQueue.produce(someRandomMessage);

            try {
                Thread.sleep(new Random().nextInt(100));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class Consumer implements Runnable {
    private final SharedQueue sharedQueue;

    public Consumer(SharedQueue sharedQueue) {
        this.sharedQueue = sharedQueue;
    }

    @Override
    public void run() {
        while (true) {
            String consumedMessage = this.sharedQueue.consume();
            System.out.println(Thread.currentThread().getName() + " consumed message = " + consumedMessage);

            try {
                Thread.sleep(new Random().nextInt(500));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

public class ProducerConsumerApplication {

    private static void test1() {
        int maxCapacity = 10;
        SharedQueue sharedQueue = new SharedQueueImpl(maxCapacity);

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
