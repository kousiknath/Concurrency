package com.concurrency.ratelimiter;

import java.util.Random;
import java.util.concurrent.Semaphore;

public class APIRateLimiter {
    /*
        Design a simple rate limiter (No complex algorithm required) which
        should simply rate limit requests based on the number of threads
        accessing the API at a given point in time.
     */

    interface ExternalAPIClient {
        void call();
    }

    static class APICaller implements Runnable {
        /*
            `Semaphore` is used when multiple threads upto a specific number should
            be allowed to access a common set of resources. `Semaphore` is used as
            a resource guard or counter. There are some good use cases for `Semaphore` like:
                1. Controlling the number of threads accessing resource pools like
                database connection pool.
                2. Rate limiting api calls.
                3. Controlling the maximum number of tasks or jobs that a system can handle.

            `Semaphore` should not be used in a critical section which directly performs
            data manipulation like increasing/decreasing counter, modifying data structure,
            etc. If multiple threads access those critical section of code through Semaphore,
            they will corrupt the data. So, for such kinds of use cases, other locks are better
            suited. See other examples.

            A semaphore does not keep track of which thread has acquired it. So, there is
            no ownership model. Any thread can acquire and any thread can release.

            A semaphore with permit value 1 is called Mutex. In our example, we use
            Counting Semaphore, which is initialized to permit more than 1.

            If there is no permit available, calling threads wait on the semaphore.
            Once a permit is available, semaphore singals waiting threads for its
            availability, and accordingly, other threads either proceed further or
            continue waiting.

            Semaphore is Non-Reentrant in nature. So if the same thread wants to
            access two nested methods which are protected by the same semaphore,
            the semaphore will be acquired twice.
         */
        private Semaphore semaphore;
        private ExternalAPIClient externalAPIClient;

        public APICaller(Semaphore semaphore, ExternalAPIClient externalAPIClient) {
            this.semaphore = semaphore;
            this.externalAPIClient = externalAPIClient;
        }

        @Override
        public void run() {
            try {
                this.semaphore.acquire();
                System.out.println("Semaphore acquired by thread = "
                        + Thread.currentThread().getName()
                        + ". Available permits = " + this.semaphore.availablePermits());
                this.externalAPIClient.call();

                Thread.sleep(5000);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println(Thread.currentThread().getName() + " releasing semaphore");
                this.semaphore.release();
            }
        }
    }

    private Semaphore semaphore;
    private ExternalAPIClient externalAPIClient;

    public APIRateLimiter(int maxLimit) {
        this.semaphore = new Semaphore(maxLimit);
        this.externalAPIClient = new ExternalAPIClient() {
            @Override
            public void call() {

            }
        };
    }

    public void callAPI(int threads) {
        for (int i = 0; i < threads; i++) {
            Thread thread = new Thread(new APICaller(this.semaphore, this.externalAPIClient));
            thread.start();
            sleep(new Random().nextInt(500));
        }
    }

    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static void test1() {
        APIRateLimiter rateLimiter = new APIRateLimiter(10);
        rateLimiter.callAPI(100);
    }

    public static void main(String[] args) {
        test1();
    }
}
