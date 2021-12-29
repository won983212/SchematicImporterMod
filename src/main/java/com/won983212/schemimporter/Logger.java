package com.won983212.schemimporter;

import org.apache.logging.log4j.LogManager;

public class Logger {
    private static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger();

    public static void info(Object obj) {
        LOGGER.info("[" + SchematicImporterMod.MODID + "] " + obj);
    }

    public static void warn(Object obj) {
        LOGGER.warn("[" + SchematicImporterMod.MODID + "] " + obj);
    }

    public static void error(String message) {
        LOGGER.error("[" + SchematicImporterMod.MODID + "] " + message);
    }

    public static void error(Throwable obj) {
        LOGGER.error("[" + SchematicImporterMod.MODID + "] ", obj);
    }

    public static void debug(Object obj) {
        LOGGER.debug("[" + SchematicImporterMod.MODID + "] " + obj);
    }
}