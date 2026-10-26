# AntiWINTER

Paper/Folia 1.21.11 plugin using PacketEvents 2.14.0. Install PacketEvents as a separate server plugin, then put the AntiWINTER jar in `plugins/`.

Workflow diagram: [.archify/workflow-antiwinter-20261027-0035/antiwinter-workflow.html](.archify/workflow-antiwinter-20261027-0035/antiwinter-workflow.html).

AntiWINTER observes two client-supplied protocol fields:

- `minecraft:brand`, when its complete value is `opsec`.
- `minecraft:register`, when a registered channel has the exact namespace `opsec`.

Each distinct positive detection adds 1 VL for that connection and is logged and sent to online staff with `antiwinter.alert` (operators have it by default). Repeated copies of the same signal do not add VL. The player is kicked at VL 2, so two distinct signals are required.

The `minecraft:brand` value is also checked against a small allowlist of common clients/loaders (Vanilla, Fabric, Forge, NeoForge, Quilt, OptiFine, Lunar, Badlion, Feather/Dawn, LabyMod, and Geyser). An unknown non-empty brand is reported to staff for review but does not add VL or kick by itself. `opsec` still contributes to VL as its own signal. Brand strings can be spoofed, and custom clients or modpacks may use a brand not on this list.

After a player joins, AntiWINTER probes up to three translation/keybind identifiers per temporary fake sign. The catalog covers the cheat clients and modules listed in CheckHacks (Meteor, LiquidBounce, Wurst, Freecam, XRay, ChestESP, KillAura, AutoFish, Lumina, AutoSwitch, BleachHack, Aristois, Coffee, World Downloader, AutoClicker, AntiAFK, Trouser Streak, SeedCrackerX and Glazed), plus review-only probes for Ui Utils, Litematica, Item Scroller, Xaero's Minimap and Baritone. Any positive result is scanned a second time; only a repeated positive result is accepted. Confirmed detections from the cheat/module set add 1 VL each; the review-only set never adds VL.

The fourth sign line is a `key.forward` control probe. If the client returns the unresolved key, or does not respond before timeout, staff receive an inconclusive/protected alert and the result adds no VL. The fake sign is removed from the client after one tick, restored after a response or timeout, and its sign-update packet is cancelled so no world sign is changed. Players may see a brief sign-editor flicker during a scan.

VL is connection-scoped and resets on disconnect. Unresolved keys and missing responses add no VL: vanilla and OpSec can produce the same result. OpSec may hide these keys, so this probe detects clients that expose the targeted mod translations; it does not prove OpSec is installed. A server resource pack that defines these same keys could still match a probe, although requiring the same result twice reduces accidental matches. Probe identifiers and response modes are adapted from the MIT-licensed [CheckHacks configuration and implementation](https://github.com/branduzzo/CheckHacks); its license is included in the jar at `META-INF/LICENSE-CheckHacks.txt`.

AntiWINTER follows the translation-key probe in the [AntiExploitPreventer source](https://github.com/themysterytrend/AntiExploitPreventer/blob/master/src/main/java/me/themysterytrend/antiExploitPreventer/checker/impl/TranslationChecker.java). Its current README separately describes replacing a message packet with a placeholder to bypass OpSec's message logger; that nonce technique is not used here.
