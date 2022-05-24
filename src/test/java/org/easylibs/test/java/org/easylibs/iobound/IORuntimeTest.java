package org.easylibs.test.java.org.easylibs.iobound;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.easylibs.cpubound.ThreadPool;
import org.easylibs.exception.UndeclaredIOTypeException;
import org.easylibs.iobound.IORuntime;
import org.easylibs.messagequeue.IMessageQueue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class IORuntimeTest {
    private IORuntime ioRuntime;

    @BeforeEach
    public void resetRuntime() {
        this.ioRuntime = new IORuntime(new ThreadPool());
        this.ioRuntime.createIOThread("stdio", 2);
    }

    @Test
    public void joinAThreadPool() throws InterruptedException {
        ThreadPool tp = new ThreadPool(8);
        Assertions.assertEquals(8, tp.getPoolSize());
        IMessageQueue<Runnable> taskQueue = tp.getTaskQueue();
        tp.join();

        List<Integer> result = new ArrayList<>();
        taskQueue.put(() -> {
            synchronized (result) {
                result.add(1);
            }
        });

        Thread.sleep(500);

        Assertions.assertEquals(0, result.size());
    }

    @Test
    public void assignTaskToThreadPool() throws InterruptedException {
        ThreadPool tp = new ThreadPool(8);
        Assertions.assertEquals(8, tp.getPoolSize());
        IMessageQueue<Runnable> taskQueue = tp.getTaskQueue();

        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            final int x = i;
            taskQueue.put(() -> {
                try {
                    Thread.sleep(50 * x);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (result) {
                    result.add(x);
                }
            });
        }

        Thread.sleep(500);

        for (int i = 0; i < 8; i++) {
            Assertions.assertEquals(i, result.get(i));
        }
    }

    @Test
    public void stuckInSameIO() throws UndeclaredIOTypeException, InterruptedException {
        List<Instant> result = new ArrayList<>();

        ioRuntime.createIOThread("a", 1);

        ioRuntime.runNonBlocked("a", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Instant end = Instant.now();
            result.add(end);
            return null;
        }, (x) -> {
            
        }, (err) -> {

        });

        ioRuntime.runNonBlocked("a", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Instant end = Instant.now();
            result.add(end);
            return null;
        }, (x) -> {
            
        }, (err) -> {

        });

        Thread.sleep(2500);
        
        long diff = Duration.between(result.get(0), result.get(1)).toMillis();
        Assertions.assertEquals(true, diff > 1000 && diff < 1200);
    }

    @Test
    public void concurrentInDifferentIO() throws UndeclaredIOTypeException, InterruptedException {
        List<Instant> result = new ArrayList<>();

        ioRuntime.createIOThread("a", 1);
        ioRuntime.createIOThread("b", 1);

        ioRuntime.runNonBlocked("a", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Instant end = Instant.now();
            result.add(end);
            return null;
        }, (x) -> {
            
        }, (err) -> {

        });

        ioRuntime.runNonBlocked("b", () -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Instant end = Instant.now();
            result.add(end);
            return null;
        }, (x) -> {
            
        }, (err) -> {

        });

        Thread.sleep(1500);
        
        Assertions.assertEquals(true, Duration.between(result.get(0), result.get(1)).toMillis() < 100);
    }

    @Test
    public void throwExceptionInIO() throws UndeclaredIOTypeException, InterruptedException {
        List<Integer> pool = new ArrayList<>();
        
        ioRuntime.runNonBlocked("stdio", () -> {
            if (!Objects.isNull(pool)) throw new RuntimeException("3");
            pool.add(1);
            return 2;
        }, (x) -> {
            pool.add(x);
        }, (err) -> {
            pool.add(Integer.parseInt(err.getMessage()));
        });

        Thread.sleep(500);

        Assertions.assertEquals(3, pool.get(0));
    }

    @Test
    public void throwExceptionInCPU() throws UndeclaredIOTypeException, InterruptedException {
        List<Integer> pool = new ArrayList<>();
        
        ioRuntime.runNonBlocked("stdio", () -> {
            pool.add(1);
            return 2;
        }, (x) -> {
            if (!Objects.isNull(pool)) throw new RuntimeException("3");
            pool.add(x);
        }, (err) -> {
            pool.add(Integer.parseInt(err.getMessage()));
        });

        Thread.sleep(500);

        Assertions.assertEquals(1, pool.get(0));
        Assertions.assertEquals(3, pool.get(1));
    }

}
