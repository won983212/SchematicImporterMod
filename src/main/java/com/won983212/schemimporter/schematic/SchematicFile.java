package com.won983212.schemimporter.schematic;

import com.won983212.schemimporter.Logger;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.schematic.parser.SchematicFileParser;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.thread.SidedThreadGroups;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SchematicFile {
    private final String owner;
    private final String name;
    private final String hash;


    private SchematicFile(String owner, String name, String hash) {
        this.owner = owner;
        this.name = name;
        this.hash = hash;
    }

    public SchematicFile(String owner, File f) {
        this(owner, f.getName(), loadHash(f));
    }

    public SchematicFile(PacketBuffer readBuffer) {
        this.owner = readBuffer.readUtf(64);
        this.name = readBuffer.readUtf(64);
        this.hash = readBuffer.readUtf(64);
    }

    public String getName() {
        return name;
    }

    public String getOwner() {
        return owner;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        } else if (!(obj instanceof SchematicFile)) {
            return false;
        } else {
            SchematicFile file = (SchematicFile) obj;
            return file.hash.equals(hash) && file.name.equals(name);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, name);
    }

    @Override
    public String toString() {
        return "SchematicFile[" + name + "]";
    }

    public void writeTo(PacketBuffer buffer) {
        buffer.writeUtf(owner, 64);
        buffer.writeUtf(name, 64);
        buffer.writeUtf(hash, 64);
    }

    public boolean isSameHash(Path filePath) {
        File file = filePath.toFile();
        if (!file.exists()) {
            return false;
        }
        return loadHash(file).equals(hash);
    }

    private static String loadHash(File f) {
        String hash = null;
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            hash = DigestUtils.md5Hex(is);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return hash;
    }

    public static List<SchematicFile> getFileList(String owner) {
        Path dir = getDirectoryPath(owner);
        File[] files = dir.toFile().listFiles();

        if (files == null) {
            Logger.warn("Can't find directory path: " + dir);
            return new ArrayList<>();
        }

        return Arrays.stream(files)
                .filter(File::isFile)
                .filter((f) -> !SchematicFileParser.isUnsupportedExtension(f.getName()))
                .map((f) -> new SchematicFile(owner, f))
                .collect(Collectors.toList());
    }

    public static Path getFilePath(String owner, String schematicFile) throws IOException {
        Path dir = getDirectoryPath(owner);
        Path path = dir.resolve(Paths.get(schematicFile)).normalize();
        if (!path.startsWith(dir)) {
            throw new IOException("Can't resolve path: " + path);
        }
        return path;
    }

    public static Path getDirectoryPath(String owner) {
        if (Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER) {
            return Paths.get(Settings.SCHEMATIC_DIR_NAME, Settings.UPLOADED_SCHEMATIC_DIR_NAME, owner).toAbsolutePath();
        } else {
            return Paths.get(Settings.SCHEMATIC_DIR_NAME).toAbsolutePath();
        }
    }

    public static Path getFilePathFromItemStack(ItemStack blueprint) throws IOException {
        String owner = blueprint.getTag().getString("Owner");
        String schematic = blueprint.getTag().getString("File");
        if (SchematicFileParser.isUnsupportedExtension(schematic)) {
            throw new IOException("Unsupported file!");
        }
        return getFilePath(owner, schematic);
    }

    public static String keyToName(String schematicKey){
        Path path = Paths.get(schematicKey);
        int nameCount = path.getNameCount();
        return path.subpath(nameCount - 1, nameCount).toString();
    }
}
