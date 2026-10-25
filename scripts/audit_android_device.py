#!/usr/bin/env python3
"""Capture Color Lines with the pinned Artemis driver; never install/uninstall or clear data.

Run with the project Artemis venv. --matrix temporarily changes rotation/font scale
and restores the exact prior settings in finally. UI hierarchy uses read-only ADB
because Artemis's optional UI helper can install packages/change device settings.
"""
import argparse
import asyncio
import io
import hashlib
import json
from pathlib import Path
import re
import subprocess
import xml.etree.ElementTree as ET

PACKAGE = "io.github.basil_as.basillines"


async def audit(args):
    from adbutils import AdbClient
    from artemis.drivers.android.adb_driver import AndroidAdbDriver
    from PIL import Image

    def adb(*parts):
        return subprocess.check_output([args.adb, "-s", args.serial, *parts], text=True, timeout=30).strip()

    def shell(*parts):
        return adb("shell", *parts)

    def setting(key, value):
        if value == "null":
            shell("settings", "delete", "system", key)
        else:
            shell("settings", "put", "system", key, value)

    output = Path(args.output)
    output.mkdir(parents=True, exist_ok=True)
    driver = AndroidAdbDriver(args.serial, AdbClient(host="127.0.0.1", port=5037))
    saved = {key: shell("settings", "get", "system", key) for key in
             ["accelerometer_rotation", "user_rotation", "font_scale"]}
    report = {"driver": "Artemis AndroidAdbDriver", "model_api_used": False,
              "device": shell("getprop", "ro.product.model"),
              "api": shell("getprop", "ro.build.version.sdk"), "captures": []}

    async def capture(name):
        # Do not save another app, the lock screen, notifications or installer UI.
        resumed = shell("dumpsys", "activity", "activities")
        foreground = [line for line in resumed.splitlines() if "ResumedActivity" in line or "mResumed" in line]
        if not any(PACKAGE in line for line in foreground):
            raise RuntimeError("Color Lines is not the foreground app; capture refused")
        data = await driver.get_screen_data()
        bitmap = Image.open(io.BytesIO(data.screenshot_bytes))
        if min(bitmap.size) < 100:
            raise RuntimeError("Artemis returned an invalid/fallback screenshot")
        remote = "/data/local/tmp/colorlines-audit.xml"
        shell("uiautomator", "dump", remote)
        xml = shell("cat", remote)
        shell("rm", "-f", remote)
        nodes = [n for n in ET.fromstring(xml).iter("node") if n.get("package") == PACKAGE]
        if not nodes:
            raise RuntimeError("App hierarchy is obscured by a system/other window; capture refused")
        bitmap.save(output / f"{name}.png")
        (output / f"{name}.xml").write_text(xml)
        cells = [n for n in nodes if re.match(r"(?:Row \d+, column \d+|Ряд \d+, столбец \d+)", n.get("content-desc", ""))]
        if args.matrix and len(cells) != 81:
            raise RuntimeError(f"Expected the unobscured game board, found {len(cells)} labelled cells")
        report["captures"].append({"name": name, "pixels": bitmap.size,
                                   "app_nodes": len(nodes), "labelled_cells": len(cells),
                                   "board_fingerprint": hashlib.sha256("\n".join(sorted(", ".join(n.get("content-desc", "").split(", ")[:3]) for n in cells)).encode()).hexdigest()})
        (output / "report.json").write_text(json.dumps(report, indent=2))

    await driver.connect()
    try:
        if args.matrix:
            shell("am", "start", "-n", f"{PACKAGE}/.MainActivity")
            setting("accelerometer_rotation", "0")
            for rotation, orientation in [(0, "portrait"), (1, "landscape")]:
                setting("user_rotation", str(rotation))
                for scale in ["1.0", "2.0"]:
                    setting("font_scale", scale)
                    await asyncio.sleep(2)
                    await capture(f"{orientation}-font-{scale}")
            signatures = {item["board_fingerprint"] for item in report["captures"]}
            if len(signatures) != 1:
                raise RuntimeError("Board changed during rotation/font changes without a game action")
        else:
            await capture(args.name)
    finally:
        restore_errors = []
        if args.matrix:
            for key, value in saved.items():
                try:
                    setting(key, value)
                except Exception as error:
                    restore_errors.append(f"{key}: {error}")
        await driver.disconnect()
        if restore_errors:
            raise RuntimeError("Device settings need restoration: " + "; ".join(restore_errors))
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--adb", default="adb")
    parser.add_argument("--output", required=True)
    parser.add_argument("--name", default="current")
    parser.add_argument("--matrix", action="store_true")
    asyncio.run(audit(parser.parse_args()))
