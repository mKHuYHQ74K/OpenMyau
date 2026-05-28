package myau.mixin;

import myau.Myau;
import myau.module.modules.NameDisplay;
import net.minecraft.client.gui.GuiPlayerTabOverlay;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@SideOnly(Side.CLIENT)
@Mixin(GuiPlayerTabOverlay.class)
public abstract class MixinGuiPlayerTabOverlay {

    @Inject(
            method = "getPlayerName",
            at = @At("RETURN"),
            cancellable = true
    )
    private void onGetPlayerName(NetworkPlayerInfo info, CallbackInfoReturnable<String> cir) {
        if (Myau.tagManager == null || Myau.moduleManager == null) return;

        NameDisplay nd = (NameDisplay) Myau.moduleManager.modules.get(NameDisplay.class);
        if (!nd.isEnabled() || !nd.tag.getValue()) return;

        String name = info.getGameProfile().getName();
        if (Myau.tagManager.isTagged(name)) {
            String color = Myau.tagManager.getColor(name);
            if (color == null) color = "7";
            cir.setReturnValue("§" + color + "* " + cir.getReturnValue());
        }
    }
}
