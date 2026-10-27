# AntiWINTER

Paper/Folia 1.21.11 client-signal detection using PacketEvents 2.14.0. Install PacketEvents as a separate server plugin, then put the AntiWINTER jar in `plugins/`.

AntiWINTER checks three client signals:

- Every player receives the same probe scan regardless of their `minecraft:brand` value. Known vanilla, loader, and popular client brands are allowed; an unknown non-empty brand adds one review signal and the scan continues.
- Client plugin channels, including names listed in `minecraft:register`. Channels in the known-client namespace list add 1 VL per namespace.
- Registered channel-set fingerprints. Capture reference sets from known vanilla, Fabric and OpSec clients with `/winterchannels capture <name> <player>`, then inspect or compare with `/winterchannels show <player>` and `/winterchannels compare <name> <player>`. Baselines live in `plugins/AntiWINTER/channel-fingerprints.yml`; an exact OpSec match is an admin review alert only. Capture each server/client version you want to compare.
- Translation and keybind probes for Meteor, LiquidBounce, Wurst, Freecam, XRay, ChestESP, KillAura, AutoFish, Lumina, AutoSwitch, BleachHack, Aristois, Coffee, World Downloader, AutoClicker, AntiAFK, Trouser Streak, SeedCrackerX, Simple World Downloader and Glazed. A positive probe is repeated before it counts. Review-only probes cover Ui Utils, Litematica, Item Scroller, Xaero's Minimap and Baritone.

Confirmed signals are sent only to staff with `antiwinter.alert` (operators have it by default). Each distinct signal adds 1 VL; the player is kicked at VL 2. A confirmed Meteor translation probe kicks at VL 1. An unknown brand by itself does not kick; it can combine with an independent confirmed probe or channel signal. VL resets on disconnect.

An unresolved key or missing response is inconclusive and adds no VL: an ordinary client and a client that blocks probes can look the same. Probe checks start 10 ticks after joining; the temporary sign is hidden after one tick, restored after the response or timeout, and its sign update is cancelled. Players may see a brief sign-editor flicker.

An experimental forced resource-pack shim can be built with `gradle packShimZip`. Host `build/distributions/AntiWINTER-pack-shim.zip` at an HTTP(S) URL, calculate its SHA-1, and set `pack-shim.enabled`, `pack-shim.url`, and `pack-shim.sha1` in `config.yml`. It is disabled by default. The pack adds a language canary and translations for the probe keys. If the pack fails to load or the canary is missing, AntiWINTER reports an inconclusive admin alert and stops that probe scan; it does not add VL or kick from this result. Test with known vanilla, Fabric, and OpSec clients before relying on any result. The shim can disrupt a player's chosen language while active, and the pack must be reachable by every player.

These checks identify client signals that are exposed to the server; they do not prove a specific mod is installed. Clients can spoof brands and filter channels, known packs, or translation results. Do not treat a missing signal as proof that a player is vanilla. A server resource pack that defines a probed key can also cause a match, so positive probes must repeat before they count.

## References

- [CheckHacks](https://github.com/branduzzo/CheckHacks) — probe identifiers and response modes; MIT License.
- [AntiExploitPreventer TranslationChecker](https://github.com/themysterytrend/AntiExploitPreventer/blob/master/src/main/java/me/themysterytrend/antiExploitPreventer/checker/impl/TranslationChecker.java) — translation probe behavior reference.
- [ModFingerprint](https://github.com/NorkelL/TexturepackAnticheat-fabric) — experimental registered-channel fingerprints and resource-pack shim concept.
- [OpSec](https://github.com/aurickk/OpSec/) — client channel filtering behavior used as a reference for the OpSec baseline.

CheckHacks license notice:

```text
MIT License

Copyright (c) 2026 Branduzzo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```
