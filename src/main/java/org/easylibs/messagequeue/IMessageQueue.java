package org.easylibs.messagequeue;

public interface IMessageQueue<T> {
    public void put(T data);
    public T get() throws InterruptedException;
}
