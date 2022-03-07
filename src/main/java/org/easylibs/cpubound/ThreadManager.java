package org.easylibs.cpubound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.easylibs.messagequeue.GenericMessageQueue;
import org.easylibs.messagequeue.IMessageQueue;

public class ThreadManager {
    public static final Runnable NullTask = () -> {};

    private IMessageQueue<Runnable> taskQueue;
    private List<Thread> workThreads;
    private int numberOfThread;

    public ThreadManager() {
        this(Runtime.getRuntime().availableProcessors());
    }

    public ThreadManager(int threadPoolSize) {
        this.numberOfThread = threadPoolSize;
        taskQueue = new GenericMessageQueue<>();
        workThreads = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadPoolSize; i++) {
            createCPUThread();
        }
    }

    public void createCPUThread() {
        Thread workThread = new Thread(() -> {
            while (true) {
                try {
                    Runnable task = taskQueue.get();
                    if (task != ThreadManager.NullTask) {
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

    public IMessageQueue<Runnable> getTaskQueue() {
        return taskQueue;
    }

    public void revert() {
        workThreads.remove(Thread.currentThread());
        createCPUThread();
    }

    public void join() {
        for (int i = 0; i < numberOfThread; i++) {
            taskQueue.put(ThreadManager.NullTask);
        }
    }
}
