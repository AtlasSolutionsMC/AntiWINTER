# AntiWINTER

Paper/Folia 1.21.11 client-signal detection using PacketEvents 2.14.0. Install PacketEvents as a separate server plugin, then put the AntiWINTER jar in `plugins/`.

Workflow diagram: [.archify/workflow-antiwinter-20261027-0035/antiwinter-workflow.html](.archify/workflow-antiwinter-20261027-0035/antiwinter-workflow.html).

AntiWINTER checks three client signals:

- Every player receives the same probe scan regardless of their `minecraft:brand` value. Known vanilla, loader, and popular client brands are allowed; an unknown non-empty brand adds one review signal and the scan continues.
- Client plugin channels, including names listed in `minecraft:register`. Channels in the known-client namespace list add 1 VL per namespace.
- Registered channel-set fingerprints. Capture reference sets from known vanilla, Fabric and OpSec clients with `/winterchannels capture <name> <player>`, then inspect or compare with `/winterchannels show <player>` and `/winterchannels compare <name> <player>`. Baselines live in `plugins/AntiWINTER/channel-fingerprints.yml`; an exact OpSec match is an admin review alert only. Capture each server/client version you want to compare.
- Translation and keybind probes for Meteor, LiquidBounce, Wurst, Freecam, XRay, ChestESP, KillAura, AutoFish, Lumina, AutoSwitch, BleachHack, Aristois, Coffee, World Downloader, AutoClicker, AntiAFK, Trouser Streak, SeedCrackerX, Simple World Downloader and Glazed. A positive probe is repeated before it counts. Review-only probes cover Ui Utils, Litematica, Item Scroller, Xaero's Minimap and Baritone.

Confirmed signals are sent only to staff with `antiwinter.alert` (operators have it by default). Each distinct signal adds 1 VL; the player is kicked at VL 2. A confirmed Meteor translation probe kicks at VL 1. An unknown brand by itself does not kick; it can combine with an independent confirmed probe or channel signal. VL resets on disconnect.

An unresolved key or missing response is inconclusive and adds no VL: an ordinary client and a client that blocks probes can look the same. Probe checks start 10 ticks after joining; the temporary sign is hidden after one tick, restored after the response or timeout, and its sign update is cancelled. Players may see a brief sign-editor flicker.

An experimental forced resource-pack shim can be built with `gradle packShimZip`. Host `build/distributions/AntiWINTER-pack-shim.zip` at an HTTP(S) URL, calculate its SHA-1, and set `pack-shim.enabled`, `pack-shim.url`, and `pack-shim.sha1` in `config.yml`. It is disabled by default. The pack adds a language canary and translations for the probe keys. If the pack fails to load or the canary is missing, AntiWINTER reports an inconclusive admin alert and stops that probe scan; it does not add VL or kick from this result. Test with known vanilla, Fabric, and OpSec clients before relying on any result. The shim can disrupt a player's chosen language while active, and the pack must be reachable by every player.

These checks identify client signals that are exposed to the server; they do not prove a specific mod is installed. Clients can spoof brands and filter channels, known packs, or translation results. Do not treat a missing signal as proof that a player is vanilla. A server resource pack that defines a probed key can also cause a match, so positive probes must repeat before they count.

Probe identifiers and response modes are adapted from the MIT-licensed [CheckHacks configuration and implementation](https://github.com/branduzzo/CheckHacks); its license is included in the jar at `META-INF/LICENSE-CheckHacks.txt`. Translation-probe behavior follows the [AntiExploitPreventer TranslationChecker](https://github.com/themysterytrend/AntiExploitPreventer/blob/master/src/main/java/me/themysterytrend/antiExploitPreventer/checker/impl/TranslationChecker.java).
