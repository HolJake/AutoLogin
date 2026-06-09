package com.autologin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class TriggerWordsScreen extends Screen {

    private static final int HEADER_H = 33;
    private static final int FOOTER_H = 36;

    private final Screen parent;
    private final AutoLoginConfig config;
    final List<String> words;

    private WordListWidget wordList;
    private TextFieldWidget addField;

    public TriggerWordsScreen(Screen parent, AutoLoginConfig config) {
        super(Text.translatable("autologin.triggers.title"));
        this.parent = parent;
        this.config = config;
        this.words = new ArrayList<>(config.triggerWords);
    }

    @Override
    protected void init() {
        int listH = height - HEADER_H - FOOTER_H;

        wordList = new WordListWidget(client, width, listH, HEADER_H, 22);
        wordList.refresh();
        addDrawableChild(wordList);

        int y = height - FOOTER_H + 6;
        addField = new TextFieldWidget(textRenderer, width / 2 - 140, y, 180, 18, Text.empty());
        addField.setMaxLength(128);
        addField.setPlaceholder(Text.translatable("autologin.triggers.add_placeholder"));
        addDrawableChild(addField);

        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.triggers.add"), btn -> addWord())
            .dimensions(width / 2 + 44, y - 1, 48, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.translatable("autologin.triggers.done"), btn -> {
            config.triggerWords = new ArrayList<>(words);
            config.save();
            client.setScreen(parent);
        }).dimensions(width / 2 + 96, y - 1, 50, 20).build());

        setFocused(addField);
    }

    private void addWord() {
        String word = addField.getText().trim();
        if (!word.isEmpty() && !words.contains(word)) {
            words.add(word);
            wordList.refresh();
            addField.setText("");
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

        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 11, 0xFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
            Text.translatable("autologin.triggers.hint"), width / 2, 21, 0x888888);

        ctx.fill(0, HEADER_H - 2, width, HEADER_H - 1, 0x55FFFFFF);
        ctx.fill(0, height - FOOTER_H, width, height - FOOTER_H + 1, 0x55FFFFFF);

        if (words.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                Text.translatable("autologin.triggers.empty"), width / 2, height / 2, 0x888888);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if ((keyCode == 257 || keyCode == 335) && addField.isFocused()) {
            addWord();
            return true;
        }
        if (keyCode == 256) { client.setScreen(parent); return true; }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    // ── Inner list widget ─────────────────────────────────────────────────────

    class WordListWidget extends EntryListWidget<WordListWidget.WordEntry> {

        WordListWidget(MinecraftClient client, int width, int height, int top, int itemHeight) {
            super(client, width, height, top, itemHeight);
        }

        @Override
        public void appendClickableNarrations(NarrationMessageBuilder builder) {}

        void refresh() {
            clearEntries();
            for (String w : words) addEntry(new WordEntry(w));
        }

        class WordEntry extends EntryListWidget.Entry<WordEntry> {
            private final String word;
            private final ButtonWidget removeBtn;

            WordEntry(String word) {
                this.word = word;
                this.removeBtn = ButtonWidget.builder(Text.translatable("autologin.triggers.remove"), b ->
                    TriggerWordsScreen.this.removeWord(word)
                ).dimensions(0, 0, 18, 14).build();
            }

            @Override
            public void render(GuiGraphics ctx, int index, int y, boolean hovered, float delta) {
                int x = WordListWidget.this.getRowLeft();
                int w = WordListWidget.this.getRowWidth();

                MinecraftClient mc = MinecraftClient.getInstance();
                int mx = (int)(mc.mouse.getX() / mc.getWindow().getScaleFactor());
                int my = (int)(mc.mouse.getY() / mc.getWindow().getScaleFactor());

                if (hovered) ctx.fill(x, y, x + w, y + 22, 0x1AFFFFFF);
                if (index > 0) ctx.fill(x + 4, y, x + w - 4, y + 1, 0x22FFFFFF);

                ctx.drawTextWithShadow(client.textRenderer, word, x + 10, y + 5, 0xFFFFFF);

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
