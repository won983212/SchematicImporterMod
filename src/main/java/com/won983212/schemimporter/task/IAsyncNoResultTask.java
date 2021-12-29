package com.won983212.schemimporter.task;

public interface IAsyncNoResultTask extends IAsyncTask<Void> {
    /**
     * @return is this task end?
     */
    boolean tick();

    default Void getResult() {
        return null;
    }
}
