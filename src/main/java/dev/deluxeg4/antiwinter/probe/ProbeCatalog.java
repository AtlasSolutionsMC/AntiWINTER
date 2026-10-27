package dev.deluxeg4.antiwinter.probe;

import java.util.List;

public final class ProbeCatalog {
    private ProbeCatalog() {
    }

    public static final List<ProbeDefinition> DEFAULT = List.of(
            new ProbeDefinition("meteor", "Meteor Client", "key.meteor-client.open-gui", ProbeMode.METEOR, true),
            new ProbeDefinition("liquidbounce", "LiquidBounce", "liquidbounce.module.killaura.name", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("wurst", "Wurst Client", "key.wurst.zoom", ProbeMode.KEYBIND, true),
            new ProbeDefinition("freecam", "Freecam", "key.freecam.toggle", ProbeMode.KEYBIND, true),
            new ProbeDefinition("xray", "XRay (Fabric)", "xray.config.toggle", ProbeMode.KEYBIND, true),
            new ProbeDefinition("chestesp", "ChestESP", "key.chestesp.toggle", ProbeMode.KEYBIND, true),
            new ProbeDefinition("killaura", "KillAura (Fabric)", "key.killaura", ProbeMode.KEYBIND, true),
            new ProbeDefinition("autofish", "AutoFish", "key.autofish.open_gui", ProbeMode.KEYBIND, true),
            new ProbeDefinition("lumina", "Lumina", "key.lumina.open_click_gui", ProbeMode.KEYBIND, true),
            new ProbeDefinition("autoswitch", "AutoSwitch", "key.autoswitch.toggle", ProbeMode.KEYBIND, true),
            new ProbeDefinition("bleachhack", "BleachHack", "bleachhack.module.killaura", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("aristois", "Aristois", "emc.module.killaura.name", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("coffee", "Coffee Client", "coffee.module.killaura.name", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("worlddownloader", "World Downloader", "key.wdl.startStop", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("autoclicker", "AutoClicker (Fabric)", "autoclicker-fabric.hud.holding", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("antiafk", "AntiAFK", "key.antiafk.toggle", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("autoclickermc", "Auto Clicker (p1k0chu)", "key.auto-clicker_.toggle", ProbeMode.KEYBIND, true),
            new ProbeDefinition("trouserstreak", "Trouser Streak", "addon.trouser-streak.name", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("uiutils", "Ui Utils", "key.ui-utils.restore_screen", ProbeMode.TRANSLATE, false),
            new ProbeDefinition("seedcrackerx", "SeedCrackerX", "cracker.already", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("simpleworlddownloader", "Simple World Downloader", "swd.screen.config.title", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("trouserstreak-newchunks", "Trouser Streak (NewerNewChunks)", "module.trouser-streak.NewerNewChunks.description", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("trouserstreak-anhero", "Trouser Streak (AnHero)", "module.trouser-streak.AnHero.description", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("glazed", "Glazed", "addon.glazed.name", ProbeMode.TRANSLATE, true),
            new ProbeDefinition("litematica", "Litematica", "litematica.hotkey.name.openmainmenuscreen", ProbeMode.TRANSLATE, false),
            new ProbeDefinition("itemscroller", "Item Scroller", "itemscroller.hotkey.name.openconfigscreen", ProbeMode.TRANSLATE, false),
            new ProbeDefinition("xaeros-minimap", "Xaero's Minimap", "gui.xaero_minimap", ProbeMode.TRANSLATE, false),
            new ProbeDefinition("baritone", "Baritone", "baritone.setting.allowBreak", ProbeMode.TRANSLATE, false)
    );
}
