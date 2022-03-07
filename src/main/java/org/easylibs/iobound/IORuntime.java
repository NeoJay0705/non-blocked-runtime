package org.easylibs.iobound;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.easylibs.cpubound.ThreadManager;
import org.easylibs.messagequeue.IMessageQueue;

public class IORuntime {
    private Map<String, ThreadManager> ioManager;
    private IMessageQueue<Runnable> cpuTaskQueue;
    private ThreadManager cpuRuntime;
    private RunCount runCount;
    private Logger logger = Logger.getLogger(IORuntime.class.getCanonicalName()); 
    

    public IORuntime(ThreadManager cpuRuntime) {
        this.ioManager = Collections.synchronizedMap(new HashMap<>());
        this.cpuTaskQueue = cpuRuntime.getTaskQueue();
        this.cpuRuntime = cpuRuntime;
        this.runCount = new RunCount();
    }

    public void createIOThread(String ioType, int threadPoolSize) {
        ThreadManager tm = new ThreadManager(threadPoolSize);
        ioManager.put(ioType, tm);
    }
    
    public <T> void runNonBlocked(String ioType, Supplier<T> ioTask, Consumer<T> then, Consumer<Throwable> except) {
        runCount.Add();
        ioManager.get(ioType).getTaskQueue().put(() -> {
            try {
                T returnValue = ioTask.get();
                runCount.Add();
                cpuTaskQueue.put(() -> {
                    try {
                        then.accept(returnValue);
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
