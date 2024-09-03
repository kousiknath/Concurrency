package com.concurrency.oddevenprinter;

public class MultiThreadedPrinter {
    static class SharedState {
        /*
            `volatile` makes sure to propagate the variable's value to CPU
            registers; hence, changes made by one thread are immediately
            available to other threads.

            So, `volatile` guarantees both mutual exclusion and visibility.
         */
        private volatile boolean state;

        public boolean isOn() {
            return state;
        }

        public void toggle() {
            this.state = !this.state;
        }
    }

    static class Printer implements Runnable {
        private int counter;
        private final SharedState state;
        private final int max;
        public Printer(int counter, SharedState state, int max) {
            this.counter = counter;
            this.state = state;
            this.max = max;
        }

        public void run() {
            do {
                // Logic for even printer
                if (counter % 2 == 0 && !state.isOn()) {
                    System.out.print(counter + " ");
                    counter += 2;
                    state.toggle();
                }
                // Odd counter
                else if (counter % 2 != 0 && state.isOn()) {
                    System.out.print(counter + " ");
                    counter += 2;
                    state.toggle();
                }

                sleep(1000);

            } while (this.counter <= this.max);
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void printOddEven() {
        int max = 100;
        SharedState state = new SharedState();

        Thread evenThread = new Thread(new Printer(0, state, max));
        Thread oddThread  = new Thread(new Printer(1, state, max));
        evenThread.start();
        oddThread.start();
    }

    private static void test1() {
        MultiThreadedPrinter obj = new MultiThreadedPrinter();
        obj.printOddEven();
    }

    public static void main(String[] args) {
        test1();
    }
}
