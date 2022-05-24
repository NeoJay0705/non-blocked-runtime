package org.easylibs.iobound;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.easylibs.cpubound.ThreadPool;
import org.easylibs.exception.UndeclaredIOTypeException;
import org.easylibs.messagequeue.IMessageQueue;

public class IORuntime {
    private Map<String, ThreadPool> ioManager;
    private IMessageQueue<Runnable> cpuTaskQueue;
    private ThreadPool cpuRuntime;
    private RunCount runCount;
    private Logger logger = Logger.getLogger(IORuntime.class.getCanonicalName()); 
    

    public IORuntime(ThreadPool cpuRuntime) {
        this.ioManager = Collections.synchronizedMap(new HashMap<>());
        this.cpuTaskQueue = cpuRuntime.getTaskQueue();
        this.cpuRuntime = cpuRuntime;
        this.runCount = new RunCount();
    }

    /**
     * <p>Create a thread pool for the {@code ioType}.</p>
     * 
     * <p>Returns {@code true} if a new thread pool has created.</p>
     * <p>Returns {@code false} if the {@code ioType} has already existed.</p>
     * 
     * @param ioType
     * @param threadPoolSize
     * @return - {@code boolean}
     */
    public boolean createIOThread(String ioType, int threadPoolSize) {
        synchronized (this.ioManager) {
            if (this.ioManager.containsKey(ioType)) {
                return false;
            }
            ThreadPool tm = new ThreadPool(threadPoolSize);
            ioManager.put(ioType, tm);
            return true;
        }
    }
    
    /**
     * Specific an {@code IOType}, define I/O task part, and the callback of CPU task.
     * 
     * @param <T>
     * @param IOType
     * @param IOTask
     * @param callbackCPUTask
     * @param except
     * @throws UndeclaredIOTypeException
     */
    public <T> void runNonBlocked(String IOType, Supplier<T> IOTask, Consumer<T> callbackCPUTask, Consumer<Throwable> except) throws UndeclaredIOTypeException {
        if (!ioManager.containsKey(IOType)) throw new UndeclaredIOTypeException(IOType);
        
        runCount.Add();
        ioManager.get(IOType).getTaskQueue().put(() -> {
            try {
                T returnValue = IOTask.get();
                runCount.Add();
                cpuTaskQueue.put(() -> {
                    try {
                        callbackCPUTask.accept(returnValue);
                    } catch (Exception e) {
                        except.accept(e);
                    } finally {
                        runCount.Sub();
                    }
                });
            } catch (Exception e) {
                except.accept(e);
            } finally {
                runCount.Sub();
            }
        });
    }

    /**
     * Close the threads of all pool including CPU thread pool of the input.
     */
    public void join() {
        while (true) {
            synchronized (runCount) {
                if (runCount.getRunCount() == 0) {
                    ioManager.forEach((k, q) -> {
                        q.join();
                    });
                    cpuRuntime.join();
                    break;
                } else {
                    try {
                        runCount.wait();
                    } catch (InterruptedException e) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        PrintStream ps = new PrintStream(baos);
                        e.printStackTrace(ps);
                        logger.severe(baos.toString());
                    }
                }
            }
        }
    }

    class RunCount {
        private int runCount;

        public RunCount() {
            this.runCount = 0;
        }

        public void Add() {
            synchronized (this) {
                runCount++;
            }
        }

        public void Sub() {
            synchronized (this) {
                runCount--;
                this.notifyAll();
            }
        }

        public int getRunCount() {
            return runCount;
        }

        @Override
        public String toString() {
            return Integer.toString(runCount);
        }
    }
}
