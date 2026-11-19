package dev.starclient.gui;

import dev.starclient.module.Module;
import dev.starclient.module.ModuleManager;
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

public final class StarClickScreen extends Screen {
    private static final int NAVY = 0xFF0B1220;
    private static final int PANEL = 0xFF111D30;
    private static final int CARD = 0xFF182943;
    private static final int CARD_HOVER = 0xFF203755;
    private static final int BLUE = 0xFF368BFF;
    private static final int MUTED = 0xFF91A7C4;
    private static final List<String> CATEGORIES =
        List.of("All", "Base Hunting", "Combat", "Movement", "Render", "Misc");

    private String selectedCategory = "All";
    private String search = "";
    private Module selectedModule;
    private int scrollOffset;

    public StarClickScreen() {
        super(Text.literal("Star Client"));
        selectedModule = ModuleManager.all().stream().findFirst().orElse(null);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fillGradient(0, 0, width, height, 0xE6091220, 0xF20A1020);

        int panelWidth = Math.min(940, width - 36);
        int panelHeight = Math.min(570, height - 36);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;

        // Outer frame and subtle blue top accent.
        context.fill(left - 1, top - 1, left + panelWidth + 1, top + panelHeight + 1, 0xFF29466A);
        context.fill(left, top, left + panelWidth, top + panelHeight, NAVY);
        context.fillGradient(left, top, left + panelWidth, top + 3, 0xFF54A5FF, 0xFF6B72FF);

        // Header.
        context.fill(left, top + 3, left + panelWidth, top + 66, PANEL);
        context.drawTextWithShadow(textRenderer, "✦ STAR CLIENT", left + 22, top + 18, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "INDEPENDENT CLIENT  •  1.21.11", left + 23, top + 37, MUTED);

        int enabledCount = (int) ModuleManager.all().stream().filter(Module::isEnabled).count();
        String status = enabledCount + " MODULE" + (enabledCount == 1 ? "" : "S") + " ENABLED";
        int statusWidth = textRenderer.getWidth(status);
        context.fill(left + panelWidth - statusWidth - 42, top + 19,
            left + panelWidth - 20, top + 43, 0xFF1A3555);
        context.drawTextWithShadow(textRenderer, status,
            left + panelWidth - statusWidth - 31, top + 27, 0xFF9FC8FF);

        int navWidth = 164;
        int mainLeft = left + navWidth + 12;
        int mainWidth = Math.max(210, (panelWidth - navWidth - 36) * 5 / 8);
        int settingsLeft = mainLeft + mainWidth + 12;
        int settingsWidth = left + panelWidth - settingsLeft - 14;

        // Sidebar.
        context.fill(left + 10, top + 76, left + navWidth, top + panelHeight - 12, PANEL);
        context.drawTextWithShadow(textRenderer, "CATEGORIES", left + 22, top + 91, MUTED);
        int categoryY = top + 111;
        for (String category : CATEGORIES) {
            boolean active = selectedCategory.equals(category);
            int y = categoryY;
            context.fill(left + 17, y, left + navWidth - 7, y + 29,
                active ? 0xFF1D4776 : (inside(mouseX, mouseY, left + 17, y, navWidth - 24, 29) ? CARD_HOVER : PANEL));
            if (active) context.fill(left + 17, y + 5, left + 20, y + 24, BLUE);
            context.drawTextWithShadow(textRenderer, category, left + 28, y + 10,
                active ? 0xFFFFFFFF : 0xFFB8C8DD);
            categoryY += 35;
        }

        // Search field.
        int searchY = top + 78;
        context.fill(mainLeft, searchY, mainLeft + mainWidth, searchY + 31, 0xFF17263B);
        context.drawTextWithShadow(textRenderer, "⌕", mainLeft + 10, searchY + 9, 0xFF83B8FF);
        String searchText = search.isEmpty() ? "Search modules..." : search;
        context.drawTextWithShadow(textRenderer, searchText, mainLeft + 29, searchY + 10,
            search.isEmpty() ? MUTED : 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, "MODULES", mainLeft, top + 124, MUTED);

        // Module cards.
        int cardY = top + 143;
        int shown = 0;
        for (Module module : filteredModules()) {
            int y = cardY + shown * 57;
            if (y + 50 > top + panelHeight - 18) break;
            boolean hovered = inside(mouseX, mouseY, mainLeft, y, mainWidth, 49);
            boolean selected = module == selectedModule;
            context.fill(mainLeft, y, mainLeft + mainWidth, y + 49,
                selected ? 0xFF203B5D : (hovered ? CARD_HOVER : CARD));
            if (selected) context.fill(mainLeft, y + 7, mainLeft + 3, y + 42, BLUE);
            context.drawTextWithShadow(textRenderer, module.getName(), mainLeft + 12, y + 9, 0xFFFFFFFF);
            String description = trim(module.getDescription(), 42);
            context.drawTextWithShadow(textRenderer, description, mainLeft + 12, y + 27, MUTED);
            int toggleX = mainLeft + mainWidth - 43;
            context.fill(toggleX, y + 15, toggleX + 30, y + 31,
                module.isEnabled() ? 0xFF287FE0 : 0xFF42536B);
            int knobX = module.isEnabled() ? toggleX + 17 : toggleX + 3;
            context.fill(knobX, y + 18, knobX + 10, y + 28, 0xFFFFFFFF);
            shown++;
        }
        if (shown == 0) {
            context.drawTextWithShadow(textRenderer, "No modules found", mainLeft + 12, cardY + 10, MUTED);
        }

        // Settings panel.
        context.fill(settingsLeft, top + 76, settingsLeft + settingsWidth, top + panelHeight - 12, PANEL);
        context.drawTextWithShadow(textRenderer, "MODULE SETTINGS", settingsLeft + 14, top + 91, MUTED);
        if (selectedModule == null) {
            context.drawTextWithShadow(textRenderer, "Select a module", settingsLeft + 14, top + 126, 0xFFFFFFFF);
            context.drawTextWithShadow(textRenderer, "Right-click a module to inspect it.", settingsLeft + 14, top + 145, MUTED);
        } else {
            context.drawTextWithShadow(textRenderer, selectedModule.getName(),
                settingsLeft + 14, top + 122, 0xFFFFFFFF);
            context.drawWrappedTextWithShadow(textRenderer, Text.literal(selectedModule.getDescription()),
                settingsLeft + 14, top + 143, Math.max(100, settingsWidth - 28), MUTED);
            int settingY = top + 197;
            if (selectedModule instanceof StorageEspModule storage) {
                context.drawTextWithShadow(textRenderer, "RANGE", settingsLeft + 14, settingY, MUTED);
                context.drawTextWithShadow(textRenderer, storage.range() + " blocks",
                    settingsLeft + 14, settingY + 20, 0xFFFFFFFF);
                drawButton(context, settingsLeft + settingsWidth - 78, settingY + 13, 26, 22, "−");
                drawButton(context, settingsLeft + settingsWidth - 42, settingY + 13, 26, 22, "+");
                settingY += 60;
                settingY = drawSettingToggle(context, settingsLeft, settingY, settingsWidth, "Chests", storage.chests());
                settingY = drawSettingToggle(context, settingsLeft, settingY, settingsWidth, "Trapped chests", storage.trappedChests());
                settingY = drawSettingToggle(context, settingsLeft, settingY, settingsWidth, "Barrels", storage.barrels());
                drawSettingToggle(context, settingsLeft, settingY, settingsWidth, "Shulker boxes", storage.shulkers());
            } else {
                context.drawTextWithShadow(textRenderer, "No extra settings yet.",
                    settingsLeft + 14, settingY, MUTED);
            }
        }

        context.drawTextWithShadow(textRenderer, "RIGHT SHIFT  •  CLOSE MENU",
            left + 20, top + panelHeight - 27, 0xFF6F86A4);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private List<Module> filteredModules() {
        String query = search.toLowerCase(Locale.ROOT);
        List<Module> result = new ArrayList<>();
        for (Module module : ModuleManager.all()) {
            if (!selectedCategory.equals("All") && !module.getCategory().equals(selectedCategory)) continue;
            if (!query.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(query)
                && !module.getDescription().toLowerCase(Locale.ROOT).contains(query)) continue;
            result.add(module);
        }
        return result;
    }

    private int drawSettingToggle(DrawContext context, int x, int y, int panelWidth, String label, boolean enabled) {
        context.drawTextWithShadow(textRenderer, label, x + 14, y + 8, 0xFFDCE8F7);
        int tx = x + panelWidth - 49;
        context.fill(tx, y + 4, tx + 30, y + 20, enabled ? 0xFF287FE0 : 0xFF42536B);
        context.fill(tx + (enabled ? 17 : 3), y + 7, tx + (enabled ? 27 : 13), y + 17, 0xFFFFFFFF);
        return y + 31;
    }

    private void drawButton(DrawContext context, int x, int y, int w, int h, String label) {
        context.fill(x, y, x + w, y + h, CARD_HOVER);
        context.drawCenteredTextWithShadow(textRenderer, label, x + w / 2, y + 7, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.buttonInfo().button();

        int panelWidth = Math.min(940, width - 36);
        int panelHeight = Math.min(570, height - 36);
        int left = (width - panelWidth) / 2;
        int top = (height - panelHeight) / 2;
        int navWidth = 164;
        int mainLeft = left + navWidth + 12;
        int mainWidth = Math.max(210, (panelWidth - navWidth - 36) * 5 / 8);
        int settingsLeft = mainLeft + mainWidth + 12;
        int settingsWidth = left + panelWidth - settingsLeft - 14;

        int categoryY = top + 111;
        for (String category : CATEGORIES) {
            if (inside(mx, my, left + 17, categoryY, navWidth - 24, 29)) {
                selectedCategory = category;
                return true;
            }
            categoryY += 35;
        }

        if (inside(mx, my, mainLeft, top + 78, mainWidth, 31)) {
            search = "";
            return true;
        }

        int cardY = top + 143;
        int shown = 0;
        for (Module module : filteredModules()) {
            int y = cardY + shown * 57;
            if (y + 50 > top + panelHeight - 18) break;
            if (inside(mx, my, mainLeft, y, mainWidth, 49)) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    selectedModule = module;
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    module.toggle();
                    selectedModule = module;
                }
                return true;
            }
            shown++;
        }

        if (selectedModule instanceof StorageEspModule storage && button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            int settingY = top + 197;
            if (inside(mx, my, settingsLeft + settingsWidth - 78, settingY + 13, 26, 22)) {
                storage.setRange(storage.range() - 4);
                return true;
            }
            if (inside(mx, my, settingsLeft + settingsWidth - 42, settingY + 13, 26, 22)) {
                storage.setRange(storage.range() + 4);
                return true;
            }
            settingY += 60;
            if (inside(mx, my, settingsLeft, settingY, settingsWidth, 27)) { storage.setChests(!storage.chests()); return true; }
            settingY += 31;
            if (inside(mx, my, settingsLeft, settingY, settingsWidth, 27)) { storage.setTrappedChests(!storage.trappedChests()); return true; }
            settingY += 31;
            if (inside(mx, my, settingsLeft, settingY, settingsWidth, 27)) { storage.setBarrels(!storage.barrels()); return true; }
            settingY += 31;
            if (inside(mx, my, settingsLeft, settingY, settingsWidth, 27)) { storage.setShulkers(!storage.shulkers()); return true; }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (input.isValidChar() && search.length() < 32) {
            search += input.asString();
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
        if (input.key() == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String trim(String value, int max) {
        if (value.length() <= max) return value;
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }
}
