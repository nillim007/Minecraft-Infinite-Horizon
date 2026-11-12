"""Erzeugt aus common/gametests.json den gemeinsamen Testkatalog und die Fabric-Hülle.

- common/src/main/java/de/infinitehorizon/gametest/IhTestCatalog.java  (NeoForge registriert per Schleife)
- fabric/src/main/java/de/infinitehorizon/gametest/IhFabricGameTests.java (Fabric braucht je Test eine @GameTest-Methode)
- fabric/src/main/resources/data/<ns>/test_environment/<id>.json (Fabric lädt Testumgebungen als Datenpaket;
  NeoForge registriert sie im Code aus IhTestCatalog.ENVIRONMENTS)

Aufruf: python tools/gen_gametests.py  (Ausgabe ist deterministisch; die CI prüft, dass sie aktuell ist)
"""
import io
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "common", "gametests.json")
CATALOG = os.path.join(ROOT, "common", "src", "main", "java", "de", "infinitehorizon", "gametest", "IhTestCatalog.java")
FABRIC = os.path.join(ROOT, "fabric", "src", "main", "java", "de", "infinitehorizon", "gametest", "IhFabricGameTests.java")
FABRIC_DATA = os.path.join(ROOT, "fabric", "src", "main", "resources", "data")


def camel(name):
    parts = re.split(r"[_\W]+", name)
    return parts[0] + "".join(p[:1].upper() + p[1:] for p in parts[1:])


def write(path, text):
    old = io.open(path, encoding="utf-8").read() if os.path.exists(path) else None
    if old != text:
        io.open(path, "w", encoding="utf-8", newline="\n").write(text)
        print("geschrieben:", os.path.relpath(path, ROOT))


def main():
    src = json.load(io.open(SRC, encoding="utf-8"))
    tests = src["tests"]
    envs = src.get("environments", {})
    for t in tests:
        if t.get("environment") and t["environment"] not in envs:
            sys.exit(f"Testumgebung {t['environment']} fehlt unter environments: {t['name']}")
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
        dim = f'"{t["dimension"]}"' if t.get("dimension") else "null"
        lines.append(f'            new Test("{t["name"]}", {t["ticks"]}, {struct}, {env}, {dim}, {t["class"]}::{t["method"]}){comma}{ref}')
    entries = "\n".join(lines)
    env_lines = []
    for i, (eid, e) in enumerate(sorted(envs.items())):
        comma = "," if i < len(envs) - 1 else ""
        setup = f'"{e["setup_function"]}"' if e.get("setup_function") else "null"
        env_lines.append(f'            new Environment("{eid}", {setup}){comma}')
    env_entries = "\n".join(env_lines)
    catalog = f'''package de.infinitehorizon.gametest;

import net.minecraft.gametest.framework.GameTestHelper;

import java.util.List;
import java.util.function.Consumer;

/**
 * Alle Server-GameTests. ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public final class IhTestCatalog {{

    /**
     * Name (Registry-Pfad), Zeitgrenze in Ticks, Struktur, Testumgebung und Dimension (null = Standard bzw.
     * Overworld), Testfunktion. Tests mit eigener Umgebung laufen in einem eigenen Durchgang (Leistungstest).
     */
    public record Test(String name, int maxTicks, String structure, String environment, String dimension,
                       Consumer<GameTestHelper> run) {{
    }}

    public static final List<Test> ALL = List.of(
{entries}
    );

    /** Testumgebung: ID und Setup-Funktion (null = leer). Fabric liest dieselben Werte als JSON. */
    public record Environment(String id, String setupFunction) {{
    }}

    public static final List<Environment> ENVIRONMENTS = List.of(
{env_entries}
    );

    private IhTestCatalog() {{
    }}
}}
'''
    methods = "\n\n".join(
        f'''    @GameTest(maxTicks = {t["ticks"]}{', structure = "' + t["structure"] + '"' if t.get("structure") else ""}{', environment = "' + t["environment"] + '"' if t.get("environment") else ""}{', dimension = "' + t["dimension"] + '"' if t.get("dimension") else ""})
    public void {camel(t["name"])}(GameTestHelper helper) {{
        {t["class"]}.{t["method"]}(helper);
    }}''' for t in tests)
    fabric = f'''package de.infinitehorizon.gametest;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/**
 * Fabric-Hülle um die gemeinsamen GameTests (Entry-Point „fabric-gametest“).
 * ERZEUGT von tools/gen_gametests.py aus common/gametests.json – nicht von Hand ändern.
 */
public class IhFabricGameTests {{

{methods}
}}
'''
    write(CATALOG, catalog)
    write(FABRIC, fabric)
    for eid, e in sorted(envs.items()):
        ns, path = eid.split(":", 1)
        if e.get("setup_function"):
            doc = {"type": "minecraft:function", "setup": e["setup_function"]}
        else:
            doc = {"type": "minecraft:all_of", "definitions": []}
        target = os.path.join(FABRIC_DATA, ns, "test_environment", path + ".json")
        os.makedirs(os.path.dirname(target), exist_ok=True)
        write(target, json.dumps(doc, indent=2) + "\n")
    print(f"{len(tests)} Tests")


if __name__ == "__main__":
    main()
