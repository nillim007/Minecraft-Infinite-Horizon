# 11. Forschung

## 11.1 Forschungspunkte (FP)

| Quelle | FP | Häufigkeit |
| --- | --- | --- |
| Probe aus Ring 0 | 1 | |
| Probe aus Ring 1 | 5 | |
| Probe aus Ring 2 | 10 | |
| Sonde erreicht ein neues Ziel | 10 | einmal pro Ziel und Agentur |
| Neue Kreatur mit `field_scanner` analysiert | 15 | einmal pro Kreaturtyp und Agentur, nur die 15 Kreaturen aus Kapitel 18 |
| Erkundungsziel betreten (Struktur aus 13.4) | 20 | pro Strukturinstanz, Abnahme wie Proben pro Strukturtyp |

- **Abnahme:** Pro Probenart bzw. Strukturtyp zählen die ersten 10 voll, danach je 1/10. FP werden intern in **Hundertsteln** (int) gespeichert, Bruchteile werden abgerundet.
- **Forschungslabor** (`research_lab`): Analysatoren, die an ein Forschungslabor angrenzen, geben +50 % FP.
- Der Ring einer Probe folgt aus ihrer `dimension`: Overworld = Ring 0, sonst der `ring` aus dem Planet-Eintrag (25.1). Proben aus Nether, End und Dimensionen ohne Planeten-Eintrag geben keine FP.

## 11.2 Proben

- `sample_tool`: Rechtsklick auf einen Block → 1 `sample` mit `block` = Block-ID und `dimension`. Der Block bleibt. 40 t Abklingzeit. Pro Block-Position nur einmal (gespeichert in Chunk-Daten).
- **Probenart** = Block-ID + Dimension. `deepslate_`-Varianten zählen als dieselbe Art wie die normale Variante.
- **Von Spielern gesetzte Blöcke** sind nie beprobbar: Beim Setzen wird die Position in den Chunk-Daten als „beprobt“ markiert.
- **Beprobbar** sind in der Overworld nur Blöcke im Tag `infinite_horizon:sampleable/ring0`, in Planetendimensionen nur Blöcke im Tag `infinite_horizon:sampleable/planet`:
  - In Ring 0 nur die 10 Erzarten `bauxite_ore`, `titanium_ore`, `quartz_sand`, Kohle-, Eisen-, Kupfer-, Gold-, Redstone-, Lapis- und Diamanterz (max. 100 FP voll).
  - In Ring 1 und 2 alle natürlichen Blöcke aus 13.4 dieser Dimension, auch dort generierte Vanilla-Blöcke wie `ice`, `basalt`, `magma_block`.
- Ein Rechtsklick auf einen nicht beprobbaren Block zeigt „Keine verwertbare Probe“.

## 11.3 Forschungsterminal

`research_terminal`: GUI mit Forschungsbaum (4 Spalten = Äste, 6 Zeilen = Stufen). Mehrere Knoten derselben Zelle stehen nebeneinander, geordnet nach `position.slot` (25.3). Das GUI scrollt in beide Richtungen. Knoten sind gesperrt, verfügbar (Stufe offen, Voraussetzung erfüllt) oder erforscht. Erforschen zieht die FP der Agentur sofort ab. Hier gründet und verwaltet man auch Agenturen (Kapitel 23).

Ein Knoten schaltet Rezepte frei: Nicht freigeschaltete Rezepte lassen sich im Montagetisch und in Maschinen nicht ausführen und sind in JEI/REI/EMI als gesperrt markiert. Mit `research_required = false` ist alles frei. Ein Knoten schaltet **alle** Rezepte frei, deren Ergebnis eines der genannten Items ist, auch Alternativrezepte. **Items, die in keinem Knoten von 11.4 stehen, sind immer herstellbar** (z. B. `circuit`, `advanced_circuit`, `pressure_hull`, `heat_shield`, Upgrades, Ports, `analyzer`, `research_terminal`, `sample_tool`, `field_scanner`). Ob man sie herstellen kann, hängt dann nur von den Zutaten ab.

## 11.4 Stufen und Knoten

> **Maßgebliche Daten:** `spec/data/research.json` (6 Stufen mit Toren und Standardkosten, alle Knoten mit Ast, Kosten, Voraussetzungen, Freischaltungen und Position).
> **Lesefassung:** `docs/spec/generated/research.md`.

Regeln:

- Äste: `industry` (Industrie und Energie), `spaceflight` (Raumfahrt), `life_support` (Lebenserhaltung), `logistics` (Logistik).
- Stufe n öffnet sich erst, wenn Stufe n − 1 offen ist **und** ihr Tor erfüllt ist (Gate-Formate in 25.3). Tore schließen sich nie wieder.
- Ein Knoten ist verfügbar, wenn seine Stufe offen ist und alle `requires` erforscht sind. Erforschen zieht die Kosten sofort von den FP der Agentur ab.
- Sondenziele (`probe_target`) werden nur über Knoten freigeschaltet; Erde und `earth_orbit` sind immer bekannt.
- `tools/validate_spec.py` prüft, dass alle Voraussetzungen existieren, keine Zyklen bestehen, keine Voraussetzung in einer höheren Stufe liegt und alle Zutaten freigeschalteter Items spätestens in derselben Stufe verfügbar sind.
