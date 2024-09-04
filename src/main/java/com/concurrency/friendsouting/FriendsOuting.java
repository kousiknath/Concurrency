package com.concurrency.friendsouting;

import java.util.Random;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FriendsOuting {
    /*
        Cyclic Barrier is used when multiple threads wait at a point
        for each other. Once all of them arrive at the barrier, the
        given runnable (if provided) executes and all the threads are
        released from the barrier.

        This barrier is called `Cyclic` because it can be reused once
        all the waiting threads are released.

        So, if multiple threads need to synchronize at a point before
        they start doing their own job, CyclicBarrier can be used.

        Essentially, Cyclic Barriers are check points. Multiple threads
        can meet at predefined checkpoints to check their progress or
        to validate some constraints, and then they continue with their
        task. Later they can meet at another checkpoint and continue
        and so on.

        A real world use case of Count Down Latch and Cyclic Barrier
        From: https://stackoverflow.com/questions/10156191/real-life-examples-for-countdownlatch-and-cyclicbarrier

        CountDownLatch A Multithreaded download manager. The download
        manager will start multiple threads to download each part of the
        file simultaneously.(Provided the server supports multiple threads
        to download). Here each thread will call a countdown method of an
        instantiated latch. After all the threads have finished execution,
        the thread associated with the countdown latch will integrate the
        parts found in the different pieces together into one file

        CyclicBarrier Same scenario as above.But assume the files are
        downloaded from P2P. Again, multiple threads are downloading the pieces.
        But here, suppose that you want the integrity check for the downloaded
        pieces to be done after a particular time interval. Here a cyclic barrier
        plays an important role. After each time interval, each thread will
        wait at the barrier so that thread associated with a cyclic barrier can
        do the integrity check. This integrity check can be done multiple
        times thanks to CyclicBarrier

        "To compare both, the CountDownLatch is supposed to be used only 1 time and
        a CyclicBarrier can be used as many times as the algorithm requires a
        synchronization point for a set of threads."

        From: https://stackoverflow.com/questions/10156191/real-life-examples-for-countdownlatch-and-cyclicbarrier

        Use case 1 Suppose you have split a large job into 10 small tasks, each one a
        thread. You have to wait for the 10 tasks' end from that threads before
        considering the job done.

        So the main job initiator thread initializes a CountDownLatch to the number
        of threads used, it distributes tasks to threads and waits for the latch
        raises zero with await method. Each executor thread will invoke countDown
        at the end of its task. Finally the main thread will be waken when all threads
        have finished so it considers the whole job is done. This scenario uses the
        doneSignal latch describes in the CountDownLatch javadoc.

        Use case 2 Suppose you have split a large job into a n * m tasks, distributed
        over n threads. m corresponds to a matrix row and you have a total to compute
        for each row. In that case, threads must be synchronized after each task ending
        so that the total for the row is processed. In that case, a CyclicBarrier
        initialized with the number of threads n is used to wait for the end of each
        row computation (m times in fact).

        To compare both, the CountDownLatch is supposed to be used only 1 time and a
        CyclicBarrier can be used as many times as the algorithm requires a
        synchronization point for a set of threads
     */
    private CyclicBarrier meetingPoint;
    public FriendsOuting(int friends, Runnable runnable) {
        this.meetingPoint = new CyclicBarrier(friends, runnable);
    }

    class Friend implements Runnable {
        private CyclicBarrier meetingPoint;
        public Friend(CyclicBarrier meetingPoint) {
            this.meetingPoint = meetingPoint;
        }

        @Override
        public void run() {
            try {
                Thread.sleep(1000 + new Random().nextInt(5000));

                meetingPoint.await();
                System.out.println(Thread.currentThread().getName() + " met at the meeting point");

            } catch (InterruptedException | BrokenBarrierException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private void callFriendsForMeeting(int numOfFriends) {
        try (ExecutorService executorService = Executors.newFixedThreadPool(10)) {
            for (int i = 0; i < numOfFriends; i++) {
                Thread thread = new Thread(new Friend(this.meetingPoint));
                executorService.submit(thread);
            }
        }
    }

    private static void test1() {
        int numOfFriends = 5;
        FriendsOuting obj = new FriendsOuting(numOfFriends,
                () -> System.out.println("Everyone is here. Let's go party! Yay!"));
        obj.callFriendsForMeeting(numOfFriends);
    }

    public static void main(String[] args) {
        test1();
    }
}
