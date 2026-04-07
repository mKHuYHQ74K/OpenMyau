package myau.module.modules;

import myau.Myau;
import myau.enums.ChatColors;
import myau.event.EventTarget;
import myau.event.types.Priority;
import myau.events.Render2DEvent;
import myau.module.Module;
import myau.property.properties.*;
import myau.util.RenderUtil;
import myau.util.TeamUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.stream.Collectors;

public class Radar extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty colorMode = new ModeProperty("color", 0, new String[]{"DEFAULT", "TEAMS", "HUD"});
    public final IntProperty position = new IntProperty("position", 0, 0, 4);
    public final IntProperty offsetX = new IntProperty("offset-x", 60, 0, 1000, () -> position.getValue() != 4);
    public final IntProperty offsetY = new IntProperty("offset-y", 60, 0, 1000, () -> position.getValue() != 4);
    public final IntProperty radarRadius = new IntProperty("radar-radius", 55, 10, 200);
    public final FloatProperty dotRadius = new FloatProperty("dot-radius", 1.5F, 0.1F, 5.0F);
    public final FloatProperty radarScale = new FloatProperty("radar-scale", 1.0F, 0.1F, 5.0F);
    public final BooleanProperty showPlayers = new BooleanProperty("players", true);
    public final BooleanProperty showFriends = new BooleanProperty("friends", true);
    public final BooleanProperty showEnemies = new BooleanProperty("enemies", true);
    public final BooleanProperty showTeams = new BooleanProperty("teams", true);
    public final BooleanProperty showBots = new BooleanProperty("bots", false);
    public final BooleanProperty showPVP = new BooleanProperty("show-pvp", false);
    public final FloatProperty markRange = new FloatProperty("mark-range", 4.0f, 0.0f, 10.0f);
    public final FloatProperty markScale = new FloatProperty("mark-scale", 1.5f, 0.0f, 2.0f);
    public final ColorProperty fillColor = new ColorProperty("fill-color", Color.GRAY.getRGB(), 0x40);
    public final ColorProperty outlineColor = new ColorProperty("outline-color", Color.DARK_GRAY.getRGB());
    public final ColorProperty crossColor = new ColorProperty("cross-color", Color.LIGHT_GRAY.getRGB(), 0x80);
    public Radar() {
        super("Radar", false);
    }

    private boolean shouldRender(EntityPlayer entityPlayer) {
        if (entityPlayer.deathTime > 0) {
            return false;
        } else if (mc.getRenderViewEntity().getDistanceToEntity(entityPlayer) > 512.0F) {
            return false;
        } else if (entityPlayer != mc.thePlayer && entityPlayer != mc.getRenderViewEntity()) {
            if (TeamUtil.isBot(entityPlayer)) {
                return this.showBots.getValue();
            } else if (TeamUtil.isFriend(entityPlayer)) {
                return this.showFriends.getValue();
            } else if (TeamUtil.isSameTeam(entityPlayer)) {
                return this.showTeams.getValue();
            } else {
                return TeamUtil.isTarget(entityPlayer) ? this.showEnemies.getValue() : this.showPlayers.getValue();
            }
        } else {
            return false;
        }
    }

    private Color getEntityColor(EntityPlayer entityPlayer) {
        if (TeamUtil.isFriend(entityPlayer)) {
            Color color = Myau.friendManager.getColor();
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        } else if (TeamUtil.isTarget(entityPlayer)) {
            Color color = Myau.targetManager.getColor();
            return new Color(color.getRed(), color.getGreen(), color.getBlue(), 255);
        } else {
            switch (this.colorMode.getValue()) {
                case 0:
                    return TeamUtil.getTeamColor(entityPlayer, 1.0F);
                case 1:
                    int teamColor = TeamUtil.isSameTeam(entityPlayer) ? ChatColors.BLUE.toAwtColor() : ChatColors.RED.toAwtColor();
                    return new Color(teamColor | 255 << 24, true);
                case 2:
                    int color = ((HUD) Myau.moduleManager.modules.get(HUD.class)).getColor(System.currentTimeMillis()).getRGB();
                    return new Color(color | 255 << 24, true);
                default:
                    return Color.WHITE;
            }
        }
    }

    public static Color getComplementaryColor(Color color) {
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float newHue = (hsb[0] + 0.5f) % 1.0f;
        Color rgb = Color.getHSBColor(newHue, hsb[1], 1.0f - hsb[2]);
        return new Color(rgb.getRed(), rgb.getGreen(), rgb.getBlue(), color.getAlpha());
    }

    @EventTarget(Priority.LOWEST)
    public void onRender(Render2DEvent event) {
        if (!this.isEnabled()) return;

        ScaledResolution sr = new ScaledResolution(mc);
        HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);

        double centerX, centerY;
        if (position.getValue() == 4) {
            centerX = sr.getScaledWidth() / 2.0F;
            centerY = sr.getScaledHeight() / 2.0F;
        } else {
            centerX = (position.getValue() & 0x1) == 0x1 ? Math.max(sr.getScaledWidth() - offsetX.getValue(), 0) : Math.min(offsetX.getValue(), sr.getScaledWidth());
            centerY = (position.getValue() & 0x2) == 0x2 ? Math.max(sr.getScaledHeight() - offsetY.getValue(), 0) : Math.min(offsetY.getValue(), sr.getScaledHeight());
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0f, 1.0f, 1.0f);
        GlStateManager.translate(centerX, centerY, 0.0f);

        RenderUtil.enableRenderState();

        float yaw = (float)Math.toRadians(mc.thePlayer.rotationYaw);
        if (mc.gameSettings.thirdPersonView != 2) {
            yaw += (float)Math.toRadians(180.0F);
        }
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);

        this.drawRadarCircle(0.0, 0, yaw, radarRadius.getValue(), 64, fillColor.getValue(), outlineColor.getValue(), crossColor.getValue());
        if (this.showPVP.getValue()) {
            double dx = - mc.thePlayer.posX;
            double dz = - mc.thePlayer.posZ;

            double relX = dx * cos + dz * sin;
            double relY = dz * cos - dx * sin;

            double dist = Math.sqrt(relX * relX + relY * relY);
            double scale = dist < radarRadius.getValue() / this.radarScale.getValue() ? 1.0F : radarRadius.getValue() / this.radarScale.getValue() / dist;
            double px = relX * scale * this.radarScale.getValue();
            double py = relY * scale * this.radarScale.getValue();
            GlStateManager.pushMatrix();
            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.translate(px / hud.scale.getValue(), py / hud.scale.getValue(), 0.0f);
            GlStateManager.scale(hud.scale.getValue() / 2.0f, hud.scale.getValue() / 2.0f, 1.0f);
            mc.fontRendererObj.drawString("PVP", -mc.fontRendererObj.getStringWidth("PVP") / 2.0f, -mc.fontRendererObj.FONT_HEIGHT / 2.0f, hud.getColor(System.currentTimeMillis()).getRGB(), hud.shadow.getValue());
            GlStateManager.popMatrix();
        }
        for (EntityPlayer player : TeamUtil.getLoadedEntitiesSorted().stream().filter(entity -> entity instanceof EntityPlayer && this.shouldRender((EntityPlayer) entity)).map(EntityPlayer.class::cast).collect(Collectors.toList())) {
            double dx = (player.lastTickPosX + (player.posX - player.lastTickPosX) * event.getPartialTicks()) - mc.thePlayer.posX;
            double dz = (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.getPartialTicks()) - mc.thePlayer.posZ;

            double relX = dx * cos + dz * sin;
            double relY = dz * cos - dx * sin;

            double dist = Math.sqrt(relX * relX + relY * relY);
            double scale = dist < radarRadius.getValue() / this.radarScale.getValue() ? 1.0F : radarRadius.getValue() / this.radarScale.getValue() / dist;
            double px = relX * scale * this.radarScale.getValue();
            double py = relY * scale * this.radarScale.getValue();

            Color color1 = getEntityColor(player);
            if (dist > this.markRange.getValue()) {
                RenderUtil.fillCircle(px, py, dotRadius.getValue(), 12, color1.getRGB());
            } else {
                Color color2 = getComplementaryColor(color1);
                double mark_scale = this.markScale.getValue();
                if (mark_scale >= 1.0f){
                    RenderUtil.fillCircle(px, py, dotRadius.getValue() * mark_scale, 12, color2.getRGB());
                    RenderUtil.fillCircle(px, py, dotRadius.getValue(), 12, color1.getRGB());
                } else {
                    RenderUtil.fillCircle(px, py, dotRadius.getValue(), 12, color2.getRGB());
                    RenderUtil.fillCircle(px, py, dotRadius.getValue() * mark_scale, 12, color1.getRGB());
                }
            }
        }
        RenderUtil.disableRenderState();
        GlStateManager.popMatrix();
    }

    public void drawRadarCircle(double x, double y, double angle, double radius,
                                       int segments,
                                       int fillColor,
                                       int outlineColor,
                                       int crossColor) {

        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if ((fillColor >>> 24) != 0) {
            RenderUtil.setColor(fillColor);
            GL11.glBegin(GL11.GL_TRIANGLE_FAN);
            GL11.glVertex2d(x, y);
            for (int i = 0; i <= segments; i++) {
                double angle1 = i * (Math.PI * 2 / segments);
                GL11.glVertex2d(
                        x + Math.cos(angle1) * radius,
                        y + Math.sin(angle1) * radius
                );
            }
            GL11.glEnd();
        }

        if ((outlineColor >>> 24) != 0) {
            RenderUtil.setColor(outlineColor);
            GL11.glLineWidth(2f);

            GL11.glBegin(GL11.GL_LINE_LOOP);
            for (int i = 0; i <= segments; i++) {
                double angle1 = i * (Math.PI * 2 / segments);
                GL11.glVertex2d(
                        x + Math.cos(angle1) * radius,
                        y + Math.sin(angle1) * radius
                );
            }
            GL11.glEnd();
        }

        if ((crossColor >>> 24) != 0) {
            RenderUtil.setColor(crossColor);
            GL11.glLineWidth(1.5f);
            GL11.glBegin(GL11.GL_LINES);

            double dx1 = Math.sin(angle);
            double dy1 = Math.cos(angle);

            double dx2 = Math.sin(angle + Math.PI / 2);
            double dy2 = Math.cos(angle + Math.PI / 2);

            GL11.glVertex2d(x - dx1 * radius, y - dy1 * radius);
            GL11.glVertex2d(x + dx1 * radius, y + dy1 * radius);

            GL11.glVertex2d(x - dx2 * radius, y - dy2 * radius);
            GL11.glVertex2d(x + dx2 * radius, y + dy2 * radius);

            GL11.glEnd();

            GlStateManager.disableDepth();
            GlStateManager.enableBlend();
            GlStateManager.enableTexture2D();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            HUD hud = (HUD) Myau.moduleManager.modules.get(HUD.class);
            float hud_scale = hud.scale.getValue();
            GlStateManager.scale(hud_scale, hud_scale, 1.0f);
            int color = hud.getColor(System.currentTimeMillis()).getRGB();
            mc.fontRendererObj.drawString("N",
                    (float) (x - dx1 * (radius / hud_scale + 5)) - mc.fontRendererObj.getStringWidth("N") / 2.0F,
                    (float) (y - dy1 * (radius / hud_scale + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            mc.fontRendererObj.drawString("E",
                    (float) (x + dx2 * (radius / hud_scale + 5)) - mc.fontRendererObj.getStringWidth("E") / 2.0F,
                    (float) (y + dy2 * (radius / hud_scale + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            mc.fontRendererObj.drawString("S",
                    (float) (x + dx1 * (radius / hud_scale + 5)) - mc.fontRendererObj.getStringWidth("S") / 2.0F,
                    (float) (y + dy1 * (radius / hud_scale + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            mc.fontRendererObj.drawString("W",
                    (float) (x - dx2 * (radius / hud_scale + 5)) - mc.fontRendererObj.getStringWidth("W") / 2.0F,
                    (float) (y - dy2 * (radius / hud_scale + 5)) - mc.fontRendererObj.FONT_HEIGHT / 2.0F,
                    color, hud.shadow.getValue());
            GlStateManager.disableTexture2D();
            GlStateManager.disableBlend();
            GlStateManager.enableDepth();
        }

        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
        GlStateManager.resetColor();
    }
}
