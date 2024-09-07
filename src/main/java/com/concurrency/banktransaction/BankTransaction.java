package com.concurrency.banktransaction;

import java.util.Random;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class BankAccount {
    private String accountNo;
    private String name;

    public BankAccount(String no) {
        this.accountNo = no;
    }
}

interface BankTransactionService {
    void moveMoney(BankAccount source, BankAccount destination, long amount);
}

class SomeBankTransactionService implements BankTransactionService {
    private Lock lock;

    public SomeBankTransactionService() {
        /*
            ReentrantLock can be acquired by a thread multiple times
            if the thread already owns the lock.

            Let's say A() call B() and while calling A(), a thread
            acquires a ReentrantLock. If the same lock protects some
            resource in B() as well, the thread is allowed to access
            the resources inside B() since the lock is already held by
            the thread.

            A()
                lock()
                // some logic
                B()
                // some logic
                unlock()
            B()
                lock()
                // some logic
                unlock()

             ReentrantLock keeps track of which thread has acquired it.
             So, in a nested method call, when a thread needs to re-acquire
             the lock, only the lock hold count is increased and the thread
             is given permission as it already holds the lock.

             The unlock() call on the lock decreases the hold count.

             In Java, `synchronized` is very famous, and that's too re-entrant.
             A synchronized block or method can call another synchronized
             method in a nested way given they synchronize on the same object.

             `Semaphore` is Non-Reentrant in nature.

             `ReentrantLock` provides much more granular control like
             lock polling, lock with timeout, lock interruptbly, etc.

             Performance wise, it's debatable whether `synchronized` is faster
             or `ReentrantLock` is faster. But you can choose `ReentrantLock`
             when you need more control on locking semantics.

             `synchronized` internally uses object mutex, whereas `ReentrantLock`
             uses Queue Synchronizer, Compare and Swap (CAS) etc.

             Caution: Don't forget to call unlock() on `ReentrantLock`. This is
             developers' responsibility.
         */
        this.lock = new ReentrantLock();
    }

    @Override
    public void moveMoney(BankAccount source, BankAccount destination, long amount) {
        // Assuming the source account has enough balance
        /*
            Locking Best Practice:
                YOU SHOULD ALWAYS TAKE THE LOCK BEFORE THE `try{}` BLOCK.

                From: https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/locks/Lock.html#unlock--
                    "A Lock implementation will usually impose restrictions on
                    which thread can release a lock (typically only the holder
                    of the lock can release it) and may throw an (unchecked)
                    exception if the restriction is violated. Any restrictions
                    and the exception type must be documented by that Lock
                    implementation."

                When a thread calls `lock.lock()` or `lock.lockInterruptibly()`,
                there could be exceptions (unchecked exceptions) to prevent
                the lock from being acquired.

                If you place `lock.lock()` or `lock.lockInterruptibly()` constructs
                inside the `try{}` block, even if lock acquisition had failed,
                the `finally{}` block will execute trying to unlock the lock
                by executing `lock.unlock()` (assuming you have put `lock.unlock()`
                inside the `finally{}` block), thus throwing error like
                `IllegalMonitorStateException` as the current thread could not
                actually acquire the lock, thus it does not own the monitor.

                So, putting the `lock()` or `lockInterruptibly()` outside the
                `try{}` block prevents such exceptions as you never get to the
                `finally{}` block trying to unlock the lock since the lock
                acquisition itself failed.

            More Info:
                https://issues.apache.org/jira/browse/AMQ-9202
                https://stackoverflow.com/questions/31058681/java-locking-structure-best-pattern

           `lock.lock()` suspends the incoming thread if another thread has already
           acquired the lock. So, the incoming thread becomes completely useless
           till the time it wakes up again.
         */
        this.lock.lock();

        try {
            long newBalanceSource = getBalance(source) - amount;
            long newBalanceDest = getBalance(destination) + amount;

            updateBalance(source, newBalanceSource);
            updateBalance(destination, newBalanceDest);
        } finally {
            this.lock.unlock();
        }
    }

    public long getBalance(BankAccount account) {
        this.lock.lock();

        try {
            // Fetch from db
            return 10000 + new Random().nextInt(10000);
        } finally {
            this.lock.unlock();
        }
    }

    public void updateBalance(BankAccount account, long newBalance) {
        this.lock.lock();

        try {
            // update balance to db
            // some logic

        } finally {
            this.lock.unlock();
        }

    }
}



public class BankTransaction {

    public static void main(String[] args) {

    }
}
