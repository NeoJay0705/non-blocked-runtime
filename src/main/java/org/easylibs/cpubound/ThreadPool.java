package org.easylibs.cpubound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.easylibs.exception.InvalidArgumentValueException;
import org.easylibs.messagequeue.GenericMessageQueue;
import org.easylibs.messagequeue.IMessageQueue;

public class ThreadPool {
    public static final Runnable NullTask = () -> {};

    private IMessageQueue<Runnable> taskQueue;
    private List<Thread> workThreads;
    private int numberOfThread;

    /**
     * Initialize a thread pool with the default number of thread in the current operating system.
     */
    public ThreadPool() {
        this(Runtime.getRuntime().availableProcessors());
    }

    /**
     * Initialize a thread pool with the specific number of thread.
     * 
     * @param threadPoolSize - {@code int}
     */
    public ThreadPool(int threadPoolSize) {
        if (threadPoolSize < 1) throw new InvalidArgumentValueException("The size of thread pool is invalid: " + threadPoolSize);

        this.numberOfThread = threadPoolSize;
        taskQueue = new GenericMessageQueue<>();
        workThreads = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadPoolSize; i++) {
            createCPUThread();
        }
    }

    /**
     * Create a thread to a thread pool managed by this object.
     */
    private void createCPUThread() {
        Thread workThread = new Thread(() -> {
            while (true) {
                try {
                    Runnable task = taskQueue.get();
                    if (task != ThreadPool.NullTask) {
                        task.run();
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    revert();
                }
            }
        });
        workThreads.add(workThread);
        workThread.start();
    }

    public int getPoolSize() {
        return numberOfThread;
    }

    /**
     * A queue for pended CPU tasks.
     * 
     * @return - {@code IMessageQueue<Runnable>}
     */
    public IMessageQueue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    /**
     * Remove the unhealthy thread by {@code Thread.currentThread()} and replace with a new thread.
     */
    public void revert() {
        workThreads.remove(Thread.currentThread());
        createCPUThread();
    }

    /**
     * End working threads by putting an empty lambda to all the threads.
     */
    public void join() {
        for (int i = 0; i < numberOfThread; i++) {
            taskQueue.put(ThreadPool.NullTask);
        }
    }
}
