package org.stone.study.queue;

import org.stone.shine.util.concurrent.SynchronousQueue2;

import java.util.concurrent.*;

public class TestSynchronousQueue {
    private static int THREAD_NUM;
    private static int N = 1000000;
    private static ExecutorService executor;

    public static void main(String[] args) throws Exception {
        System.out.println("Producer\tConsumer\tcapacity \t LinkedBlockingQueue \t ArrayBlockingQueue \t SynchronousQueue \t SynchronousQueue2");

        for (int j = 0; j < 10; j++) {
            THREAD_NUM = (int) Math.pow(2, j);
            executor = Executors.newFixedThreadPool(THREAD_NUM * 2);

            for (int i = 0; i < 10; i++) {
                int length = (i == 0) ? 1 : i * 10;
                System.out.print("\t" + THREAD_NUM + "\t\t");
                System.out.print("\t" + THREAD_NUM + "\t\t");
                System.out.print("\t" + length + "\t\t");
//                 System.out.print("\t" + doTest2(new LinkedBlockingQueue<Integer>(length), N) + "/s\t\t\t");
//                 System.out.print("\t" + doTest2(new ArrayBlockingQueue<Integer>(length), N) + "/s\t\t\t");
                System.out.print("\t" + doTest2(new java.util.concurrent.SynchronousQueue<Integer>(true), N) + "/s\t\t\t");
                System.out.print("\t" + doTest2(new SynchronousQueue2<Integer>(true), N) + "/s");
                System.out.println();
            }

            executor.shutdown();
        }
    }

    private static long doTest2(final BlockingQueue<Integer> q, final int n)
            throws Exception {
        CompletionService<Long> completionServ = new ExecutorCompletionService<Long>(executor);

        long t = System.nanoTime();
        for (int i = 0; i < THREAD_NUM; i++) {
            executor.submit(new Producer(n / THREAD_NUM, q));
        }
        for (int i = 0; i < THREAD_NUM; i++) {
            completionServ.submit(new Consumer(n / THREAD_NUM, q));
        }

        for (int i = 0; i < THREAD_NUM; i++) {
            completionServ.take().get();
        }

        t = System.nanoTime() - t;
        return (long) (1000000000.0 * N / t); // Throughput, items/sec
    }

    private static class Producer implements Runnable {
        int n;
        BlockingQueue<Integer> q;

        public Producer(int initN, BlockingQueue<Integer> initQ) {
            n = initN;
            q = initQ;
        }

        public void run() {
            for (int i = 0; i < n; i++)
                try {
                    q.put(i);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
        }
    }

    private static class Consumer implements Callable<Long> {
        int n;
        BlockingQueue<Integer> q;

        public Consumer(int initN, BlockingQueue<Integer> initQ) {
            n = initN;
            q = initQ;
        }

        public Long call() {
            long sum = 0;
            for (int i = 0; i < n; i++) {
                try {
                    sum += q.take();
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            return sum;
        }
    }
}

