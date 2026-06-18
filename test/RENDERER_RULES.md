# Myau 渲染器开发规范

> 任何渲染器模块的开发**必须**阅读并遵守本文档。
> 文档基于上游 `upsteam/develop` 分支的渲染架构整理。

---

## 目录

1. [渲染事件系统](#1-渲染事件系统)
2. [RenderUtil 核心工具](#2-renderutil-核心工具)
3. [GL 状态管理](#3-gl-状态管理)
4. [3D 实体渲染模式](#4-3d-实体渲染模式)
5. [2D 屏幕叠加渲染模式](#5-2d-屏幕叠加渲染模式)
6. [Shader 系统](#6-shader-系统)
7. [颜色处理规范](#7-颜色处理规范)
8. [模块通用模式](#8-模块通用模式)
9. [Filter 实体筛选模式](#9-filter-实体筛选模式)
10. [Mixin 集成点](#10-mixin-集成点)
11. [硬性规则](#11-硬性规则)

---

## 1. 渲染事件系统

共有 **4 个渲染事件**，分别用于不同的渲染时机：

### 1.1 Render2DEvent
```
包路径: myau.events.Render2DEvent
调度点: MixinGuiIngameForge.renderGameOverlay (forge 渲染后)
字段:   float partialTicks
用途:   HUD、屏幕叠加、2D ESP、TargetHUD、Radar、Indicators 等
```
- 在 `GuiIngameForge.renderGameOverlay` 中被调度
- 是 **正交投影** 上下文，适合 2D 屏幕坐标渲染
- `partialTicks` 用于帧间插值

### 1.2 Render3DEvent
```
包路径: myau.events.Render3DEvent
调度点: MixinEntityRenderer.renderWorldPass (世界渲染完成后, 渲染手之前)
字段:   float partialTicks
用途:   3D ESP、Tracers lines、Trajectories、ItemESP、ChestESP、BedESP 等
```
- 在 `EntityRenderer.renderWorldPass` 中被调度
- 是 **透视投影** 上下文，适合世界坐标渲染
- **在渲染手之前**，所以物品栏/手不会遮挡 3D 渲染

### 1.3 RenderLivingEvent
```
包路径: myau.events.RenderLivingEvent
调度点: MixinRendererLivingEntity
字段:   EventType type (PRE / POST), EntityLivingBase entity
用途:   Chams 等需要在实体渲染前后操作 GL 状态的模块
```
- `EventType.PRE` 在实体渲染前调用, `EventType.POST` 在渲染后调用
- 使用 `@EventTarget` 监听，**不是** `@SubscribeEvent`

### 1.4 ResizeEvent
```
包路径: myau.events.ResizeEvent
调度点: 窗口大小变化时
字段:   无
用途:   重建 Framebuffer（如 ESP OUTLINE 模式）
```

### 关键区别

| 事件 | 投影 | 坐标系 | 典型用途 |
|------|------|--------|----------|
| Render2DEvent | 正交 | 屏幕像素 | HUD、2D框、文字、雷达 |
| Render3DEvent | 透视 | 世界坐标 | 3D框、线条、轨迹 |
| RenderLivingEvent | - | - | GL状态拦截（Chams） |

---

## 2. RenderUtil 核心工具

**包路径**: `myau.util.RenderUtil`

### 2D 渲染

| 方法 | 参数 | 说明 |
|------|------|------|
| `drawRect(x1, y1, x2, y2, color)` | float x4, int argb | 填充矩形，需在 `enableRenderState` 内调用 |
| `drawOutlineRect(x1, y1, x2, y2, lineWidth, bgColor, lineColor)` | float x4+lineWidth, int x2 | 带边框矩形（第一行为背景填充） |
| `drawLine(x1, y1, x2, y2, lineWidth, color)` | float x4+lineWidth, int | 2D 线段 |
| `drawArrow(cx, cy, angle, length, lineWidth, color)` | float x5, int | 箭头（两条线段成 90°） |
| `drawTriangle(cx, cy, angle, length, color)` | float x4, int | 填充三角形 |
| `fillCircle(x, y, radius, segments, color)` | double x3, int x2 | 填充圆 |
| `drawCircle(cx, cy, cz, radius, segments, color)` | double x4, int x2 | 3D 圆圈（线框） |
| `drawFramebuffer(framebuffer)` | Framebuffer | 将 Framebuffer 渲染到屏幕 |

### 3D 渲染

| 方法 | 参数 | 说明 |
|------|------|------|
| `drawLine3D(start, endX, endY, endZ, r, g, b, a, lineWidth)` | Vec3 + double x3 + float x5 | 3D 线段，内部调用 setupCameraTransform |
| `drawFilledBox(aabb, r, g, b)` | AxisAlignedBB, int x3 | 填充 3D 盒子（Tessellator） |
| `drawBoundingBox(aabb, r, g, b, a, lineWidth)` | AxisAlignedBB, int x4, float | 3D 线框盒子 |
| `drawEntityBox(entity, r, g, b)` | Entity, int x3 | 实体填充盒（含 lerp 插值） |
| `drawEntityBoundingBox(entity, r, g, b, a, lineWidth, expand)` | Entity, int x4, float x2 | 实体线框盒 |
| `drawBlockBox(pos, height, r, g, b)` | BlockPos, double, int x3 | 方块填充盒 |
| `drawBlockBoundingBox(pos, height, r, g, b, a, lineWidth)` | BlockPos, double, int x4, float | 方块线框盒 |
| `drawCornerESP(entity, r, g, b)` | EntityPlayer, float x3 | 四角 ESP（billboard） |
| `drawFake2DESP(entity, r, g, b)` | EntityPlayer, float x3 | 伪 2D 边框（billboard） |
| `drawEntityCircle(entity, radius, segments, color)` | Entity, double, int x2 | 实体底部圆圈 |
| `drawRect3D(x1, y1, x2, y2, color)` | float x4, int | 3D 空间中的 2D 矩形（深度测试开启时使用） |
| `draw3DRect(x1, y1, x2, y2)` | float x4 | 裸 GL 四边形（用于 corner/fake2d） |

### 辅助方法

| 方法 | 说明 |
|------|------|
| `enableRenderState()` | **启用**混合+禁用纹理2D/剔除/alpha/深度 |
| `disableRenderState()` | **恢复**深度/alpha/剔除/纹理2D+禁用混合 |
| `setColor(argb)` | 从 ARGB int 设置 GL 颜色 |
| `lerpFloat(current, previous, t)` | 线性插值 float |
| `lerpDouble(current, previous, t)` | 线性插值 double |
| `projectToScreen(entity, screenScale)` | 将 3D 实体投影为 2D 屏幕坐标 (Vector4d: x,y min, z,w max) |
| `isInViewFrustum(aabb, expand)` | 视锥体裁剪检测 |
| `drawOutlinedString(text, x, y)` | 带黑色轮廓的文本（4方向偏移+原色） |
| `renderEnchantmentText(stack, x, y, scale)` | 附魔文本渲染 |
| `renderItemInGUI(stack, x, y)` | GUI 物品渲染（含附魔） |
| `renderPotionEffect(effect, x, y)` | 药水效果图标渲染 |

---

## 3. GL 状态管理

### 黄金规则：`enableRenderState` / `disableRenderState` 配对

**`enableRenderState()` 会修改以下状态：**
- `GL_BLEND` → 启用
- `GL_TEXTURE_2D` → 禁用
- `GL_CULL_FACE` → 禁用
- `GL_ALPHA_TEST` → 禁用
- `GL_DEPTH_TEST` → 禁用
- BlendFunc → `GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA`

**`disableRenderState()` 恢复以上所有状态。**

```
// 标准用法
RenderUtil.enableRenderState();
// ... 渲染操作 ...
RenderUtil.disableRenderState();

// 错误用法 - 不可嵌套
RenderUtil.enableRenderState();
RenderUtil.enableRenderState(); // BUG: 状态混乱
```

### 何时不使用 enableRenderState

- **3D 渲染且需要深度测试**（如 ESP 3D box）：用 `enableRenderState()` 然后自己控制部分状态
- **需要纹理**（如 TargetHUD 的头像）：不要完全禁用纹理
- **字体渲染**：直接使用 `mc.fontRendererObj.drawString/drawStringWithShadow`，自己管理深度
- **物品渲染**：使用 `renderItemInGUI`，它内部管理状态

### 3D 线条渲染的 Camera Transform

`drawLine3D` 内部调用 `setupCameraTransform`，这是为了线条在不同视角下正确显示：
```java
// drawLine3D 内部的模式
mc.gameSettings.viewBobbing = false;
entityRenderer.callSetupCameraTransform(partialTicks, 2);
mc.gameSettings.viewBobbing = bl;
// 然后画 GL_LINES
```
**不要在 3D 空间手写 GL 线条**，必须用 `drawLine3D` 以确保视角正确。

### 纹理绑定安全

渲染 3D 后必须恢复纹理状态，否则会导致后续原版渲染纹理错乱：
```java
// ESP OUTLINE 模式的正确做法
this.framebuffer.bindFramebuffer(false);
entityRenderer.callSetupCameraTransform(...);
// ... 渲染实体 ...
mc.getFramebuffer().bindFramebuffer(false);  // 恢复主 framebuffer
```

---

## 4. 3D 实体渲染模式

所有 3D 实体渲染遵循以下模式：

### 4.1 位置插值

使用 `lerpDouble` 进行帧间插值：
```java
double x = RenderUtil.lerpDouble(entity.posX, entity.lastTickPosX, event.getPartialTicks())
        - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
double y = RenderUtil.lerpDouble(entity.posY, entity.lastTickPosY, event.getPartialTicks())
        - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
double z = RenderUtil.lerpDouble(entity.posZ, entity.lastTickPosZ, event.getPartialTicks())
        - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
```
**必须使用 `IAccessorRenderManager.getRenderPosX/Y/Z()`** 偏移到相机空间。

### 4.2 Billboard 文本/物品

用于 NameTags、ItemESP 等的文字标签：
```java
GlStateManager.pushMatrix();
GlStateManager.translate(x, y, z);
GlStateManager.rotate(mc.getRenderManager().playerViewY * -1.0F, 0.0F, 1.0F, 0.0F);
float flip = mc.gameSettings.thirdPersonView == 2 ? -1.0F : 1.0F;
GlStateManager.rotate(mc.getRenderManager().playerViewX, flip, 0.0F, 0.0F);
GlStateManager.scale(-scale, -scale, 1.0);
// ... 渲染文字/物品 ...
GlStateManager.popMatrix();
```

### 4.3 视锥体裁剪

渲染前检查实体是否在视野内：
```java
if (entity.ignoreFrustumCheck || RenderUtil.isInViewFrustum(entity.getEntityBoundingBox(), expand)) {
    // 渲染
}
```

### 4.4 远距离裁剪

```java
if (mc.getRenderViewEntity().getDistanceToEntity(entity) > 512.0F) {
    return; // 跳过
}
```

---

## 5. 2D 屏幕叠加渲染模式

### 5.1 直接 2D 渲染

在 `Render2DEvent` 中使用屏幕坐标：
```java
@EventTarget
public void onRender(Render2DEvent event) {
    if (!this.isEnabled()) return;
    // 使用 RenderUtil.drawRect / drawOutlineRect 等
    // 使用 mc.fontRendererObj.drawString 等
}
```

### 5.2 3D 投影到 2D

使用 `projectToScreen` 获取实体在屏幕上的 AABB：
```java
Vector4d screenPos = RenderUtil.projectToScreen(entity, scaleFactor);
if (screenPos != null) {
    float x = (float) screenPos.x;  // 左
    float y = (float) screenPos.y;  // 上
    float z = (float) screenPos.z;  // 右
    float w = (float) screenPos.w;  // 下
    // 在 (x,y)-(z,w) 范围内绘制 2D 元素
}
```

### 5.3 方向指示器

Indicators / Tracers arrows 使用 `RotationUtil.getYawBetween` + 三角函数定位：
```java
float yawBetween = RotationUtil.getYawBetween(
    lerpDouble(player.posX, player.prevPosX, pt),
    lerpDouble(player.posZ, player.prevPosZ, pt),
    lerpDouble(target.posX, target.prevPosX, pt),
    lerpDouble(target.posZ, target.prevPosZ, pt)
);
float dirX = (float) Math.sin(Math.toRadians(yawBetween));
float dirY = (float) Math.cos(Math.toRadians(yawBetween)) * -1.0F;
// 在屏幕中心偏移 (offset * dirX, offset * dirY) 的位置绘制
```

### 5.4 缩放和位移

HUD 元素使用 `scale` + 透明度/位置自定义：
```java
GlStateManager.pushMatrix();
GlStateManager.scale(scale, scale, 0.0F);
GlStateManager.translate(posX, posY, zLevel);
// ... 渲染 ...
GlStateManager.popMatrix();
```

---

## 6. Shader 系统

### 6.1 基类 `Shader`

```java
public abstract class Shader {
    protected int programId;
    private static final String vertex = "#version 120\n...";
    
    protected Shader(String fragmentSource);  // 编译链接着色器
    public abstract void onLink();            // 注册 uniform locations
    public abstract void onUse();             // 绑定 shader + 设置 uniforms
    public void stop();                       // glUseProgram(0)
}
```

### 6.2 OutlineShader

- 像素级边缘检测，描边半径 2px
- 需要 `framebuffer` 两遍渲染
- `onUse()` 设置 `texture`, `size` (1/displayWidth, 1/displayHeight), `radius` (2.0)

### 6.3 GlowShader

- 简单的颜色替换：保留原纹理 alpha，用指定颜色替换 RGB
- `onUse()` 设置纹理 unit 0
- `W(Color)` 方法设置 uniform color

### 6.4 Shader ESP 模式（OUTLINE）

标准流程：
```java
// 1. 准备
GlStateManager.pushMatrix();
GlStateManager.pushAttrib();
framebuffer.bindFramebuffer(false);
entityRenderer.callSetupCameraTransform(partialTicks, 0);

// 2. 渲染 glow pass（所有实体用单一颜色渲染到 framebuffer）
boolean shadow = mc.gameSettings.entityShadows;
mc.gameSettings.entityShadows = false;
glowShader.use();
for (EntityPlayer player : renderedEntities) {
    glowShader.W(entityColor);
    // 临时处理隐身
    boolean invisible = player.isInvisible();
    player.setInvisible(false);
    mc.getRenderManager().renderEntityStatic(player, partialTicks, true);
    player.setInvisible(invisible);
}
glowShader.stop();

// 3. 恢复状态
mc.gameSettings.entityShadows = shadow;
mc.getFramebuffer().bindFramebuffer(false);  // 切回主 framebuffer

// 4. 渲染 outline pass（对 framebuffer 纹理做边缘检测）
outlineShader.use();
RenderUtil.drawFramebuffer(this.framebuffer);
outlineShader.stop();

// 5. 清理
framebuffer.framebufferClear();
GlStateManager.popAttrib();
GlStateManager.popMatrix();
```

---

## 7. 颜色处理规范

### 7.1 颜色模式枚举

所有需要颜色的模块遵循三种模式（以 ESP/Tracers/Radar 为例）：
```
"color" -> {"DEFAULT", "TEAMS", "HUD"}
```
- **DEFAULT**: 队伍颜色或 `TeamUtil.getTeamColor()`
- **TEAMS**: `isSameTeam → 蓝, otherwise → 红`
- **HUD**: `HUD.getColor(System.currentTimeMillis())`

### 7.2 TeamUtil 颜色方法

```java
TeamUtil.isFriend(entity)    // → friendManager.getColor()
TeamUtil.isTarget(entity)     // → targetManager.getColor()
TeamUtil.isSameTeam(entity)   // → boolean
TeamUtil.getTeamColor(entity, alpha)  // → Color
```

### 7.3 HUD 颜色系统

`HUD.getColor(long time, long offset)` 支持 6 种模式：
- RAINBOW / CHROMA / ASTOLFO / CUSTOM1 / CUSTOM12 / CUSTOM123
- 所有渲染模块的 HUD 颜色模式最终调用此方法

### 7.4 ColorUtil 工具

```java
ColorUtil.fromHSB(hue, sat, bright)       // HSB → Color
ColorUtil.interpolate(progress, a, b)      // 线性插值两个颜色
ColorUtil.getHealthBlend(percent)          // 血量颜色 (红→黄→绿)
ColorUtil.darker(color, factor)            // 变暗
ColorUtil.scale(color, scaleFactor, alpha) // 缩放 RGB
```

### 7.5 颜色属性

```java
// 使用 ColorProperty 让用户在 GUI 中选色
public final ColorProperty customColor = new ColorProperty("custom-color", defaultRGB);
// 透明度用 PercentProperty 独立控制
public final PercentProperty opacity = new PercentProperty("opacity", 25);
```

---

## 8. 模块通用模式

### 8.1 渲染模块结构

```java
public class Xxx extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    
    // 属性
    public final ModeProperty mode = new ModeProperty("mode", 0, new String[]{...});
    public final BooleanProperty players = new BooleanProperty("players", true);
    // ...
    
    public Xxx() {
        super("Xxx", false);
    }
    
    @EventTarget
    public void onRender(RenderXxxEvent event) {
        if (!this.isEnabled()) return;
        // 渲染逻辑
    }
}
```

### 8.2 生命周期

- `Module` 基类不绑定渲染事件，`onEnabled/onDisabled` 可选覆写
- 渲染模块不需要注册/注销事件，`@EventTarget` 由 `EventManager` 统一管理
- 使用 Forge 事件的模块（如 AutoReconnect）才需要 `MinecraftForge.EVENT_BUS.register/unregister`

### 8.3 Entity Filter 模式

所有实体渲染模块使用统一的 filter 模式：
```java
private boolean shouldRender(EntityPlayer entity) {
    if (entity.deathTime > 0) return false;
    if (distance > 512) return false;
    if (entity == mc.thePlayer || entity == mc.getRenderViewEntity()) return false;
    
    if (isBot) return bots.getValue();
    if (isFriend) return friends.getValue();
    if (isTarget) return enemies.getValue();
    return players.getValue();
}
```
对于非玩家实体（NameTags/Chams），额外区分 mobs/bosses/animals/creepers/endermen/blazes。

---

## 9. Filter 实体筛选模式

所有玩家筛选模块**必须**提供以下布尔属性（但不一定全部需要，视模块功能而定）：

### 玩家目标
```java
// 基础
BooleanProperty players   // 普通玩家
BooleanProperty friends   // 好友
BooleanProperty enemies   // 敌人
BooleanProperty bots      // bot

// 可选
BooleanProperty self      // 自己（需要第三人称）
BooleanProperty teams     // 同队
```

### 生物目标（NameTags / Chams 适用）
```java
BooleanProperty mobs      // 普通怪物
BooleanProperty creepers  // 苦力怕
BooleanProperty endermen  // 末影人
BooleanProperty blazes    // 烈焰人
BooleanProperty bosses    // Boss（龙/凋零）
BooleanProperty animals   // 动物
```

### Filter 方法命名规范

- 方法名为 `shouldRender(EntityType entity)`
- 返回 `false` = 跳过此实体
- 先检查 `deathTime > 0` → 跳过死亡
- 再检查 `distance > 512` → 超出范围跳过  
- 最后按类型分发到对应属性

---

## 10. Mixin 集成点

### 10.1 访问器 (Accessor)

| Mixin | 提供 | 用途 |
|-------|------|------|
| `IAccessorRenderManager` | `getRenderPosX/Y/Z()` | 3D 渲染坐标偏移 |
| `IAccessorEntityRenderer` | `callSetupCameraTransform(float, int)` | 3D 线条/OUTLINE ESP |
| `IAccessorMinecraft` | `getTimer().renderPartialTicks` | 帧间插值时间 |

### 10.2 修改器 (Modify/Inject)

| Mixin | 功能 | 模块 |
|-------|------|------|
| `MixinEntityRenderer` | 调度 `Render3DEvent` | 所有 3D 渲染 |
| `MixinGuiIngameForge` | 调度 `Render2DEvent` | 所有 2D 渲染 |
| `MixinRendererLivingEntity` | 调度 `RenderLivingEvent` + 取消原版 NameTag | Chams + NameTags |
| `MixinFontRenderer` | 拦截文字渲染 | NickHider, AntiObfuscate |
| `MixinRenderManager` | 旋转覆盖 | KillAura 等 |

### 10.3 新渲染功能的 Mixin 规则

- 如果只需要**在特定时机画东西**：使用已有事件 `Render2DEvent` / `Render3DEvent`
- 如果需要**拦截原版渲染行为**（如取消标签、修改模型）：新增 Mixin
- 所有 Mixin **必须**有 `@SideOnly(Side.CLIENT)` 和 `priority` 属性
- Mixin 必须是抽象类或接口

---

## 11. 硬性规则

### ✅ 必须遵守

1. **配对状态管理**: `enableRenderState()` 与 `disableRenderState()` 必须成对调用，不可嵌套
2. **3D 坐标偏移**: 所有世界坐标渲染必须减去 `IAccessorRenderManager.getRenderPosX/Y/Z()`
3. **帧间插值**: 所有移动实体的位置必须使用 `lerpDouble(pos, lastTickPos, partialTicks)` 插值
4. **视锥体裁剪**: 3D 渲染必须使用 `isInViewFrustum` 检查可见性
5. **距离裁剪**: 所有实体渲染必须在 `512` 格外裁剪
6. **死亡过滤**: `entity.deathTime > 0` 的实体不渲染
7. **模块注册**: 新模块必须在 `Myau.java` 和 `ClickGui.java` 中同时注册
8. **事件绑定**: 渲染事件使用 `@EventTarget` 注解，Forge 事件使用 `@SubscribeEvent`
9. **Alpha 处理**: 使用 `setColor(int argb)` 而不是手动 `GlStateManager.color`（特殊需求除外）
10. **Builder 构建**: 使用 `Gradle` + Java 21 构建，确保 mixin 在 `myau.mixin` 包下

### ❌ 禁止

1. **禁止在 3D 渲染中使用 `Gui.drawRect`**（它是 2D 屏幕坐标方法）
2. **禁止直接修改 GL 状态后不恢复**（使用 `GlStateManager.push/popMatrix` 或 `enable/disableRenderState`）
3. **禁止在 `Render2DEvent` 中做 3D 渲染**（没有透视投影矩阵）
4. **禁止使用 `GL11.glColor*` 裸函数**（用 `setColor` 或 `GlStateManager.color`）
5. **禁止在渲染循环中创建新对象**（预先分配或在字段中缓存）
6. **禁止省略 `isEnabled()` 检查**（每个 EventTarget 方法第一行必须是 enable 检查）

### ⚠️ 注意

- `drawOutlineRect` 的第一个参数 `backgroundColor` 是**填充色**，第二个 `lineColor` 是**边框色**
- 不要假设 render state 的初始值，始终用 `enableRenderState` 进入受控状态
- 纹理操作后必须恢复 `GL_TEXTURE_2D` 状态（`disableRenderState` 会自动做）
- 所有 z-level 值应为负数（如 `-100`, `-300`, `-450`），正值会被遮挡

---

## 附录：渲染模块速查表

| 模块 | 事件 | 维度 | 渲染内容 |
|------|------|------|----------|
| ESP | Render2DEvent + Render3DEvent | 2D/3D | 实体边框/填充/着色器 |
| Tracers | Render3DEvent + Render2DEvent | 3D+2D | 视线 + 方向箭头 |
| NameTags | Render3DEvent | 3D | Billboard 标签 |
| TargetHUD | Render2DEvent | 2D | 目标信息面板 |
| Radar | Render2DEvent | 2D | 俯视雷达 |
| Indicators | Render2DEvent | 2D | 投掷物方向指示 |
| ItemESP | Render3DEvent | 3D | 掉落物高亮 |
| BedESP | Render3DEvent | 3D | 床 + 黑曜石高亮 |
| ChestESP | Render3DEvent | 3D | 箱子线框 |
| Trajectories | Render3DEvent | 3D | 投掷物轨迹 |
| Chams | RenderLivingEvent | GL | 实体透视 |
| HUD | Render2DEvent | 2D | 模块列表 + 状态信息 |
