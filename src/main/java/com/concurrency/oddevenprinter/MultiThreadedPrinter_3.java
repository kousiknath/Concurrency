package com.concurrency.oddevenprinter;

public class MultiThreadedPrinter_3 {
    static class Counter {
        private int count;
        private boolean state;
        public synchronized int even() {
         // state should be false
             while (state) {
                 waitForMonitor();
             }

             int data = count;
             count++;
             state = !state;
             notifyAll();
             sleep();

             return data;
        }

        public synchronized int odd() {
            // state should be true
            while (!state) {
                waitForMonitor();
            }

            int data = count;
            count++;
            state = !state;
            notifyAll();
            sleep();

            return data;
        }

        private void waitForMonitor() {
            try{
                wait();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }

        private void sleep() {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    static class EvenPrinter implements Runnable {
        private Counter counter;
        private int max;
        public EvenPrinter(Counter counter, int max) {
            this.counter = counter;
            this.max = max;
        }

        @Override
        public void run() {
            while (true) {
                int even = this.counter.even();
                if (even > this.max) {
                    break;
                }

                System.out.print(even + " ");
            }
        }
    }

    static class OddPrinter implements Runnable {
        private Counter counter;
        private int max;

        public OddPrinter(Counter counter, int max) {
            this.counter = counter;
            this.max = max;
        }

        @Override
        public void run() {
            while (true) {
                int odd = this.counter.odd();
                if (odd > this.max) {
                    break;
                }

                System.out.print(odd + " ");
            }
        }
    }

    private static void test1() {
        Counter counter = new Counter();
        int max = 100;
        Thread even = new Thread(new EvenPrinter(counter, max));
        Thread odd  = new Thread(new OddPrinter(counter, max));

        even.start();
        odd.start();
    }

    public static void main(String[] args) {
        test1();
    }
}
