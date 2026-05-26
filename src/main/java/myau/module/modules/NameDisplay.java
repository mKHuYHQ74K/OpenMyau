package myau.module.modules;

import myau.event.EventTarget;
import myau.events.Render3DEvent;
import myau.mixin.IAccessorRenderManager;
import myau.module.Module;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import myau.property.properties.BooleanProperty;
import myau.property.properties.FloatProperty;
import myau.property.properties.PercentProperty;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import org.apache.commons.lang3.StringUtils;

public class NameDisplay extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final FloatProperty scale = new FloatProperty("scale", 1.0F, 0.5F, 2.0F);
    public final BooleanProperty autoScale = new BooleanProperty("auto-scale", true);
    public final PercentProperty backgroundOpacity = new PercentProperty("background", 25);
    public final BooleanProperty shadow = new BooleanProperty("shadow", true);
    public final BooleanProperty players = new BooleanProperty("players", true);
    public final BooleanProperty friends = new BooleanProperty("friends", true);
    public final BooleanProperty enemies = new BooleanProperty("enemies", true);
    public final BooleanProperty bots = new BooleanProperty("bots", false);
    public final BooleanProperty tabName = new BooleanProperty("tab-name", false);

    public NameDisplay() {
        super("NameDisplay", false);
    }

    public boolean shouldRender(EntityPlayer entityPlayer) {
        if (entityPlayer.deathTime > 0) return false;
        if (mc.getRenderViewEntity().getDistanceToEntity(entityPlayer) > 512.0F) return false;
        if (entityPlayer != mc.thePlayer && entityPlayer != mc.getRenderViewEntity()) {
            if (TeamUtil.isBot(entityPlayer)) return this.bots.getValue();
            if (TeamUtil.isFriend(entityPlayer)) return this.friends.getValue();
            if (TeamUtil.isTarget(entityPlayer)) return this.enemies.getValue();
            return this.players.getValue();
        }
        return false;
    }

    @EventTarget
    public void onRender(Render3DEvent event) {
        if (!this.isEnabled()) return;

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (!shouldRender(player)) continue;
            if (!player.ignoreFrustumCheck && !RenderUtil.isInViewFrustum(player.getEntityBoundingBox(), 10.0)) continue;

            String displayName;
            if (this.tabName.getValue()) {
                net.minecraft.client.network.NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(player.getUniqueID());
                if (info != null && info.getDisplayName() != null) {
                    displayName = info.getDisplayName().getFormattedText();
                } else {
                    displayName = TeamUtil.stripName(player);
                }
            } else {
                displayName = TeamUtil.stripName(player);
            }
            if (StringUtils.isBlank(EnumChatFormatting.getTextWithoutFormattingCodes(displayName))) continue;

            double x = RenderUtil.lerpDouble(player.posX, player.lastTickPosX, event.getPartialTicks())
                    - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
            double y = RenderUtil.lerpDouble(player.posY, player.lastTickPosY, event.getPartialTicks())
                    - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY()
                    + player.getEyeHeight();
            double z = RenderUtil.lerpDouble(player.posZ, player.lastTickPosZ, event.getPartialTicks())
                    - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();

            double distance = mc.getRenderViewEntity().getDistanceToEntity(player);

            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y + 0.9, z);
            GlStateManager.rotate(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
            float view = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
            GlStateManager.rotate(mc.getRenderManager().playerViewX, view, 0.0F, 0.0F);
            double billboardScale = Math.pow(Math.min(Math.max(this.autoScale.getValue() ? distance : 0.0, 6.0), 128.0), 0.75) * 0.0075;
            GlStateManager.scale(-billboardScale * this.scale.getValue(), -billboardScale * this.scale.getValue(), 1.0);

            int width = mc.fontRendererObj.getStringWidth(displayName);

            if (this.backgroundOpacity.getValue() > 0) {
                int bgAlpha = (int) (this.backgroundOpacity.getValue() / 100.0F * 255.0F);
                RenderUtil.enableRenderState();
                RenderUtil.drawRect(
                        (float) (-width) / 2.0F - 1.0F,
                        (float) (-mc.fontRendererObj.FONT_HEIGHT) - 1.0F,
                        (float) width / 2.0F + 1.0F,
                        0.0F,
                        (bgAlpha << 24)
                );
                RenderUtil.disableRenderState();
            }

            GlStateManager.disableDepth();
            mc.fontRendererObj.drawString(
                    displayName,
                    (float) (-width) / 2.0F,
                    (float) (-mc.fontRendererObj.FONT_HEIGHT),
                    -1,
                    this.shadow.getValue()
            );
            GlStateManager.enableDepth();

            GlStateManager.popMatrix();
        }
    }
}
