package org.easylibs;

import org.easylibs.cpubound.ThreadManager;
import org.easylibs.iobound.IORuntime;

public class AppMain {
    public static void main(String[] args) {
        IORuntime ioRuntime = new IORuntime(new ThreadManager());
        ioRuntime.createIOThread("screen", 1);

        ioRuntime.runNonBlocked("screen", () -> {
            return "screen.a.io";
        }, (s) -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            System.out.println(s + "/then");
        }, (e) -> {
            System.out.println("a error\n" + e.getMessage());
        });

        ioRuntime.runNonBlocked("screen", () -> {
            return "screen.b.io";
        }, (s) -> {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            System.out.println(s + "/then");
        }, (e) -> {
            System.out.println("b error\n" + e.getMessage());
        });

        System.out.println("After run non blocked and Before Join");
        ioRuntime.join();
        System.out.println("After Join");
    }
}
