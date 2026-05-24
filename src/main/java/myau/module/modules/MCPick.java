package myau.module.modules;

import myau.event.EventTarget;
import myau.events.KeyEvent;
import myau.module.Module;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

public class MCPick extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public MCPick() {
        super("MCPick", true);
    }

    @EventTarget
    public void onKey(KeyEvent event) {
        if (!this.isEnabled() || event.getKey() != -98) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (mc.playerController == null) return;

        // Don't interfere with creative middle-click (which already picks blocks)
        if (mc.thePlayer.capabilities.isCreativeMode) return;

        if (mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return;

        Block targetBlock = mc.theWorld.getBlockState(mc.objectMouseOver.getBlockPos()).getBlock();
        if (targetBlock == null) return;

        int currentItem = mc.thePlayer.inventory.currentItem;

        // Already holding the matching block
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held != null && held.getItem() instanceof ItemBlock && ((ItemBlock) held.getItem()).getBlock() == targetBlock) {
            return;
        }

        // Search hotbar first (slots 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock() == targetBlock) {
                mc.thePlayer.inventory.currentItem = i;
                return;
            }
        }

        // Search main inventory (slots 9-35), swap with current hotbar slot
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.thePlayer.inventory.getStackInSlot(i);
            if (stack != null && stack.getItem() instanceof ItemBlock && ((ItemBlock) stack.getItem()).getBlock() == targetBlock) {
                mc.playerController.windowClick(0, i, currentItem, 2, mc.thePlayer);
                return;
            }
        }
    }
}
