package dev.anhcraft.radiumenu.utils.chat;

import java.util.LinkedHashMap;
import java.util.Map;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ComponentBuilder {
    private BaseComponent component;
    private ClickEvent clickEvent;
    private HoverEvent hoverEvent;
    private String insertion;
    private final Map<Object, Object[]> extra = new LinkedHashMap<>();
    private ChatColor color = ChatColor.WHITE;
    private boolean bold = false;
    private boolean obfuscate = false;
    private boolean underline = false;
    private boolean italic = false;
    private boolean strikethrough = false;

    public ComponentBuilder(String text) {
        this.component = new TextComponent(TextComponent.fromLegacyText(text));
    }

    public ComponentBuilder(String text, Class<? extends BaseComponent> clazz) {
        if (clazz.equals(TextComponent.class)) {
            this.component = new TextComponent(text);
        }
    }

    public ComponentBuilder(Class<? extends BaseComponent> clazz) {
        if (clazz.equals(TextComponent.class)) {
            this.component = new TextComponent();
        }
    }

    public ComponentBuilder(BaseComponent component) {
        this.component = component;
    }

    public ComponentBuilder text(String text, Object ... events) {
        this.extra.put(text, events);
        return this;
    }

    public ComponentBuilder component(BaseComponent component) {
        this.extra.put(component, new Object[0]);
        return this;
    }

    public ComponentBuilder color(ChatColor color) {
        this.color = color;
        return this;
    }

    public ComponentBuilder event(ClickEvent clickEvent) {
        this.clickEvent = clickEvent;
        return this;
    }

    public ComponentBuilder event(HoverEvent hoverEvent) {
        this.hoverEvent = hoverEvent;
        return this;
    }

    public ComponentBuilder bold() {
        this.bold = !this.bold;
        return this;
    }

    public ComponentBuilder italic() {
        this.italic = !this.italic;
        return this;
    }

    public ComponentBuilder underline() {
        this.underline = !this.underline;
        return this;
    }

    public ComponentBuilder strikethrough() {
        this.strikethrough = !this.strikethrough;
        return this;
    }

    public ComponentBuilder obfuscate() {
        this.obfuscate = !this.obfuscate;
        return this;
    }

    public ComponentBuilder insertion(String text) {
        this.insertion = text;
        return this;
    }

    public BaseComponent build() {
        if (clickEvent != null) {
            component.setClickEvent(clickEvent);
        }
        if (hoverEvent != null) {
            component.setHoverEvent(hoverEvent);
        }
        component.setColor(color);
        component.setBold(bold);
        component.setObfuscated(obfuscate);
        component.setItalic(italic);
        component.setStrikethrough(strikethrough);
        component.setUnderlined(underline);
        if (insertion != null) {
            component.setInsertion(insertion);
        }
        for (Map.Entry<Object, Object[]> entry : extra.entrySet()) {
            ComponentBuilder cb;
            if (entry.getKey() instanceof String) {
                cb = new ComponentBuilder((String) entry.getKey(), TextComponent.class);
                for (Object obj : entry.getValue()) {
                    if (obj instanceof ClickEvent) {
                        cb.event((ClickEvent)obj);
                        continue;
                    }
                    if (!(obj instanceof HoverEvent)) continue;
                    cb.event((HoverEvent)obj);
                }
                component.addExtra(cb.build());
                continue;
            }
            if (!(entry.getKey() instanceof BaseComponent)) continue;
            cb = new ComponentBuilder((BaseComponent) entry.getKey());
            for (Object obj : entry.getValue()) {
                if (obj instanceof ClickEvent) {
                    cb.event((ClickEvent)obj);
                    continue;
                }
                if (!(obj instanceof HoverEvent)) continue;
                cb.event((HoverEvent)obj);
            }
            component.addExtra(cb.build());
        }
        return component;
    }
}
