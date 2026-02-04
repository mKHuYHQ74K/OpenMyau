package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.Render2DEvent;
import myau.events.TickEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.util.KeyBindUtil;
import net.minecraft.client.Minecraft;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.concurrent.locks.LockSupport;

public class Timer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private final net.minecraft.util.Timer timer;
    public final FloatProperty speed = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);
    private boolean isRelease = false;

    public Timer() {
        super("Timer", false);
        timer = ((IAccessorMinecraft)mc).getTimer();
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.speed.getValue() == 0) {
            if (mc.currentScreen != null) {
                this.setEnabled(false);
                return;
            }
            if (!KeyBindUtil.isKeyDown(this.key)) {
                this.isRelease = true;
            } else if (this.isRelease) {
                this.setEnabled(false);
            }
        }
    }

    @Override
    public void onEnabled() {
        if (this.speed.getValue() == 0.0f && this.key == 0 && mc.currentScreen == null || this.isRelease) {
            this.setEnabled(false);
            this.isRelease = false;
        } else {
            this.timer.timerSpeed = this.speed.getValue();
        }
    }

    @Override
    public void onDisabled() {
        this.timer.timerSpeed = 1.0f;
    }

    @Override
    public String[] getSuffix() {
        return new String[]{df.format(this.speed.getValue())};
    }

    @Override
    public void verifyValue(String string) {
        if (this.isEnabled() && string.equals(this.speed.getName())) {
            this.onEnabled();
        }
    }
}
