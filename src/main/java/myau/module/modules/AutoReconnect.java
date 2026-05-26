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

import java.net.InetSocketAddress;
import java.net.Socket;

public class AutoReconnect extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final IntProperty delay = new IntProperty("Delay", 5, 0, 30);

    private int timer = -1;
    private ServerData lastServerData;
    private boolean pinging;

    public AutoReconnect() {
        super("AutoReconnect", false);
    }

    @Override
    public void onEnabled() {
        try {
            MinecraftForge.EVENT_BUS.register(this);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDisabled() {
        try {
            MinecraftForge.EVENT_BUS.unregister(this);
        } catch (Throwable ignored) {}
        reset();
    }

    @SubscribeEvent
    public void onGuiInit(GuiScreenEvent.InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiDisconnected)) return;

        lastServerData = mc.getCurrentServerData();
        if (lastServerData == null) return;

        timer = delay.getValue() * 20;
        pinging = false;
        if (timer <= 0) {
            tryReconnect();
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
            tryReconnect();
        }
    }

    @SubscribeEvent
    public void onDrawScreen(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiDisconnected)) return;
        if (timer <= 0 || lastServerData == null) return;

        String text;
        if (pinging) {
            text = "Pinging server...";
        } else {
            int seconds = timer / 20 + 1;
            text = "Reconnecting in " + seconds + "s...";
        }

        ScaledResolution res = new ScaledResolution(mc);
        mc.fontRendererObj.drawStringWithShadow(
                text,
                (float) res.getScaledWidth() / 2 - mc.fontRendererObj.getStringWidth(text) / 2.0F,
                (float) res.getScaledHeight() / 2 + 30,
                0x55FF55
        );
    }

    private void tryReconnect() {
        if (lastServerData == null) return;

        pinging = true;
        new Thread(() -> {
            boolean reachable = pingServer(lastServerData);
            if (reachable) {
                mc.addScheduledTask(this::reconnect);
            } else {
                // Server not reachable, restart timer
                mc.addScheduledTask(() -> {
                    if (mc.currentScreen instanceof GuiDisconnected) {
                        timer = delay.getValue() * 20;
                        pinging = false;
                    }
                });
            }
        }, "AutoReconnect-Ping").start();
    }

    private boolean pingServer(ServerData serverData) {
        if (serverData.serverIP == null) return false;

        try {
            String[] parts = serverData.serverIP.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 25565;

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 3000);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
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
        pinging = false;
    }
}
