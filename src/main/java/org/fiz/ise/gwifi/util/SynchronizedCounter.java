package org.fiz.ise.gwifi.util;
public class SynchronizedCounter {
    private int c = 0;

    public synchronized void increment() {
        c++;
    }
    public synchronized void incrementbyValue(int value) {
        c+=value;
    }
    public synchronized void decrement() {
        c--;
    }

    public synchronized int value() {
        return c;
    }

}