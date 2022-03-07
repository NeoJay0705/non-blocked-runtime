package org.easylibs.messagequeue;

import java.util.ArrayDeque;
import java.util.Deque;

public class GenericMessageQueue<T> implements IMessageQueue<T> {

    private Deque<T> queue;

    public GenericMessageQueue() {
        queue = new ArrayDeque<>();
    }

    @Override
    public void put(T data) {
        synchronized (queue) {
            queue.addLast(data);
            queue.notifyAll();
        }
    }

    @Override
    public T get() throws InterruptedException {
        while (true) {
            synchronized (queue) {
                if (queue.size() == 0) {
                    queue.wait();
                } else {
                    return queue.removeFirst();
                }
            }
        }
    }
}
