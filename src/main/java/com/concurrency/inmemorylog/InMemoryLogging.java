package com.concurrency.inmemorylog;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

class Writer implements Runnable {
    private InMemoryLogging logging;

    public Writer(InMemoryLogging logging) {
        this.logging = logging;
    }

    @Override
    public void run() {
        while (true) {
            String message = "random message " + new Random().nextInt(10000);
            this.logging.writeLog(message);
            System.out.println("Produced message = " + message);

            try {
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

class Reader implements Runnable {
    private InMemoryLogging logging;
    private int logIndex = -1;

    public Reader(InMemoryLogging logging) {
        this.logging = logging;
    }

    @Override
    public void run() {
        while (true) {
            int nextIndex = this.logIndex + 1;
            String message = this.logging.readLogLine(nextIndex);
            if (!message.isEmpty() && !message.isBlank()) {
                this.logIndex = nextIndex;
                System.out.println(Thread.currentThread().getName()
                        + " retrieved log message = " + message + " @ index = " + this.logIndex);
            }

            try {
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

public class InMemoryLogging {
    private List<String> log;

    /*
        `ReadWriteLock` is used when you need more granular control on read
        and write paths.

        The writeLock.lock() will block any read and write operation.
        `writeLock` can be acquired when there is no writer thread already
        holding the writeLock and there is no reader thread already holding
        the `readLock`.

        The readLock.lock() will allow multiple readers to go through the
        critical section when no writer is yet writing, and no writer is
        concurrently requesting the writeLock.

        So, readLock is shared among multiple threads, whereas writeLock is
        exclusive.

        If a thread attempts to update the data while other threads are reading,
        the writer thread also blocks until the read lock is released.

        `ReentrantReadWriteLock` allows a thread to acquire the read lock or
        write lock multiple times just like `ReentrantLock`.
     */

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    public InMemoryLogging() {
        this.log = new ArrayList<>();
    }

    public void writeLog(String message) {
        this.lock.writeLock().lock();

        try {
            this.log.add(message);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public String readLogLine(int i) {
        try {
            /*
                tryLock() acquires the lock if it is free within the given waiting
                time and the current thread has not been interrupted.
                If the lock is available, this method returns immediately with the
                value true. If the lock is not available, then the current thread
                becomes disabled for thread scheduling purposes and lies dormant
                until one of three things happens:
                    The lock is acquired by the current thread; or
                    Some other thread interrupts the current thread, and interruption
                    of lock acquisition is supported; or
                    The specified waiting time elapses
             */
            if (this.lock.readLock().tryLock(100, TimeUnit.MILLISECONDS)) {
                if (i < this.log.size()) {
                    return this.log.get(i);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            this.lock.readLock().unlock();
        }

        return "";
    }

    private static void test1() {
        InMemoryLogging logging = new InMemoryLogging();

        Thread writerThread = new Thread(new Writer(logging));
        Thread readerThread1 = new Thread(new Reader(logging));
        Thread readerThread2 = new Thread(new Reader(logging));

        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            executorService.submit(writerThread);
            executorService.submit(readerThread1);
            executorService.submit(readerThread2);
        }
    }

    public static void main(String[] args) {
        test1();
    }
}
