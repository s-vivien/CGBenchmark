package fr.svivien.cgbenchmark.business;

import fr.svivien.cgbenchmark.model.test.TestInput;
import lombok.Data;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Holds test instances, consumers will consume them from here
 */
@Data
public class TestBroker {

    private String codeContent;
    private String codeLanguage;
    private ArrayBlockingQueue<TestInput> queue = new ArrayBlockingQueue<>(5000);

    public void reset() {
        queue.clear();
    }

    public void addTest(TestInput test) {
        queue.add(test);
    }

    public int size() {
        return queue.size();
    }

    public TestInput getNextTest() throws InterruptedException {
        return this.queue.poll(1, TimeUnit.SECONDS);
    }
}
