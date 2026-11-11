# 5. Materialien

> **Maßgebliche Daten:** `spec/data/items.json` (alle IDs, Namen, Herkunft, Phase), `spec/data/recipes.json` (Verarbeitung).
> **Lesefassung:** `docs/spec/generated/items.md` (erzeugt von `tools/render_docs.py`).
> Dieses Kapitel beschreibt nur die Regeln. Zahlen und Listen stehen ausschließlich in den Daten.

## 5.1 Metalle und Formen

- Pro Metall gibt es höchstens die Formen `raw_<m>`, `<m>_dust`, `<m>_ingot`, `<m>_plate`, `<m>_rod`, `<m>_wire`. Welche Formen ein Metall hat, steht in `items.json` (Kategorie `material`).
- Bei Vanilla-Metallen (Eisen, Kupfer, Gold) fügt die Mod nur die fehlenden Formen hinzu; Rohstoff und Barren bleiben `minecraft:*`.
- Sonderfälle: Aluminium-Rohstoff heißt `bauxite`, Lithium-Rohstoff `lithium_salt`.
- Jede Form bekommt den passenden Konventions-Tag (`c:ingots/aluminum`, `c:plates/titanium`, `c:dusts/tungsten`, `c:raw_materials/nickel`, `c:ores/titanium` …). Rezepte verwenden für Metallformen bevorzugt diese Tags.
- **Tag-Muster:** Jeder Eintrag der Kategorien `material` und `ore` bekommt einen Tag `c:<form>s/<metall>`, z. B. `c:ingots/titanium`, `c:dusts/aluminum`, `c:plates/iron`, `c:rods/titanium`, `c:wires/copper`. `c:ores/titanium` gilt für `titanium_ore` und `deepslate_titanium_ore`, `c:ores/aluminum` für `bauxite_ore`, `c:raw_materials/aluminum` für `bauxite`. Die Rezepte in `recipes.json` nennen trotzdem Items; bei Widerspruch gelten die Daten (Kapitel 0).

## 5.2 Weitere Items

Alle übrigen Items (Bauteile, Werkzeuge, Drops, Behälter) stehen in `items.json` mit `obtained_by` und `spec_ref`. Besondere Item-Daten:

- `antimatter_cell`: Data Component `sternenwerft:charge` (0 = leer, 1 = gefüllt).
- `transport_tank` (16.000 mB), `oxygen_canister` (600 mB), `large_oxygen_canister` (1.200 mB): Data Component `sternenwerft:fluid`.
- `sample`: Data Component `sternenwerft:sample` mit `block` und `dimension`.
- Pressformen werden in der Presse nicht verbraucht.

## 5.3 Fluide

Alle Fluide stehen in `items.json` (`kind: fluid`) mit Typ (`liquid`/`gas`), Rohrfarbe, Quellblock und Eimer. Gase haben keine Quellblöcke und keine Eimer (3.3). Wasser ist Vanilla.

- **`crude_oil`:** wie Wasser, aber `tickDelay` 30, Fließweite 4 (Pegelabfall 2), hohe Dichte (der Spieler schwimmt langsam), wird nicht zur Quelle (`canConvertToSource` false), Licht 0.
- **Gase in M2:** `hydrogen` und `oxygen` haben in M2 keinen Speicher (`pressure_bottle` ist Phase 2) und keine Verbraucher. Sie bleiben im Maschinentank, Rohre dürfen sie führen. Ist der Tank voll, meldet der Elektrolyseur `error`. Das ist so gewollt.
- **Gase ab M3:** `pressure_bottle` speichert Gase (6.3), Sauerstoffflaschen nehmen `oxygen` auf (4.1), Kuppelgeneratoren und Anzüge verbrauchen es (9.1, 10.1).

## 5.4 Verarbeitungsketten (max. 3 Schritte)

```
Erz → Zerkleinerer (×2 Staub) → Elektroofen  → Barren → Presse (+Pressform) → Platte / Stange / Draht
Erz ───────────────────────────→ Vanilla-Ofen → Barren          (nur aluminum, titanium, iron, copper, gold)
raw_tungsten / tungsten_dust ──→ Hochtemperaturofen → tungsten_ingot
crude_oil → Raffinerie → kerosene + plastic
Wasser → Elektrolyseur → hydrogen + oxygen
moon_regolith → Regolith-Erhitzer → helium → Kryo-Verflüssiger → liquid_helium
```
