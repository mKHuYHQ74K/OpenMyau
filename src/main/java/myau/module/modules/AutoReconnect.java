package myau.module.modules;

import myau.module.Module;
import myau.property.properties.IntProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class AutoReconnect extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty delay = new IntProperty("Delay", 5, 0, 30);

    private int timer = -1;
    private ServerData lastServerData;

    public AutoReconnect() {
        super("AutoReconnect", false);
    }

    @Override
    public void onEnabled() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisabled() {
        MinecraftForge.EVENT_BUS.unregister(this);
        reset();
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiDisconnected)) return;

        lastServerData = mc.getCurrentServerData();
        if (lastServerData == null) return;

        timer = delay.getValue() * 20;
        if (timer <= 0) {
            reconnect();
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (timer < 0) return;

        if (!(mc.currentScreen instanceof GuiDisconnected) || lastServerData == null) {
            reset();
            return;
        }

        if (--timer == 0) {
            reconnect();
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiDisconnected)) return;
        if (timer <= 0 || lastServerData == null) return;

        int seconds = timer / 20 + 1;
        ScaledResolution res = new ScaledResolution(mc);
        String text = "Reconnecting in " + seconds + "s...";
        mc.fontRendererObj.drawStringWithShadow(
                text,
                (float) res.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(text) / 2.0F,
                (float) res.getScaledHeight() / 2 + 30,
                0x55FF55
        );
    }

    private void reconnect() {
        if (lastServerData == null) return;

        mc.displayGuiScreen(new GuiConnecting(
                new GuiMultiplayer(new GuiMainMenu()),
                mc,
                lastServerData
        ));
        reset();
    }

    private void reset() {
        timer = -1;
        lastServerData = null;
    }
}
