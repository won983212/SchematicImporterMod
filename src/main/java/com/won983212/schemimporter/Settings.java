package com.won983212.schemimporter;

public class Settings {
    public static final long CRITERIA_TIME_SCHEMATIC_PARSER = 20;
    public static final long CRITERIA_TIME_SCHEMATIC_PRINTER = 20;
    public static final long CRITERIA_TIME_SCHEMATIC_RENDERER = 20;

    public static final int SCHEMATIC_IDLE_TIMEOUT = 600;
    public static final int MAX_SCHEMATICS = 256;
    public static final int MAX_TOTAL_SCHEMATIC_SIZE = 1024;
    public static final int SCHEMATIC_PACKET_SIZE = 4096;
    public static final int PACKET_DELAY = 10;

    public static final String SCHEMATIC_DIR_NAME = "schematics";
    public static final String UPLOADED_SCHEMATIC_DIR_NAME = "uploaded";
}
