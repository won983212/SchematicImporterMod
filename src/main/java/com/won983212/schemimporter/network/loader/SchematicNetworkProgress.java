package com.won983212.schemimporter.network.loader;

import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.schematic.IProgressEntry;

public class SchematicNetworkProgress<S> implements IProgressEntry {
    private static final String[] SIZE_UNITS = {"B", "KB", "MB", "GB"};
    private final String key;
    private final S obj;
    private final long totalBytes;
    private long bytesUploaded;
    private int idleTime;

    protected SchematicNetworkProgress(String key, S obj, long size) {
        this.key = key;
        this.obj = obj;
        this.totalBytes = size;
        this.bytesUploaded = 0;
        this.idleTime = 0;
    }

    @Override
    public String getTitle() {
        return getSchematicKey();
    }

    @Override
    public String getSubtitle() {
        long size = totalBytes;
        int unitIdx = 0;
        while (size >= 1024) {
            unitIdx++;
            size /= 1024;
        }
        return size + SIZE_UNITS[unitIdx];
    }

    public String getSchematicKey() {
        return key;
    }

    @Override
    public double getProgress() {
        return (double) bytesUploaded / totalBytes;
    }

    public S getValue() {
        return obj;
    }

    public void resetIdleTime() {
        this.idleTime = 0;
    }

    /**
     * @return return true if idletime is not exceed timeout.
     */
    public boolean increaseIdleTime() {
        return idleTime++ <= Settings.SCHEMATIC_IDLE_TIMEOUT;
    }

    /**
     * @return return true if <code>bytesUploaded <= totalBytes</code>
     */
    public boolean addUploadedBytes(long uploaded) {
        this.bytesUploaded += uploaded;
        return bytesUploaded <= totalBytes;
    }

    public void setUploaded(long uploaded) {
        this.bytesUploaded = uploaded;
    }

    public long getUploadedBytes() {
        return bytesUploaded;
    }

    public long getTotalBytes() {
        return totalBytes;
    }
}