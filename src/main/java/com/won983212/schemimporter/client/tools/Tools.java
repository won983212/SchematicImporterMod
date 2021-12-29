package com.won983212.schemimporter.client.tools;

import com.won983212.schemimporter.ModIcons;
import com.won983212.schemimporter.SchematicImporterMod;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public enum Tools {

    Move(new MoveTool(), ModIcons.TOOL_MOVE_XZ),
    MoveY(new MoveVerticalTool(), ModIcons.TOOL_MOVE_Y),
    Deploy(new DeployTool(), ModIcons.TOOL_DEPLOY),
    Rotate(new RotateTool(), ModIcons.TOOL_ROTATE),
    Flip(new FlipTool(), ModIcons.TOOL_MIRROR),
    ToggleAir(new ToggleAirTool(), ModIcons.EMPTY_BLOCK),
    Print(new PlaceTool(), ModIcons.CONFIRM);

    private final ISchematicTool tool;
    private final ModIcons icon;

    Tools(ISchematicTool tool, ModIcons icon) {
        this.tool = tool;
        this.icon = icon;
    }

    public ISchematicTool getTool() {
        return tool;
    }

    public TranslationTextComponent getDisplayName() {
        return SchematicImporterMod.translate("schematic.tool." + name().toLowerCase(Locale.ROOT));
    }

    public ModIcons getIcon() {
        return icon;
    }

    public List<ITextComponent> getDescription() {
        List<ITextComponent> result = new ArrayList<>(4);
        for (int i = 0; i < 4; i++) {
            result.add(SchematicImporterMod.translate("schematic.tool." + name().toLowerCase(Locale.ROOT) + ".description." + i));
        }
        return result;
    }

}
