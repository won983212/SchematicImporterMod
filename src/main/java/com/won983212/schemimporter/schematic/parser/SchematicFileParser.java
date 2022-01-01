package com.won983212.schemimporter.schematic.parser;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.won983212.schemimporter.schematic.IProgressEvent;
import com.won983212.schemimporter.schematic.SchematicFile;
import com.won983212.schemimporter.schematic.container.SchematicContainer;
import com.won983212.schemimporter.task.FinishedTask;
import com.won983212.schemimporter.task.IAsyncTask;
import com.won983212.schemimporter.task.QueuedAsyncTask;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

// TODO 같은 File을 두 명이 동시에 print하면, 한명이 기다리도록 하기.
public class SchematicFileParser {

    private static final Cache<String, SchematicContainer> schematicCache;
    private static final HashMap<String, QueuedAsyncTask<SchematicContainer>> loadingMap = new HashMap<>();
    private static final HashMap<String, Class<? extends AbstractSchematicReader>> extensionToReaderMap = new HashMap<>();

    static {
        schematicCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build();
        extensionToReaderMap.put("schematic", MCEditSchematicReader.class);
        extensionToReaderMap.put("schem", SpongeSchematicReader.class);
    }

    public static boolean isUnsupportedExtension(String fileName) {
        return !extensionToReaderMap.containsKey(fileName.substring(fileName.lastIndexOf('.') + 1));
    }

    public static BlockPos parseSchematicSizeFromItem(ItemStack blueprint) {
        try {
            File file = SchematicFile.getFilePathFromItemStack(blueprint).toFile();
            SchematicContainer t = schematicCache.getIfPresent(file.getAbsolutePath());
            if (t != null) {
                return t.getSize();
            }
            return createSupportedReader(file).parseSize();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return BlockPos.ZERO;
    }

    public static IAsyncTask<SchematicContainer> parseSchematicFromItemAsync(ItemStack blueprint, IProgressEvent event) {
        try {
            Path path = SchematicFile.getFilePathFromItemStack(blueprint);
            return SchematicFileParser.parseSchematicFileAsync(path.toFile(), event);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new FinishedTask<>(new SchematicContainer(BlockPos.ZERO));
    }

    public static IAsyncTask<SchematicContainer> parseSchematicFileAsync(File file, IProgressEvent event) throws IOException {
        String filePath = file.getAbsolutePath();
        SchematicContainer container = schematicCache.getIfPresent(filePath);

        if (container != null) {
            return new FinishedTask<>(container);
        }

        AbstractSchematicReader reader = createSupportedReader(file);
        reader.setProgressEvent(event);
        reader.setCompletedEvent((c) -> schematicCache.put(filePath, c));
        return reader;
    }

    public static void clearCache() {
        schematicCache.invalidateAll();
    }

    private static AbstractSchematicReader createSupportedReader(File file) throws IOException {
        String fileName = file.getName();
        String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
        try {
            Class<? extends AbstractSchematicReader> readerCls = extensionToReaderMap.get(fileExtension);
            if (readerCls == null) {
                throw new IOException("Unsupported type: " + fileExtension);
            }
            return readerCls.getConstructor(File.class).newInstance(file);
        } catch (InstantiationException | IllegalAccessException |
                NoSuchMethodException | InvocationTargetException e) {
            throw new IOException(e);
        }
    }
}
