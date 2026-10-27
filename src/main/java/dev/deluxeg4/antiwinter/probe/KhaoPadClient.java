package dev.deluxeg4.antiwinter.probe;

import java.util.List;

public final class KhaoPadClient {
    private KhaoPadClient() {
    }

    public static final List<MooPingDetector> DEFAULT = List.of(
            new MooPingDetector("meteor", "Meteor Client", "key.meteor-client.open-gui", Ping999.METEOR, true),
            new MooPingDetector("liquidbounce", "LiquidBounce", "liquidbounce.module.killaura.name", Ping999.TRANSLATE, true),
            new MooPingDetector("wurst", "Wurst Client", "key.wurst.zoom", Ping999.KEYBIND, true),
            new MooPingDetector("freecam", "Freecam", "key.freecam.toggle", Ping999.KEYBIND, true),
            new MooPingDetector("xray", "XRay (Fabric)", "xray.config.toggle", Ping999.KEYBIND, true),
            new MooPingDetector("chestesp", "ChestESP", "key.chestesp.toggle", Ping999.KEYBIND, true),
            new MooPingDetector("killaura", "KillAura (Fabric)", "key.killaura", Ping999.KEYBIND, true),
            new MooPingDetector("autofish", "AutoFish", "key.autofish.open_gui", Ping999.KEYBIND, true),
            new MooPingDetector("lumina", "Lumina", "key.lumina.open_click_gui", Ping999.KEYBIND, true),
            new MooPingDetector("autoswitch", "AutoSwitch", "key.autoswitch.toggle", Ping999.KEYBIND, true),
            new MooPingDetector("bleachhack", "BleachHack", "bleachhack.module.killaura", Ping999.TRANSLATE, true),
            new MooPingDetector("aristois", "Aristois", "emc.module.killaura.name", Ping999.TRANSLATE, true),
            new MooPingDetector("coffee", "Coffee Client", "coffee.module.killaura.name", Ping999.TRANSLATE, true),
            new MooPingDetector("worlddownloader", "World Downloader", "key.wdl.startStop", Ping999.TRANSLATE, true),
            new MooPingDetector("autoclicker", "AutoClicker (Fabric)", "autoclicker-fabric.hud.holding", Ping999.TRANSLATE, true),
            new MooPingDetector("antiafk", "AntiAFK", "key.antiafk.toggle", Ping999.TRANSLATE, true),
            new MooPingDetector("autoclickermc", "Auto Clicker (p1k0chu)", "key.auto-clicker_.toggle", Ping999.KEYBIND, true),
            new MooPingDetector("trouserstreak", "Trouser Streak", "addon.trouser-streak.name", Ping999.TRANSLATE, true),
            new MooPingDetector("uiutils", "Ui Utils", "key.ui-utils.restore_screen", Ping999.TRANSLATE, false),
            new MooPingDetector("seedcrackerx", "SeedCrackerX", "cracker.already", Ping999.TRANSLATE, true),
            new MooPingDetector("simpleworlddownloader", "Simple World Downloader", "swd.screen.config.title", Ping999.TRANSLATE, true),
            new MooPingDetector("trouserstreak-newchunks", "Trouser Streak (NewerNewChunks)", "module.trouser-streak.NewerNewChunks.description", Ping999.TRANSLATE, true),
            new MooPingDetector("trouserstreak-anhero", "Trouser Streak (AnHero)", "module.trouser-streak.AnHero.description", Ping999.TRANSLATE, true),
            new MooPingDetector("glazed", "Glazed", "addon.glazed.name", Ping999.TRANSLATE, true),
            new MooPingDetector("litematica", "Litematica", "litematica.hotkey.name.openmainmenuscreen", Ping999.TRANSLATE, false),
            new MooPingDetector("itemscroller", "Item Scroller", "itemscroller.hotkey.name.openconfigscreen", Ping999.TRANSLATE, false),
            new MooPingDetector("xaeros-minimap", "Xaero's Minimap", "gui.xaero_minimap", Ping999.TRANSLATE, false),
            new MooPingDetector("baritone", "Baritone", "baritone.setting.allowBreak", Ping999.TRANSLATE, false)
    );
}
