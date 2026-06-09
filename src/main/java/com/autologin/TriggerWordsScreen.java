package com.autologin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class TriggerWordsScreen extends Screen {

    private static final int HEADER_H = 33;
    private static final int FOOTER_H = 36;

    private final Screen parent;
    private final AutoLoginConfig config;
    final List<String> words;

    private WordListWidget wordList;
    private EditBox addField;

    public TriggerWordsScreen(Screen parent, AutoLoginConfig config) {
        super(Component.translatable("autologin.triggers.title"));
        this.parent = parent;
        this.config = config;
        this.words = new ArrayList<>(config.triggerWords);
    }

    @Override
    protected void init() {
        int listH = height - HEADER_H - FOOTER_H;

        wordList = new WordListWidget(minecraft, width, listH, HEADER_H, 22);
        wordList.refresh();
        addRenderableWidget(wordList);

        int y = height - FOOTER_H + 6;
        addField = new EditBox(font, width / 2 - 140, y, 180, 18, Component.empty());
        addField.setMaxLength(128);
        addField.setHint(Component.translatable("autologin.triggers.add_placeholder"));
        addRenderableWidget(addField);

        addRenderableWidget(Button.builder(Component.translatable("autologin.triggers.add"), btn -> addWord())
            .bounds(width / 2 + 44, y - 1, 48, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("autologin.triggers.done"), btn -> {
            config.triggerWords = new ArrayList<>(words);
            config.save();
            minecraft.setScreen(parent);
        }).bounds(width / 2 + 96, y - 1, 50, 20).build());

        setInitialFocus(addField);
    }

    private void addWord() {
        String word = addField.getValue().trim();
        if (!word.isEmpty() && !words.contains(word)) {
            words.add(word);
            wordList.refresh();
            addField.setValue("");
        }
    }

    void removeWord(String word) {
        words.remove(word);
        wordList.refresh();
    }

    @Override
    public void render(GuiGraphics ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, 0xB0000000);
        super.render(ctx, mx, my, delta);

        ctx.drawCenteredString(font, title, width / 2, 11, 0xFFFFFF);
        ctx.drawCenteredString(font,
            Component.translatable("autologin.triggers.hint"), width / 2, 21, 0x888888);

        ctx.fill(0, HEADER_H - 2, width, HEADER_H - 1, 0x55FFFFFF);
        ctx.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, 0x55FFFFFF);

        if (words.isEmpty()) {
            ctx.drawCenteredString(font,
                Component.translatable("autologin.triggers.empty"), width / 2, height / 2, 0x888888);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && addField.isFocused()) {
            addWord();
            return true;
        }
        if (keyCode == 256) { minecraft.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    // ── Inner list widget ─────────────────────────────────────────────────────

    class WordListWidget extends AbstractSelectionList<WordListWidget.WordEntry> {

        WordListWidget(Minecraft mc, int width, int height, int top, int itemHeight) {
            super(mc, width, height, top, itemHeight);
        }

        @Override
        public void updateNarration(NarrationElementOutput narrationElementOutput) {}

        void refresh() {
            clearEntries();
            for (String w : words) addEntry(new WordEntry(w));
        }

        class WordEntry extends AbstractSelectionList.Entry<WordEntry> {
            private final String word;
            private final Button removeBtn;

            WordEntry(String word) {
                this.word = word;
                this.removeBtn = Button.builder(Component.translatable("autologin.triggers.remove"), b ->
                    TriggerWordsScreen.this.removeWord(word)
                ).bounds(0, 0, 18, 14).build();
            }

            @Override
            public void render(GuiGraphics ctx, int index, int y, boolean hovered, float delta) {
                int x = WordListWidget.this.getRowLeft();
                int w = WordListWidget.this.getRowWidth();

                Minecraft mc = Minecraft.getInstance();
                int mx = (int)(mc.mouseHandler.xpos() / mc.getWindow().getGuiScale());
                int my = (int)(mc.mouseHandler.ypos() / mc.getWindow().getGuiScale());

                if (hovered) ctx.fill(x, y, x + w, y + 22, 0x1AFFFFFF);
                if (index > 0) ctx.fill(x + 4, y, x + w - 4, y + 1, 0x22FFFFFF);

                ctx.drawString(minecraft.font, word, x + 10, y + 5, 0xFFFFFF);

                removeBtn.setX(x + w - 24);
                removeBtn.setY(y + 4);
                removeBtn.render(ctx, mx, my, delta);
            }

            @Override
            public boolean mouseClicked(double mx, double my, int btn) {
                return removeBtn.mouseClicked(mx, my, btn);
            }
        }
    }
}
