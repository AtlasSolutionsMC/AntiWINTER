# AntiWINTER

[English](README.md) | **ไทย**

ปลั๊กอินตรวจจับสัญญาณ client สำหรับ Paper/Folia 1.21.11 โดยใช้ PacketEvents 2.14.0 ติดตั้ง PacketEvents เป็นปลั๊กอินแยก แล้วนำไฟล์ AntiWINTER jar ไปไว้ใน `plugins/`

AntiWINTER ใช้ MIT License ดูรายละเอียดที่ [LICENSE](LICENSE)

## การตรวจจับ

- ผู้เล่นทุกคนได้รับ probe scan ชุดเดียวกัน ไม่ขึ้นกับค่า `minecraft:brand` หาก brand ไม่รู้จัก จะเพิ่มสัญญาณไว้ตรวจสอบ 1 VL และตรวจต่อ ไม่เตะจาก brand อย่างเดียว
- ตรวจ plugin channels รวมถึง channel ที่ประกาศผ่าน `minecraft:register` namespace ของ client ที่รู้จักจะเพิ่ม 1 VL ต่อ namespace
- เก็บและเทียบชุด channel ด้วย `/winterchannels capture <name> <player>` แล้วดูด้วย `/winterchannels show <player>` หรือเทียบด้วย `/winterchannels compare <name> <player>` คำสั่งจัดการ baseline อยู่ใน `plugins/AntiWINTER/channel-fingerprints.yml` การตรงกับ baseline ของ OpSec จะแจ้งแอดมินเพื่อพิจารณาเท่านั้น
- probe translation และ keybind ครอบคลุม Meteor, LiquidBounce, Wurst, Freecam, XRay, ChestESP, KillAura, AutoFish, Lumina, AutoSwitch, BleachHack, Aristois, Coffee, World Downloader, AutoClicker, AntiAFK, Trouser Streak, SeedCrackerX, Simple World Downloader และ Glazed ผลบวกจะถูก probe ซ้ำก่อนนับ ส่วน Ui Utils, Litematica, Item Scroller, Xaero's Minimap และ Baritone เป็นสัญญาณแจ้งเพื่อพิจารณาเท่านั้น

แจ้งเตือนไปยังแอดมินที่มี permission `antiwinter.alert` (ค่าเริ่มต้นให้ operator) แต่ละสัญญาณที่ไม่ซ้ำเพิ่ม 1 VL และเตะเมื่อถึง VL 2; probe ของ Meteor ที่ยืนยันซ้ำเตะที่ VL 1 brand ที่ไม่รู้จักเพียงอย่างเดียวไม่ทำให้ถูกเตะ VL ถูกล้างเมื่อผู้เล่นออก

การตอบ key ที่ไม่ถูก resolve หรือไม่มีการตอบกลับถือเป็นผลสรุปไม่ได้ จึงไม่เพิ่ม VL เพราะ client ปกติและ client ที่บล็อก probe อาจให้ผลเหมือนกัน เริ่มตรวจหลังเข้าเกม 10 ticks; ป้าย sign ชั่วคราวจะถูกซ่อนหลัง 1 tick และคืนสภาพหลังตอบกลับหรือหมดเวลา ผู้เล่นอาจเห็นหน้าต่างแก้ sign กระพริบสั้นๆ

## Resource-pack shim แบบทดลอง

สร้าง pack ด้วย `gradle packShimZip` จากนั้นโฮสต์ `build/distributions/AntiWINTER-pack-shim.zip` ให้เข้าถึงผ่าน HTTP(S) และตั้ง `pack-shim.enabled`, `pack-shim.url`, `pack-shim.sha1` ใน `config.yml` ค่าเริ่มต้นปิดไว้

Pack มี language canary และคำแปลสำหรับ probe keys หาก pack โหลดไม่สำเร็จหรืออ่าน canary ไม่ได้ AntiWINTER จะแจ้งแอดมินว่าได้ผลสรุปไม่ได้และหยุด probe scan รอบนั้น โดยไม่เพิ่ม VL หรือเตะ ควรทดลองกับ Vanilla, Fabric และ OpSec ที่ทราบผลก่อนใช้งานจริง Pack อาจเปลี่ยนภาษาที่ผู้เล่นเลือก และต้องโฮสต์ให้ผู้เล่นทุกคนเข้าถึงได้

สัญญาณเหล่านี้บอกได้เฉพาะข้อมูลที่ client เปิดเผยให้ server เห็น ไม่ได้พิสูจน์ว่ามี mod ใดติดตั้งอยู่ Client สามารถปลอม brand หรือกรอง channels, known packs และผล translation ได้ การไม่พบสัญญาณจึงไม่ใช่หลักฐานว่าเป็น Vanilla ส่วน resource pack ของ server ที่กำหนด probe key ก็ทำให้เกิดผลบวกได้ จึงต้องยืนยัน probe ซ้ำ

## แหล่งอ้างอิง

- [CheckHacks](https://github.com/branduzzo/CheckHacks) — รายชื่อ probe และรูปแบบผลตอบกลับ; ใช้ MIT License
- [AntiExploitPreventer TranslationChecker](https://github.com/themysterytrend/AntiExploitPreventer/blob/master/src/main/java/me/themysterytrend/antiExploitPreventer/checker/impl/TranslationChecker.java) — อ้างอิงแนวทาง translation probe
- [ModFingerprint](https://github.com/NorkelL/TexturepackAnticheat-fabric) — แนวคิดทดลองสำหรับ channel fingerprint และ resource-pack shim
- [OpSec](https://github.com/aurickk/OpSec/) — อ้างอิงพฤติกรรมการกรอง client channels

## ข้อความสัญญาอนุญาตของ CheckHacks

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
