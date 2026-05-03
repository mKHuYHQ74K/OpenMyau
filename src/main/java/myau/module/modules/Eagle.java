package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.event.types.Priority;
import myau.events.MoveInputEvent;
import myau.events.TickEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;
import myau.util.ItemUtil;
import myau.util.MoveUtil;
import myau.util.PlayerUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.IntProperty;
import myau.util.RandomUtil;
import net.minecraft.client.Minecraft;
import org.apache.commons.lang3.RandomUtils;
import org.lwjgl.input.Keyboard;

import java.util.Objects;

public class Eagle extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private int sneakDelay = 0;
    private float sneakOffset = 1.0f;
    public final IntProperty minDelay = new IntProperty("min-delay", 2, 0, 10);
    public final IntProperty maxDelay = new IntProperty("max-delay", 3, 0, 10);
    public final BooleanProperty directionCheck = new BooleanProperty("direction-check", true);
    public final BooleanProperty pitchCheck = new BooleanProperty("pitch-check", true);
    public final BooleanProperty blocksOnly = new BooleanProperty("blocks-only", true);
    public final BooleanProperty sneakOnly = new BooleanProperty("sneaking-only", false);
    public final FloatProperty minOffset = new FloatProperty("min-offset", 0.0f, 0.0f, 9.0f);
    public final FloatProperty maxOffset = new FloatProperty("max-offset", 0.0f, 0.0f, 9.0f);

    private boolean canMoveSafely() {
        double[] offset = MoveUtil.predictMovement(this.sneakOffset);
        if (this.sneakOnly.getValue() && offset[0] == 0.0 && offset[1] == 0.0) return true;
        return PlayerUtil.canMove(mc.thePlayer.motionX + offset[0], mc.thePlayer.motionZ + offset[1]);
    }

    private boolean shouldSneak() {
        if (this.directionCheck.getValue() && mc.gameSettings.keyBindForward.isKeyDown()) {
            return false;
        } else if (this.pitchCheck.getValue() && mc.thePlayer.rotationPitch < 69.0F) {
            return false;
        } else if(sneakOnly.getValue() && !mc.gameSettings.keyBindSneak.isKeyDown()){
            return false;
        } else {
            return (!this.blocksOnly.getValue() || ItemUtil.isHoldingBlock()) && mc.thePlayer.onGround;
        }
    }

    public Eagle() {
        super("Eagle", false);
    }

    @EventTarget(Priority.LOWEST)
    public void onTick(TickEvent event) {
        if (this.isEnabled() && event.getType() == EventType.PRE) {
            if (this.sneakDelay > 0) {
                this.sneakDelay--;
            }
            if (this.sneakDelay == 0 && this.canMoveSafely()) {
                this.sneakDelay = RandomUtils.nextInt(this.minDelay.getValue(), this.maxDelay.getValue() + 1);
            }
        }
    }

    @EventTarget(Priority.LOWEST)
    public void onMoveInput(MoveInputEvent event) {
        if (this.isEnabled() && mc.currentScreen == null && shouldSneak()) {
            if(sneakOnly.getValue() && mc.thePlayer.movementInput.sneak){
                if (!mc.thePlayer.movementInput.jump) {
                    mc.thePlayer.movementInput.sneak = false;
                    mc.thePlayer.movementInput.moveForward /= 0.3F;
                    mc.thePlayer.movementInput.moveStrafe /= 0.3F;
                }
            }

            if(!mc.thePlayer.movementInput.sneak && (this.sneakDelay > 0 || this.canMoveSafely())) {
                mc.thePlayer.movementInput.sneak = true;
                mc.thePlayer.movementInput.moveStrafe *= 0.3F;
                mc.thePlayer.movementInput.moveForward *= 0.3F;
                this.sneakOffset = 1.0f + RandomUtil.nextFloat(this.minOffset, this.maxOffset);
            }
        }
    }

    @Override
    public void onDisabled() {
        this.sneakDelay = 0;
    }

    @Override
    public void onEnabled() {
        this.sneakOffset = 1.0f + RandomUtil.nextFloat(this.minOffset, this.maxOffset);
    }

    @Override
    public void verifyValue(String name) {
        if (this.minDelay.getName().equals(name)) {
            if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                this.maxDelay.setValue(this.minDelay.getValue());
            }

        } else if (this.maxDelay.getName().equals(name)) {
            if (this.minDelay.getValue() > this.maxDelay.getValue()) {
                this.minDelay.setValue(this.maxDelay.getValue());
            }

        } else if (this.minOffset.getName().equals(name)) {
            if (this.minOffset.getValue() > this.maxOffset.getValue()) {
                this.maxOffset.setValue(this.minOffset.getValue());
            }

        } else if (this.maxOffset.getName().equals(name)) {
            if (this.minOffset.getValue() > this.maxOffset.getValue()) {
                this.minOffset.setValue(this.maxOffset.getValue());
            }
        }
    }

    @Override
    public String[] getSuffix() {
        return Objects.equals(this.minDelay.getValue(), this.maxDelay.getValue())
                ? new String[]{this.minDelay.getValue().toString()}
                : new String[]{String.format("%d-%d", this.minDelay.getValue(), this.maxDelay.getValue())};
    }
}
