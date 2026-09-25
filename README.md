# ByteScythe System Utility

A straightforward Android debloating utility designed to clean up heavily bloated stock phones (Xiaomi, OPPO, Samsung, Realme, etc.). Works using **Shizuku** or **Root** access.

> ⚠️ **Note:** This tool is currently in early development and is **not properly tested**. Use at your own risk and make sure to back up critical data.

---

## Features

* **App Tabs:** Easily sort between `ALL`, `SYSTEM`, `USER`, and your `RECYCLE BIN` from the top navigation bar.
* **Dual Execution Modes:**
  * **Root Mode:** Full system access to remove stubborn packages.
  * **Shizuku Mode:** Rootless debloating (~80% success rate depending on OEM restrictions).
* **Batch Operations:** Select multiple system or user applications and uninstall or restore them in one go.
* **Temporary Recycle Bin & Undo:** 
  * Uninstalled apps drop into a limited **Recycle Bin** archive tab so you can hit `RESTORE` or `UNDO` if you made a mistake.
  * **Capacity Limit (3–5 Apps):** The bin buffer only holds 3 to 5 recent items. Once you exceed this limit or batch process past it, older apps are **permanently deleted—there is no turning back.**
  * *Known Bug Note:* The Recycle Bin sometimes visually retains older purged apps in the list view even after they have been permanently uninstalled.

---

## Shizuku Mode Limitations (~80% Accuracy)

If you are using **Shizuku (Non-Root)**, some aggressive OEM skins block standard ADB/Shizuku uninstall commands on specific pre-installed vendor apps:

* **Xiaomi / MIUI / HyperOS:** Apps like *Game Center* or certain system services may fail to delete without Root.
* **OPPO / ColorOS / Realme:** Apps like *OPPO Store* or *HeyTap* frameworks are hard-locked by OS permissions.

To completely wipe these restricted OEM system apps, you need to **root the phone**.

---

## How to Use

1. Open ByteScythe and grant either **Shizuku** or **Root** permissions.
2. Select **SYSTEM** or **USER** tab.
3. Check off the apps you want to remove and tap **UNINSTALL**.
4. Confirm the prompt to move them to the **RECYCLE BIN**.
5. If you change your mind immediately, tap **UNDO** or go to the **RECYCLE BIN** tab to restore them before the 3–5 app limit wipes them permanently.

---

## Links & Community

* **XDA Thread:** Join the discussion, report bugs, and give feedback on the official [XDA Thread](https://xdaforums.com/t/app-8-0-major-root-shizuku-bytescythe-system-utility-v1-1-1-shizuku-app-manager-2-1.4799190/).
