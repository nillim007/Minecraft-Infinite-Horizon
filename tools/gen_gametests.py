"""Erzeugt aus common/gametests.json den gemeinsamen Testkatalog und die Fabric-Hülle.

- common/src/main/java/de/sternenwerft/gametest/SwTestCatalog.java  (NeoForge registriert per Schleife)
- fabric/src/main/java/de/sternenwerft/gametest/SwFabricGameTests.java (Fabric braucht je Test eine @GameTest-Methode)

Aufruf: python tools/gen_gametests.py  (Ausgabe ist deterministisch; die CI prüft, dass sie aktuell ist)
"""
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "common", "gametests.json")
CATALOG = os.path.join(ROOT, "common", "src", "main", "java", "de", "sternenwerft", "gametest", "SwTestCatalog.java")
FABRIC = os.path.join(ROOT, "fabric", "src", "main", "java", "de", "sternenwerft", "gametest", "SwFabricGameTests.java")


def camel(name):
    parts = re.split(r"[_\W]+", name)
    return parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:])


def write(path, text):
    old = io.open(path, encoding="utf-8").read() if os.path.exists(path) else None
    if old != text:
        io.open(path, "w", encoding="utf-8", newline="\n").write(text)
        print("geschrieben:", os.path.relpath(path, ROOT))


def main():
    tests = json.load(io.open(SRC, encoding="utf-8"))["tests"]
    names = [t["name"] for t in tests]
    if len(set(names)) != len(names):
        sys.exit("Doppelte Testnamen in gametests.json")
    for t in tests:
        if not re.fullmatch(r"[a-z0-9_]+", t["name"]):
            sys.exit(f"Testname nur a-z, 0-9, _: {t['name']}")

    lines = []
    for i, t in enumerate(tests):
        comma = "," if i < len(tests) - 1 else ""
        ref = f"  // {t['ref']}" if t.get("ref") else ""
        struct = f'"{t["structure"]}"' if t.get("structure") else "null"
        env = f'"{t["environment"]}"' if t.get("environment") else "null"
        lines.append(f'            new Test("{t["name"]}", {t["ticks"]}, {struct}, {env}, {t["class"]}::{t["method"]}){comma}{ref}')
    entries = "\n".join(lines)
    catalog = f'''package de.sternenwerft.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

/**
 * Alle Server-GameTests. ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public final class SwTestCatalog {{

    /**
     * Name (Registry-Pfad), Zeitgrenze in Ticks, Struktur und Testumgebung (null = Standard), Testfunktion. Tests mit
     * eigener Umgebung laufen in einem eigenen Durchgang (Leistungstest).
     */
    public record Test(String name, int maxTicks, String structure, String environment, Consumer<GameTestHelper> run) {{
    }}

    public static final List<Test> ALL = List.of(
{entries}
    );

    private SwTestCatalog() {{
    }}
}}
'''
    methods = "\n\n".join(
        f'''    @GameTest(maxTicks = {t["ticks"]}{', structure = "' + t["structure"] + '"' if t.get("structure") else ""}{', environment = "' + t["environment"] + '"' if t.get("environment") else ""})
    public void {camel(t["name"])}(GameTestHelper helper) {{
        {t["class"]}.{t["method"]}(helper);
    }}''' for t in tests)
    fabric = f'''package de.sternenwerft.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric-Hülle um die gemeinsamen GameTests (Entry-Point „fabric-gametest“).
 * ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public class SwFabricGameTests {{

{methods}
}}
'''
    write(CATALOG, catalog)
    write(FABRIC, fabric)
    print(f"{len(tests)} Tests")


if __name__ == "__main__":
    main()
