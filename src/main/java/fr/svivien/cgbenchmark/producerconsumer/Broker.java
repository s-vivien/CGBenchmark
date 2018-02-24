package fr.svivien.cgbenchmark.producerconsumer;

import fr.svivien.cgbenchmark.model.test.TestInput;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Stocks test instances, consumers will consume them from here
 */
public class Broker {
    public ArrayBlockingQueue<TestInput> queue = new ArrayBlockingQueue<>(5000);

    public TestInput getNextTest() throws InterruptedException {
        return this.queue.poll(1, TimeUnit.SECONDS);
    }

    public int getTestSize() {
        return queue.size();
    }
}
