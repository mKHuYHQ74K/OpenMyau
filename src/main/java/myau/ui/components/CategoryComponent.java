package myau.ui.components;

import myau.module.Module;
import myau.ui.Component;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CategoryComponent {
    public ArrayList<Component> modulesInCategory = new ArrayList<>();
    public String categoryName;
    private boolean categoryOpened;
    private int width;
    private int y;
    private int x;
    private final int bh;
    public boolean dragging;
    public int xx;
    public int yy;
    public boolean pin = false;
    private double marginY, marginX;
    private int scroll = 0;
    private double animScroll = 0;
    private int height = 0;
    private int maxHeight;

    public CategoryComponent(String category, List<Module> modules) {
        this.categoryName = category;
        this.width = 92;
        this.x = 5;
        this.y = 5;
        this.bh = 13;
        this.xx = 0;
        this.categoryOpened = false;
        this.dragging = false;
        int tY = this.bh + 3;
        this.marginX = 80;
        this.marginY = 4.5;
        for (Module mod : modules) {
            ModuleComponent b = new ModuleComponent(mod, this, tY);
            this.modulesInCategory.add(b);
            tY += 16;
        }
    }

    public ArrayList<Component> getModules() {
        return this.modulesInCategory;
    }

    public void setX(int n) {
        this.x = n;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void mousePressed(boolean d) {
        this.dragging = d;
    }

    public boolean isPin() {
        return this.pin;
    }

    public void setPin(boolean on) {
        this.pin = on;
    }

    public boolean isOpened() {
        return this.categoryOpened;
    }

    public void setOpened(boolean on) {
        this.categoryOpened = on;
    }

    public void render(FontRenderer renderer) {
        ScaledResolution sr = new ScaledResolution(Minecraft.getMinecraft());
        this.maxHeight = sr.getScaledHeight() - 40;
        this.width = 92;
        update();
        height = 0;
        for (Component moduleRenderManager : this.modulesInCategory) {
            height += moduleRenderManager.getHeight();
        }
        int maxScroll = Math.max(0, height - maxHeight);
        if (scroll > maxScroll) scroll = maxScroll;
        if (animScroll > maxScroll) animScroll = maxScroll;
        animScroll += (scroll - animScroll) * 0.2;
        if (!this.modulesInCategory.isEmpty() && this.categoryOpened) {
            int displayHeight = Math.min(height, maxHeight);
            Gui.drawRect(this.x - 1, this.y, this.x + this.width + 1, this.y + this.bh + displayHeight + 4, new Color(0, 0, 0, 100).getRGB());
        }
        Gui.drawRect((this.x - 2), this.y, (this.x + this.width + 2), (this.y + this.bh + 3), new Color(0, 0, 0, 200).getRGB());
        renderer.drawString(this.categoryName, (float) (this.x + 2), (float) (this.y + 4), -1, false);
        renderer.drawString(this.categoryOpened ? "-" : "+", (float) (this.x + marginX), (float) ((double) this.y + marginY), Color.white.getRGB(), false);
        if (this.categoryOpened && !this.modulesInCategory.isEmpty()) {
            int renderHeight = 0;
            double scale = sr.getScaleFactor();
            int contentTop = this.y + this.bh + 3;
            int contentBottom = contentTop + maxHeight - 1;
            GL11.glEnable(GL11.GL_SCISSOR_TEST);
            GL11.glScissor((int) ((this.x + 1) * scale),
                    (int) ((sr.getScaledHeight() - contentBottom) * scale),
                    (int) ((this.width - 2) * scale),
                    (int) ((contentBottom - contentTop) * scale));
            for (Component c2 : this.modulesInCategory) {
                int compHeight = c2.getHeight();
                if (renderHeight + compHeight > animScroll &&
                        renderHeight < animScroll + maxHeight) {
                    int drawY = (int) (renderHeight - animScroll);
                    c2.setComponentStartAt(this.bh + 3 + drawY);
                    c2.draw(new AtomicInteger(0));
                }
                renderHeight += compHeight;
            }
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            if (height > maxHeight) {
                int trackX = this.x + this.width - 3;
                float trackY = contentTop;
                float trackH = contentBottom - contentTop;
                Gui.drawRect(trackX, (int) trackY,
                        this.x + this.width, (int) (trackY + trackH), new Color(0, 0, 0, 100).getRGB());
                float thumbH = Math.max(16, trackH * trackH / height);
                float thumbY = trackY + (float) ((animScroll / height) * (trackH - thumbH));
                Gui.drawRect(trackX, (int) thumbY,
                        this.x + this.width, (int) (thumbY + thumbH), new Color(255, 255, 255, 60).getRGB());
            }
        }
    }

    public void update() {
        int offset = this.bh + 3;
        for (Component component : this.modulesInCategory) {
            component.setComponentStartAt(offset);
            offset += component.getHeight();
        }
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public void handleDrag(int x, int y) {
        if (this.dragging) {
            this.setX(x - this.xx);
            this.setY(y - this.yy);
        }
    }

    public boolean isHovered(int x, int y) {
        return x >= this.x + 92 - 13 && x <= this.x + this.width && (float) y >= (float) this.y + 2.0F && y <= this.y + this.bh + 1;
    }

    public boolean mousePressed(int x, int y) {
        return x >= this.x + 77 && x <= this.x + this.width - 6 && (float) y >= (float) this.y + 2.0F && y <= this.y + this.bh + 1;
    }

    public boolean insideArea(int x, int y) {
        return x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.bh;
    }

    public String getName() {
        return categoryName;
    }

    public void setLocation(int parseInt, int parseInt1) {
        this.x = parseInt;
        this.y = parseInt1;
    }

    public void onScroll(int mouseX, int mouseY, int scrollAmount) {
        if (!categoryOpened || height <= maxHeight) return;
        int contentTop = this.y + this.bh + 3;
        int contentBottom = contentTop + maxHeight;
        if (mouseX >= this.x && mouseX <= this.x + width && mouseY >= contentTop && mouseY <= contentBottom) {
            scroll -= scrollAmount * 12;
            scroll = Math.max(0, Math.min(scroll, height - maxHeight));
        }
    }
}
