package com.won983212.schemimporter.task;

public interface IAsyncTask<T> {
    /**
     * @return is this task end?
     */
    boolean tick();

    T getResult();

    default void setScheduler(TaskScheduler scheduler){
    }
}
