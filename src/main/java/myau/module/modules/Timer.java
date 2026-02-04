package myau.module.modules;

import myau.event.EventManager;
import myau.event.EventTarget;
import myau.events.KeyEvent;
import myau.events.Render2DEvent;
import myau.mixin.IAccessorMinecraft;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class Timer extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final DecimalFormat df = new DecimalFormat("0.0#", new DecimalFormatSymbols(Locale.US));
    private final net.minecraft.util.Timer timer;
    public final FloatProperty speed = new FloatProperty("speed", 1.0f, 0.0f, 10.0f);

    public Timer() {
        super("Timer", false);
        timer = ((IAccessorMinecraft)mc).getTimer();
    }

    @EventTarget
    public void onRender(Render2DEvent event) {
        if (this.isEnabled() && this.speed.getValue() == 0) {
            if (mc.currentScreen != null) {
                this.toggle();
                return;
            }
            while (Mouse.next()) {
                if (Mouse.getEventButtonState()) {
                    int i = Mouse.getEventButton() - 100;
                    if (i == this.key) {
                        EventManager.call(new KeyEvent(this.key));
                    }
                }
            }
            while (Keyboard.next()) {
                if (Keyboard.getEventKeyState()) {
                    int k = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
                    if (k == this.key) {
                        EventManager.call(new KeyEvent(this.key));
                    }
                }
            }
        }
    }

    @Override
    public void onEnabled() {
        if (this.speed.getValue() == 0.0f && this.key == 0 && mc.currentScreen == null) {
            this.toggle();
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
