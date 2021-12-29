package com.won983212.schemimporter.schematic;

public interface IProgressEntryProducer {
    Iterable<? extends IProgressEntry> getProgressEntries();

    int size();
}