package com.lev666;

@FunctionalInterface
public interface ProgressReporter {
    void report(String message);
}
