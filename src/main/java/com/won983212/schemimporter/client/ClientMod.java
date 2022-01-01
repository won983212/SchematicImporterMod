package com.won983212.schemimporter.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.won983212.schemimporter.CommonMod;
import com.won983212.schemimporter.ModKeys;
import com.won983212.schemimporter.SchematicImporterMod;
import com.won983212.schemimporter.client.gui.SchematicStatusScreen;
import com.won983212.schemimporter.client.render.ChunkVertexBuffer;
import com.won983212.schemimporter.client.render.SuperRenderTypeBuffer;
import com.won983212.schemimporter.network.loader.ClientSchematicLoader;
import com.won983212.schemimporter.schematic.parser.SchematicFileParser;
import com.won983212.schemimporter.task.TaskScheduler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import java.awt.*;

@Mod.EventBusSubscriber(modid = SchematicImporterMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class ClientMod extends CommonMod {
    public static final TaskScheduler CLIENT_SCHEDULER = new TaskScheduler();
    public static final ClientSchematicLoader CLIENT_SCHEMATIC_LOADER = new ClientSchematicLoader();
    public static final SchematicStatusScreen SCHEMATIC_UPLOAD_SCREEN = new SchematicStatusScreen();
    public static final SchematicHandler SCHEMATIC_HANDLER = new SchematicHandler();

    static {
        CLIENT_SCHEMATIC_LOADER.registerProgressProducers();
        SCHEMATIC_UPLOAD_SCREEN.registerProgressProducer(SCHEMATIC_HANDLER.getRendererManager());
    }

    @Override
    public void onCommonSetup(FMLCommonSetupEvent event) {
        super.onCommonSetup(event);
        ModKeys.registerKeys();
    }

    @SubscribeEvent
    public static void onCameraSetup(EntityViewRenderEvent.CameraSetup e) {
        ChunkVertexBuffer.setCameraPosition(e.getInfo().getPosition());
    }

    @SubscribeEvent
    public static void onUnloadWorld(WorldEvent.Unload event) {
        if (event.getWorld().isClientSide()) {
            SchematicFileParser.clearCache();
            CLIENT_SCHEDULER.cancelAllTask();
            CLIENT_SCHEMATIC_LOADER.shutdown();
            ClientMod.SCHEMATIC_HANDLER.unload();
            CommonMod.SERVER_SCHEMATIC_LOADER.shutdown();
        }
    }

    @SubscribeEvent
    public static void onTick(TickEvent.ClientTickEvent event) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        CLIENT_SCHEDULER.tick();
        ClientMod.SCHEMATIC_HANDLER.tick();
        ClientMod.CLIENT_SCHEMATIC_LOADER.tick();
    }

    @SubscribeEvent
    public static void onRenderWorld(RenderWorldLastEvent event) {
        Vector3d cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        MatrixStack ms = event.getMatrixStack();

        ms.pushPose();
        ms.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z());

        SuperRenderTypeBuffer buffer = SuperRenderTypeBuffer.getInstance();
        ClientMod.SCHEMATIC_HANDLER.render(ms, buffer);
        buffer.draw();
        RenderSystem.enableCull();

        ms.popPose();
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.HOTBAR) {
            return;
        }

        MatrixStack ms = event.getMatrixStack();
        IRenderTypeBuffer.Impl buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        Point mousePos = getMousePosition();
        ClientMod.SCHEMATIC_HANDLER.renderOverlay(ms, buffers, event.getPartialTicks());
        ClientMod.SCHEMATIC_UPLOAD_SCREEN.render(ms, mousePos.x, mousePos.y, event.getPartialTicks());
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.KeyInputEvent e) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        int key = e.getKey();
        boolean pressed = !(e.getAction() == 0);
        ClientMod.SCHEMATIC_HANDLER.onKeyInput(key, pressed);
    }

    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseInputEvent event) {
        int button = event.getButton();
        boolean pressed = !(event.getAction() == 0);

        if (Minecraft.getInstance().screen == null) {
            ClientMod.SCHEMATIC_HANDLER.onMouseInput(button, pressed);
        }
    }

    @SubscribeEvent
    public static void onMouseScrolled(InputEvent.MouseScrollEvent event) {
        if (Minecraft.getInstance().screen != null) {
            return;
        }

        double delta = event.getScrollDelta();
        event.setCanceled(ClientMod.SCHEMATIC_HANDLER.mouseScrolled(delta));
    }

    private static Point getMousePosition() {
        Minecraft mc = Minecraft.getInstance();
        int i = (int) (mc.mouseHandler.xpos() * (double) mc.getWindow().getGuiScaledWidth() / (double) mc.getWindow().getScreenWidth());
        int j = (int) (mc.mouseHandler.ypos() * (double) mc.getWindow().getGuiScaledHeight() / (double) mc.getWindow().getScreenHeight());
        return new Point(i, j);
    }
}
