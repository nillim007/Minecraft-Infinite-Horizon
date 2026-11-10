# 0. Lesehinweise für die umsetzende KI

Die Kapitel in `docs/spec/` und die Daten in `spec/data/` sind zusammen die **einzige Quelle** für die Umsetzung. Sie sollen ohne Rückfragen umsetzbar sein. Welche Kapitel und Daten ein Meilenstein braucht, steht in `docs/spec/README.md`.

**Verbindlichkeit**

- **MUSS** = Pflicht. **SOLL** = umsetzen, wenn es den Aufwand nicht sprengt. **KANN** = optional.
- Bei Widerspruch gilt: **`spec/data` vor allem Text**, dann Kapitel 25 (Datenformate), dann Tabelle vor Fließtext, dann späteres Kapitel vor früherem. Widersprüche zwischen Daten und Text werden zusätzlich in `docs/spec-gaps.md` gemeldet.
- Alle Zahlen sind **Startwerte**. Sie MÜSSEN in den Datendateien aus Kapitel 25 (Balance-Werte in 25.7) oder in der Konfiguration stehen, nie fest im Code (Ausnahme: Konstanten in Kapitel 3.6).
- Fehlt trotzdem ein Detail, wähle die einfachste Lösung im Stil von Vanilla und markiere die Stelle im Code mit `// SPEC-GAP: <Beschreibung>`.

**Einheiten**

| Größe | Einheit | Hinweis |
| --- | --- | --- |
| Zeit | Ticks (t) | 20 t = 1 s, 1.200 t = 1 min, 24.000 t = 1 Ingame-Tag, 1.000 t = 1 Ingame-Stunde |
| Energie | J, J/t | 1 J = 1 FE |
| Fluide/Gase | mB | 1.000 mB = 1 Eimer. Fabric: 1 mB = 81 Droplets |
| Strecke | Blöcke | |
| Temperatur | °C | spielinterne Werte, nicht exakt real |
| Schwerkraft | g | 1 g = Vanilla-Schwerkraft 0,08 |

**Konventionen**

- Mod-ID `sternenwerft`. Alle IDs englisch, `snake_case`. Im Text stehen IDs in `code`, z. B. `sternenwerft:crusher` oder kurz `crusher`.
- Ein **„Tank X“** in Rezepten bedeutet 16.000 mB des Fluids X und wird als `fluid`-Zutat kodiert (25.4). Der Montagetisch bekommt das Fluid über Rohre oder über seinen Leerslot (4.1), zum Beispiel aus einem gefüllten `transport_tank`.
- Mengen wie „4 Aluminiumplatten“ beziehen sich auf die Item-IDs aus Kapitel 5.
- **Assets:** Die KI erzeugt **Platzhalter-Texturen** (16×16 PNG, Grundfarbe nach Kapitel 24.5 mit 1 px dunklerem Rand, Items mit hellerem Mittelpunkt) per Skript `tools/gen_placeholders.py`. Modelle für Blöcke: Würfel (`cube_all` bzw. `orientable` für Maschinen). Kreaturen: einfache Quader-Modelle nach Größe in Kapitel 18.
