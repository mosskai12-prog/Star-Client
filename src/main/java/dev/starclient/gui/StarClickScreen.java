package dev.starclient.gui;

import dev.starclient.module.Module;
import dev.starclient.module.ModuleManager;
import dev.starclient.module.modules.BlockFinderModule;
import dev.starclient.module.modules.StorageEspModule;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Star Client's lightweight click GUI.
 *
 * The screen is drawn directly with DrawContext: no per-frame widget creation,
 * blur passes, textures, or external GUI dependencies.
 */
public final class StarClickScreen extends Screen {
    private static final int BACKGROUND = 0xFF080F1C;
    private static final int PANEL = 0xFF101B2C;
    private static final int PANEL_ALT = 0xFF142238;
    private static final int CARD = 0xFF172840;
    private static final int CARD_HOVER = 0xFF1D3452;
    private static final int CARD_SELECTED = 0xFF1C3B60;
    private static final int ACCENT = 0xFF40A0FF;
    private static final int ACCENT_BRIGHT = 0xFF70C6FF;
    private static final int TEXT = 0xFFF2F7FF;
    private static final int MUTED = 0xFF8FA6C3;
    private static final int GREEN = 0xFF45D6A0;
    private static final int RED = 0xFFFF6B78;

    private static final List<String> CATEGORIES =
        List.of("All", "Base Hunting", "Combat", "Movement", "Render", "Misc");

    private String selectedCategory = "All";
    private String search = "";
    private boolean searchFocused;
    private Module selectedModule;
    private List<Module> visibleModules = List.of();

    private float scrollPosition;
    private float targetScrollPosition;
    private float openProgress;

    public StarClickScreen() {
        super(Text.literal("Star Client"));
        selectedModule = ModuleManager.all().stream().findFirst().orElse(null);
        rebuildVisibleModules();
    }

    @Override
    protected void init() {
        super.init();
        clampScroll();
    }

    @Override
    public void tick() {
        super.tick();
        // A short ease-in instead of an expensive blur or shader animation.
        openProgress = Math.min(1.0F, openProgress + 0.22F);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        Layout ui = layout();
        int x = ui.x();
        int y = ui.y();
        int w = ui.panelWidth();
        int h = ui.panelHeight();

        // Clean dark backdrop with inexpensive gradients and subtle accent lights.
        context.fillGradient(0, 0, width, height, 0xE9081020, 0xF4080C17);
        context.fillGradient(0, 0, width, Math.max(1, height / 3), 0x183C8FFF, 0x00081020);

        // Main shell.
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF294D78);
        context.fill(x, y, x + w, y + h, BACKGROUND);
        context.fillGradient(x, y, x + w, y + 3, 0xFF47A7FF, 0xFF776BFF);
        context.fill(x, y + 3, x + w, y + 66, PANEL);
        context.fill(x + 1, y + 65, x + w - 1, y + 66, 0xFF263B56);

        // Brand/header.
        context.fillGradient(x + 18, y + 16, x + 49, y + 47, 0xFF317FE8, 0xFF735CFA);
        context.drawCenteredTextWithShadow(textRenderer, "S", x + 33, y + 25, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "STAR CLIENT", x + 60, y + 17, TEXT);
        context.drawTextWithShadow(textRenderer, "INDEPENDENT CLIENT  /  1.21.11",
            x + 60, y + 36, MUTED);

        int enabledCount = 0;
        int totalCount = ModuleManager.all().size();
        for (Module module : ModuleManager.all()) {
            if (module.isEnabled()) enabledCount++;
        }
        String status = enabledCount + " / " + totalCount + " ACTIVE";
        int statusWidth = textRenderer.getWidth(status);
        int statusX = x + w - statusWidth - 54;
        context.fill(statusX - 12, y + 21, x + w - 20, y + 45, 0xFF1B3049);
        context.fill(statusX - 3, y + 30, statusX + 3, y + 36,
            enabledCount > 0 ? GREEN : MUTED);
        context.drawTextWithShadow(textRenderer, status, statusX + 9, y + 29, 0xFFBBD8F8);

        // Sidebar.
        int sidebarRight = x + ui.sidebarWidth();
        context.fill(x + 10, y + 77, sidebarRight, y + h - 38, PANEL);
        context.drawTextWithShadow(textRenderer, "WORKSPACE", x + 22, y + 91, MUTED);
        int categoryY = y + 111;
        for (String category : CATEGORIES) {
            boolean active = selectedCategory.equals(category);
            int itemX = x + 16;
            int itemW = ui.sidebarWidth() - 24;
            boolean hovered = inside(mouseX, mouseY, itemX, categoryY, itemW, 31);
            if (active || hovered) {
                context.fill(itemX, categoryY, itemX + itemW, categoryY + 31,
                    active ? 0xFF1B3A60 : 0xFF192B42);
            }
            if (active) {
                context.fill(itemX, categoryY + 6, itemX + 3, categoryY + 25, ACCENT);
            }
            context.drawTextWithShadow(textRenderer, category, itemX + 12,
                categoryY + 11, active ? TEXT : 0xFFB9C9DE);
            String count = Integer.toString(countInCategory(category));
            int countWidth = textRenderer.getWidth(count);
            context.drawTextWithShadow(textRenderer, count, itemX + itemW - countWidth - 10,
                categoryY + 11, active ? ACCENT_BRIGHT : 0xFF7188A5);
            categoryY += 37;
        }

        // Main module browser.
        context.fill(ui.mainX(), y + 77, ui.mainX() + ui.mainWidth(), y + h - 38, PANEL);
        drawSearch(context, ui, y);
        context.drawTextWithShadow(textRenderer,
            selectedCategory.equals("All") ? "ALL MODULES" : selectedCategory.toUpperCase(Locale.ROOT),
            ui.mainX() + 13, y + 122, TEXT);
        context.drawTextWithShadow(textRenderer, visibleModules.size() + " modules",
            ui.mainX() + 13, y + 140, MUTED);

        int listTop = ui.listTop();
        int listBottom = ui.listBottom();
        int viewportHeight = Math.max(0, listBottom - listTop);
        int maxScroll = maxScroll(viewportHeight);
        targetScrollPosition = clamp(targetScrollPosition, 0, maxScroll);
        scrollPosition += (targetScrollPosition - scrollPosition) * 0.32F;
        if (Math.abs(targetScrollPosition - scrollPosition) < 0.35F) {
            scrollPosition = targetScrollPosition;
        }

        context.enableScissor(ui.mainX() + 8, listTop, ui.mainX() + ui.mainWidth() - 8, listBottom);
        if (visibleModules.isEmpty()) {
            context.drawTextWithShadow(textRenderer, "No matching modules",
                ui.mainX() + 16, listTop + 12, TEXT);
            context.drawTextWithShadow(textRenderer, "Try another search or category.",
                ui.mainX() + 16, listTop + 31, MUTED);
        } else {
            for (int i = 0; i < visibleModules.size(); i++) {
                Module module = visibleModules.get(i);
                int cardY = listTop + 4 + i * 60 - Math.round(scrollPosition);
                if (cardY + 53 < listTop || cardY > listBottom) continue;
                drawModuleCard(context, ui, module, cardY, mouseX, mouseY);
            }
        }
        context.disableScissor();
        drawScrollbar(context, ui, listTop, listBottom, maxScroll);

        // Settings inspector.
        if (ui.settingsWidth() > 0) {
            drawInspector(context, ui, y, mouseX, mouseY);
        }

        // Footer and key hints.
        context.fill(x + 12, y + h - 34, x + w - 12, y + h - 33, 0xFF20344D);
        context.drawTextWithShadow(textRenderer, "RIGHT SHIFT  CLOSE",
            x + 20, y + h - 22, 0xFF7188A5);
        context.drawTextWithShadow(textRenderer, "CLICK CARD  SELECT    /    SWITCH  TOGGLE    /    SCROLL  BROWSE",
            x + w - 340, y + h - 22, 0xFF7188A5);

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void drawSearch(DrawContext context, Layout ui, int panelTop) {
        int x = ui.mainX() + 10;
        int y = panelTop + 79;
        int w = ui.mainWidth() - 20;
        context.fill(x, y, x + w, y + 32, searchFocused ? 0xFF1B3049 : 0xFF15243A);
        context.drawStrokedRectangle(x, y, w, 32, searchFocused ? ACCENT : 0xFF293D58);
        context.drawTextWithShadow(textRenderer, "SEARCH", x + 10, y + 11, ACCENT_BRIGHT);
        String shown = search.isEmpty() ? "Type a module name..." : trim(search, Math.max(30, w - 94));
        int textX = x + 62;
        context.drawTextWithShadow(textRenderer, shown, textX, y + 11,
            search.isEmpty() ? MUTED : TEXT);
        if (searchFocused && (System.currentTimeMillis() / 500L) % 2L == 0L) {
            int caretX = textX + textRenderer.getWidth(search.isEmpty() ? "" : trim(search, Math.max(30, w - 94)));
            context.fill(caretX + 1, y + 9, caretX + 2, y + 23, ACCENT_BRIGHT);
        }
    }

    private void drawModuleCard(DrawContext context, Layout ui, Module module,
                                int y, int mouseX, int mouseY) {
        int x = ui.mainX() + 10;
        int w = ui.mainWidth() - 20;
        boolean hovered = inside(mouseX, mouseY, x, y, w, 53);
        boolean selected = module == selectedModule;
        int fill = selected ? CARD_SELECTED : (hovered ? CARD_HOVER : CARD);
        context.fill(x, y, x + w, y + 53, fill);
        context.drawStrokedRectangle(x, y, w, 53, selected ? 0xFF397FC4 : 0xFF20364F);
        if (selected) context.fill(x, y + 8, x + 3, y + 45, ACCENT);

        int iconX = x + 11;
        context.fillGradient(iconX, y + 11, iconX + 31, y + 42,
            module.isEnabled() ? 0xFF2464A7 : 0xFF26384E,
            module.isEnabled() ? 0xFF3152A0 : 0xFF1B2A3D);
        String initial = module.getName().isEmpty() ? "?" : module.getName().substring(0, 1).toUpperCase(Locale.ROOT);
        context.drawCenteredTextWithShadow(textRenderer, initial, iconX + 16, y + 21,
            module.isEnabled() ? 0xFFD9EEFF : MUTED);

        int textX = x + 51;
        int textWidth = Math.max(50, w - 116);
        context.drawTextWithShadow(textRenderer, trim(module.getName(), textWidth),
            textX, y + 10, TEXT);
        context.drawTextWithShadow(textRenderer, trim(module.getDescription(), textWidth),
            textX, y + 29, MUTED);

        int switchX = x + w - 43;
        drawSwitch(context, switchX, y + 18, module.isEnabled());
    }

    private void drawInspector(DrawContext context, Layout ui, int panelTop, int mouseX, int mouseY) {
        int x = ui.settingsX();
        int w = ui.settingsWidth();
        int y = panelTop + 77;
        int bottom = panelTop + ui.panelHeight() - 38;

        context.fill(x, y, x + w, bottom, PANEL);
        context.drawTextWithShadow(textRenderer, "INSPECTOR", x + 14, y + 14, MUTED);
        if (selectedModule == null) {
            context.drawTextWithShadow(textRenderer, "Choose a module",
                x + 14, y + 47, TEXT);
            context.drawTextWithShadow(textRenderer, "Its settings will appear here.",
                x + 14, y + 67, MUTED);
            return;
        }

        int contentY = y + 43;
        context.drawTextWithShadow(textRenderer, trim(selectedModule.getName(), w - 28),
            x + 14, contentY, TEXT);
        String state = selectedModule.isEnabled() ? "ENABLED" : "DISABLED";
        int stateWidth = textRenderer.getWidth(state);
        int stateX = x + 14;
        context.fill(stateX, contentY + 22, stateX + stateWidth + 18, contentY + 42,
            selectedModule.isEnabled() ? 0xFF174B43 : 0xFF3A2B38);
        context.drawTextWithShadow(textRenderer, state,
            stateX + 9, contentY + 28, selectedModule.isEnabled() ? GREEN : 0xFFFFA0A8);

        String desc = trim(selectedModule.getDescription(), w - 28);
        context.drawTextWithShadow(textRenderer, desc, x + 14, contentY + 55, MUTED);

        int toggleY = contentY + 84;
        context.drawTextWithShadow(textRenderer, "Module state", x + 14, toggleY + 5, TEXT);
        drawSwitch(context, x + w - 48, toggleY, selectedModule.isEnabled());
        context.fill(x + 12, toggleY + 34, x + w - 12, toggleY + 35, 0xFF263B55);

        int sectionY = toggleY + 52;
        context.drawTextWithShadow(textRenderer, "CONFIGURATION", x + 14, sectionY, MUTED);
        int rangeY = sectionY + 23;

        if (selectedModule instanceof StorageEspModule storage) {
            drawRangeRow(context, x, w, rangeY, storage.range(), 4, 96);
            int rowY = rangeY + 50;
            rowY = drawSettingToggle(context, x, w, rowY, "Chests", storage.chests());
            rowY = drawSettingToggle(context, x, w, rowY, "Trapped chests", storage.trappedChests());
            rowY = drawSettingToggle(context, x, w, rowY, "Barrels", storage.barrels());
            drawSettingToggle(context, x, w, rowY, "Shulker boxes", storage.shulkers());
        } else if (selectedModule instanceof BlockFinderModule finder) {
            drawRangeRow(context, x, w, rangeY, finder.range(), 8, 64);
            context.drawTextWithShadow(textRenderer, "FOUND  " + finder.targets().size(),
                x + 14, rangeY + 54, ACCENT_BRIGHT);
            context.drawTextWithShadow(textRenderer, "Searches nearby blocks every few ticks.",
                x + 14, rangeY + 75, MUTED);
            context.drawTextWithShadow(textRenderer, "Outlines show through terrain.",
                x + 14, rangeY + 92, MUTED);
        } else {
            context.drawTextWithShadow(textRenderer, "No extra options for this module yet.",
                x + 14, rangeY + 4, MUTED);
        }
    }

    private void drawRangeRow(DrawContext context, int x, int width, int y, int range,
                              int min, int max) {
        context.drawTextWithShadow(textRenderer, "Search range", x + 14, y + 8, TEXT);
        context.drawTextWithShadow(textRenderer, range + " blocks", x + 14, y + 27, ACCENT_BRIGHT);
        drawSmallButton(context, x + width - 76, y + 11, "−");
        drawSmallButton(context, x + width - 42, y + 11, "+");
    }

    private int drawSettingToggle(DrawContext context, int x, int width, int y,
                                  String label, boolean enabled) {
        context.drawTextWithShadow(textRenderer, label, x + 14, y + 9, 0xFFD7E4F5);
        drawSwitch(context, x + width - 48, y + 4, enabled);
        return y + 31;
    }

    private void drawSmallButton(DrawContext context, int x, int y, String label) {
        context.fill(x, y, x + 27, y + 23, 0xFF213B59);
        context.drawStrokedRectangle(x, y, 27, 23, 0xFF345577);
        context.drawCenteredTextWithShadow(textRenderer, label, x + 13, y + 7, TEXT);
    }

    private void drawSwitch(DrawContext context, int x, int y, boolean enabled) {
        context.fill(x, y, x + 34, y + 18, enabled ? 0xFF217DC7 : 0xFF3A4B61);
        context.fill(x + (enabled ? 18 : 3), y + 3,
            x + (enabled ? 29 : 14), y + 15, 0xFFFFFFFF);
    }

    private void drawScrollbar(DrawContext context, Layout ui, int top, int bottom, int maxScroll) {
        if (maxScroll <= 0) return;
        int trackX = ui.mainX() + ui.mainWidth() - 5;
        int trackHeight = bottom - top;
        int contentHeight = visibleModules.size() * 60;
        int thumbHeight = Math.max(22, trackHeight * trackHeight / Math.max(1, contentHeight));
        int travel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = top + Math.round(travel * (scrollPosition / maxScroll));
        context.fill(trackX, top, trackX + 2, bottom, 0xFF1B2C41);
        context.fill(trackX, thumbY, trackX + 2, thumbY + thumbHeight, ACCENT);
    }

    private void rebuildVisibleModules() {
        String query = search.toLowerCase(Locale.ROOT).trim();
        List<Module> result = new ArrayList<>();
        for (Module module : ModuleManager.all()) {
            if (!selectedCategory.equals("All") && !module.getCategory().equals(selectedCategory)) {
                continue;
            }
            if (!query.isEmpty()
                && !module.getName().toLowerCase(Locale.ROOT).contains(query)
                && !module.getDescription().toLowerCase(Locale.ROOT).contains(query)) {
                continue;
            }
            result.add(module);
        }
        visibleModules = List.copyOf(result);
        targetScrollPosition = 0;
        scrollPosition = 0;
        if (selectedModule != null && !ModuleManager.all().contains(selectedModule)) {
            selectedModule = visibleModules.stream().findFirst().orElse(null);
        }
    }

    private int countInCategory(String category) {
        if (category.equals("All")) return ModuleManager.all().size();
        int count = 0;
        for (Module module : ModuleManager.all()) {
            if (module.getCategory().equals(category)) count++;
        }
        return count;
    }

    private void clampScroll() {
        targetScrollPosition = clamp(targetScrollPosition, 0, maxScroll(layout().listBottom() - layout().listTop()));
        scrollPosition = clamp(scrollPosition, 0, maxScroll(layout().listBottom() - layout().listTop()));
    }

    private int maxScroll(int viewportHeight) {
        return Math.max(0, visibleModules.size() * 60 - Math.max(0, viewportHeight - 8));
    }

    private Layout layout() {
        int panelWidth = Math.min(1040, Math.max(0, width - 32));
        int panelHeight = Math.min(640, Math.max(0, height - 32));
        int x = (width - panelWidth) / 2;
        int slide = Math.round((1.0F - openProgress) * 12.0F);
        int y = (height - panelHeight) / 2 + slide;

        int sidebarWidth = Math.min(168, Math.max(128, panelWidth / 6));
        int mainX = x + sidebarWidth + 22;
        int right = x + panelWidth - 16;
        int available = Math.max(0, right - mainX);
        int settingsWidth = available >= 520
            ? Math.min(300, Math.max(230, Math.round(available * 0.37F)))
            : 0;
        int gap = settingsWidth > 0 ? 14 : 0;
        int mainWidth = Math.max(190, available - settingsWidth - gap);
        int listTop = y + 157;
        int listBottom = y + panelHeight - 48;
        return new Layout(x, y, panelWidth, panelHeight, sidebarWidth,
            mainX, mainWidth, mainX + mainWidth + gap, settingsWidth, listTop, listBottom);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        Layout ui = layout();
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.buttonInfo().button();

        int categoryY = ui.y() + 111;
        for (String category : CATEGORIES) {
            if (inside(mouseX, mouseY, ui.x() + 16, categoryY, ui.sidebarWidth() - 24, 31)) {
                selectedCategory = category;
                searchFocused = false;
                rebuildVisibleModules();
                return true;
            }
            categoryY += 37;
        }

        int searchX = ui.mainX() + 10;
        int searchY = ui.y() + 79;
        if (inside(mouseX, mouseY, searchX, searchY, ui.mainWidth() - 20, 32)) {
            searchFocused = true;
            return true;
        }
        searchFocused = false;

        int listTop = ui.listTop();
        int listBottom = ui.listBottom();
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT
            && inside(mouseX, mouseY, ui.mainX() + 8, listTop, ui.mainWidth() - 16, listBottom - listTop)) {
            for (int i = 0; i < visibleModules.size(); i++) {
                Module module = visibleModules.get(i);
                int cardY = listTop + 4 + i * 60 - Math.round(scrollPosition);
                int cardX = ui.mainX() + 10;
                int cardW = ui.mainWidth() - 20;
                if (!inside(mouseX, mouseY, cardX, cardY, cardW, 53)) continue;
                selectedModule = module;
                int switchX = cardX + cardW - 43;
                if (inside(mouseX, mouseY, switchX - 4, cardY + 12, 42, 30)) {
                    module.toggle();
                }
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT
            && inside(mouseX, mouseY, ui.mainX(), listTop, ui.mainWidth(), listBottom - listTop)) {
            for (int i = 0; i < visibleModules.size(); i++) {
                int cardY = listTop + 4 + i * 60 - Math.round(scrollPosition);
                if (inside(mouseX, mouseY, ui.mainX() + 10, cardY, ui.mainWidth() - 20, 53)) {
                    selectedModule = visibleModules.get(i);
                    return true;
                }
            }
        }

        if (selectedModule != null && ui.settingsWidth() > 0
            && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int x = ui.settingsX();
            int w = ui.settingsWidth();
            int contentY = ui.y() + 77 + 43;
            int toggleY = contentY + 84;
            if (inside(mouseX, mouseY, x + w - 56, toggleY - 3, 48, 25)) {
                selectedModule.toggle();
                return true;
            }

            int sectionY = toggleY + 52;
            int rangeY = sectionY + 23;
            if (selectedModule instanceof StorageEspModule storage) {
                if (handleRangeClick(mouseX, mouseY, x, w, rangeY, storage.range(), 4, 96,
                    storage::setRange)) return true;
                int rowY = rangeY + 50;
                if (settingHit(mouseX, mouseY, x, w, rowY)) { storage.setChests(!storage.chests()); return true; }
                rowY += 31;
                if (settingHit(mouseX, mouseY, x, w, rowY)) { storage.setTrappedChests(!storage.trappedChests()); return true; }
                rowY += 31;
                if (settingHit(mouseX, mouseY, x, w, rowY)) { storage.setBarrels(!storage.barrels()); return true; }
                rowY += 31;
                if (settingHit(mouseX, mouseY, x, w, rowY)) { storage.setShulkers(!storage.shulkers()); return true; }
            } else if (selectedModule instanceof BlockFinderModule finder) {
                if (handleRangeClick(mouseX, mouseY, x, w, rangeY, finder.range(), 8, 64,
                    finder::setRange)) return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private boolean handleRangeClick(double mouseX, double mouseY, int x, int width,
                                     int y, int current, int min, int max,
                                     java.util.function.IntConsumer setter) {
        if (inside(mouseX, mouseY, x + width - 80, y + 7, 31, 31)) {
            setter.accept(Math.max(min, current - 4));
            return true;
        }
        if (inside(mouseX, mouseY, x + width - 46, y + 7, 31, 31)) {
            setter.accept(Math.min(max, current + 4));
            return true;
        }
        return false;
    }

    private boolean settingHit(double mouseX, double mouseY, int x, int width, int y) {
        return inside(mouseX, mouseY, x + 7, y, width - 14, 29);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY,
                                 double horizontalAmount, double verticalAmount) {
        Layout ui = layout();
        if (inside(mouseX, mouseY, ui.mainX(), ui.listTop(),
            ui.mainWidth(), ui.listBottom() - ui.listTop())) {
            int max = maxScroll(ui.listBottom() - ui.listTop());
            targetScrollPosition = clamp(targetScrollPosition - (float) (verticalAmount * 54.0),
                0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (searchFocused && input.isValidChar() && search.length() < 48) {
            search += input.asString();
            rebuildVisibleModules();
            return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE || input.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        if (searchFocused && input.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            rebuildVisibleModules();
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ENTER) {
            searchFocused = false;
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String trim(String value, int maxWidth) {
        if (value == null || value.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            String candidate = result.toString() + value.charAt(i);
            if (textRenderer.getWidth(candidate) > Math.max(10, maxWidth - 8)) {
                return result.length() > 2 ? result.substring(0, result.length() - 1) + "..." : result + "...";
            }
            result.append(value.charAt(i));
        }
        return result.toString();
    }

    private record Layout(int x, int y, int panelWidth, int panelHeight,
                          int sidebarWidth, int mainX, int mainWidth,
                          int settingsX, int settingsWidth, int listTop, int listBottom) {}
}
