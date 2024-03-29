package com.won983212.schemimporter.client.gui;

import com.google.common.collect.Iterables;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.won983212.schemimporter.ModTextures;
import com.won983212.schemimporter.Settings;
import com.won983212.schemimporter.client.ClientMod;
import com.won983212.schemimporter.client.gui.component.HoveringCover;
import com.won983212.schemimporter.client.gui.component.ScrollSelector;
import com.won983212.schemimporter.network.NetworkDispatcher;
import com.won983212.schemimporter.network.packets.CSchematicFileDelete;
import com.won983212.schemimporter.schematic.SchematicFile;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class SchematicSelectionScreen extends PanelScreen {
    private final List<SchematicFileValue> schematicFiles = new ArrayList<>();
    private ScrollSelector fileSelector;
    private HoveringCover openBtn;
    private HoveringCover uploadBtn;
    private HoveringCover deleteBtn;

    public SchematicSelectionScreen(List<SchematicFile> serverSideFiles) {
        super(new StringTextComponent("Schematic Selection Screen"));

        Set<SchematicFile> clientFileSet = new HashSet<>(SchematicFile.getFileList(""));
        for (SchematicFile serverFile : serverSideFiles) {
            if (!clientFileSet.contains(serverFile)) {
                continue;
            }
            schematicFiles.add(new SchematicFileValue(false, serverFile));
            clientFileSet.remove(serverFile);
        }
        schematicFiles.addAll(clientFileSet.stream()
                .map((t) -> new SchematicFileValue(true, t))
                .collect(Collectors.toList()));
        Collections.sort(schematicFiles);
    }

    private void onAccept(HoveringCover btn) {
        if (btn == openBtn) {
            try {
                Path p = Paths.get(Minecraft.getInstance().gameDirectory.toString(), Settings.SCHEMATIC_DIR_NAME);
                Runtime.getRuntime().exec("explorer " + p.toAbsolutePath().toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        int index = fileSelector.getSelectedIndex();
        if (index == -1) {
            onClose();
            return;
        }

        SchematicFileValue file = schematicFiles.get(index);
        if (btn == uploadBtn) {
            ClientMod.CLIENT_SCHEMATIC_LOADER.startNewUpload(file.schematicFile);
            onClose();
        } else if (btn == deleteBtn) {
            if (file.needsUpload) {
                alert("서버에 업로드된 파일만 삭제할 수 있습니다!");
                return;
            }
            NetworkDispatcher.sendToServer(new CSchematicFileDelete(file.getName()));
        }
    }

    @Override
    protected void init() {
        super.init();
        this.children.clear();
        this.openBtn = new HoveringCover(21, 21, 18, 18, SchematicSelectionScreen.this::onAccept);
        this.children.add(openBtn);
        this.uploadBtn = new HoveringCover(178, 109, 18, 18, SchematicSelectionScreen.this::onAccept);
        this.children.add(uploadBtn);
        this.deleteBtn = new HoveringCover(152, 109, 18, 18, SchematicSelectionScreen.this::onAccept);
        this.children.add(deleteBtn);
        this.fileSelector = new ScrollSelector(45, 21, 139, 72, schematicFiles);
        this.children.add(fileSelector);
        applyBackgroundOffset(ModTextures.SCHEMATIC_SELECT_BACKGROUND);
    }

    @Override
    public void render(MatrixStack ms, int mouseX, int mouseY, float partialTime) {
        this.renderBackground(ms);
        drawTexturedBackground(ms, ModTextures.SCHEMATIC_SELECT_BACKGROUND);
        super.render(ms, mouseX, mouseY, partialTime);
        drawCenteredString(ms, font, "마우스 휠로 파일을 선택하세요.", width / 2, (height + ModTextures.SCHEMATIC_SELECT_BACKGROUND.height) / 2 + 6, 0xffffffff);
        drawCenteredString(ms, font, "§n[밑줄]§r 으로 된 파일은 업로드가 필요한 파일입니다.", width / 2, (height + ModTextures.SCHEMATIC_SELECT_BACKGROUND.height) / 2 + 20, 0xffffffff);
        drawCenteredString(ms, font, "이미 업로드된 파일은 [삭제]버튼을 눌러 삭제할 수 있습니다.", width / 2, (height + ModTextures.SCHEMATIC_SELECT_BACKGROUND.height) / 2 + 34, 0xffffffff);
    }

    public void onResponseFileDeletion(String fileName) {
        alert(fileName + "이 서버에서 삭제되었습니다.", 1000);
        int index = Iterables.indexOf(schematicFiles, (t) -> t.getName().equals(fileName));
        if (index >= 0) {
            schematicFiles.get(index).needsUpload = true;
        }
    }

    private static class SchematicFileValue implements Comparable<SchematicFileValue> {
        public boolean needsUpload;
        public final SchematicFile schematicFile;

        public SchematicFileValue(boolean needsUpload, SchematicFile schematicFile) {
            this.needsUpload = needsUpload;
            this.schematicFile = schematicFile;
        }

        public String getName() {
            return schematicFile.getName();
        }

        @Override
        public int compareTo(SchematicFileValue o) {
            return schematicFile.getName().compareTo(o.schematicFile.getName());
        }

        @Override
        public String toString() {
            return needsUpload ? ("§n" + getName()) : getName();
        }
    }
}
