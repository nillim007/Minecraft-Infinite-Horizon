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
- **Rechnung (ab M4):** Wert in Hundertsteln = `fp × scale`, ab der 11. Probe derselben Art bzw. der 11. Instanz desselben Strukturtyps × `factor_after`, abgerundet (`balance/research.json`). Beispiele: Ring 0 = 100, ab der 11. Probe 10; Ring 1 = 500 bzw. 50; Erkundungsziel = 2.000 bzw. 200. Die Zähler gehören der Agentur (`sample_counts` je Probenart, `structure_counts` je Strukturtyp, 23.1). Gezählt werden analysierte Proben, nicht beprobte Blöcke.
- **Forschungslabor** (`research_lab`, ab M6): Analysatoren, die an ein Forschungslabor angrenzen, geben +50 % FP.
- Der Ring einer Probe folgt aus ihrer `dimension`: Overworld = Ring 0, sonst der `ring` aus dem Planet-Eintrag (25.1, `PlanetRegistry`). Proben aus Nether, End und Dimensionen ohne Planeten-Eintrag geben keine FP.
- **Analysator (ab M4):** Die FP einer Probe gehen am Ende des Vorgangs an die Agentur `OwnershipResolver.current(owner)` des Analysators. Ohne Besitzer steht er auf `error` und macht keinen Fortschritt. Proben, die keine FP geben, nimmt der Eingangsslot nicht an (6.1).
- **Erkundungsziel betreten (ab M4):** Geprüft wird zusammen mit der Umweltprüfung (alle `interval_ticks`, 8.2): Der Spieler steht in einem Teil (`StructureManager#getStructureWithPieceAt`) einer Struktur aus `exploration_target.structures`. Instanz = Dimension + Struktur-ID + Start-Chunk (`instance_key`). Je Agentur und Instanz einmal: FP nach der Abnahme je Strukturtyp, Eintrag in `found_structures` (Atlas, 24.2) und Meldung in der Aktionsleiste (`message.infinite_horizon.exploration.found`). Zuschauer zählen nicht. In M4 gibt es nur `lava_tube` auf dem Mond (13.4).
- **Sonde:** Die 10 FP gibt es einmal je Ziel und Agentur, bei der Ankunft (12.3).

## 11.2 Proben

- `sample_tool`: Rechtsklick auf einen Block → 1 `sample` mit `block` = Block-ID und `dimension`. Der Block bleibt. 40 t Abklingzeit als Vanilla-Item-Abklingzeit. Pro Block-Position nur einmal (gespeichert in Chunk-Daten).
- **Probenart** = Block-ID + Dimension. `deepslate_`-Varianten zählen als dieselbe Art wie die normale Variante.
- **Stapeln:** Gleiche Proben (gleiche Komponente `sample`) stapeln bis 64. Der Tooltip nennt Block und Dimension.
- **Von Spielern gesetzte Blöcke** sind nie beprobbar: Beim Setzen wird die Position in den Chunk-Daten als „beprobt“ markiert.
  - Markiert werden nur Blöcke aus den beiden `sampleable`-Tags, die ein Spieler über ein `BlockItem` setzt. Kolben, Endermen und Befehle markieren nicht.
  - Speicher: Chunk-Attachment `sampled_positions` als Menge gepackter Positionen (`BlockPos#asLong`, 3A.4). Ein Eintrag bleibt nach dem Abbau des Blocks bestehen.
- **Beprobbar** sind in der Overworld nur Blöcke im Tag `infinite_horizon:sampleable/ring0`, in Planetendimensionen nur Blöcke im Tag `infinite_horizon:sampleable/planet`:
  - In Ring 0 nur die 10 Erzarten `bauxite_ore`, `titanium_ore`, `quartz_sand`, Kohle-, Eisen-, Kupfer-, Gold-, Redstone-, Lapis- und Diamanterz (max. 100 FP voll). Der Tag enthält jede Art mit ihrer `deepslate_`-Variante; die Liste steht in `balance/research.json` (`sample_ring_rules.ring0_blocks`).
  - In Ring 1 und 2 alle natürlichen Blöcke aus 13.4 dieser Dimension, auch dort generierte Vanilla-Blöcke wie `ice`, `basalt`, `magma_block`. `gen_resources.py` füllt den Tag aus `blocks[].id` und `ores[].block` in `planets.json` für alle Dimensionen in `common/implemented_ids.json` (M4: die 6 Mondblöcke).
- **Meldungen** in der Aktionsleiste: Ein Rechtsklick auf einen nicht beprobbaren Block zeigt „Keine verwertbare Probe“ (`message.infinite_horizon.sample.none`), auf eine schon beprobte Position `message.infinite_horizon.sample.used`.

## 11.3 Forschungsterminal

`research_terminal`: GUI mit Forschungsbaum (4 Spalten = Äste, 6 Zeilen = Stufen). Mehrere Knoten derselben Zelle stehen nebeneinander, geordnet nach `position.slot` (25.3). Das GUI scrollt in beide Richtungen. Knoten sind gesperrt, verfügbar (Stufe offen, Voraussetzung erfüllt) oder erforscht. Erforschen zieht die FP der Agentur sofort ab. Im Reiter „Agentur“ verwaltet man die Agentur (23.1).

**Forschungsbaum (ab M4):**

- Aufbau wie der Vanilla-Erfolgsbildschirm: verschiebbare Fläche. Knoten 26 × 26 px mit dem Item-Symbol aus `icon`; Rahmen grau (gesperrt), gelb (verfügbar), grün (erforscht); Linien zu den Knoten in `requires`.
- Spaltenköpfe = Äste. Zeilenköpfe = Stufen mit Tor-Text und Haken, wenn die Stufe offen ist. Oben die FP der Agentur mit 2 Nachkommastellen.
- Tooltip eines Knotens: Name, Kosten und Freischaltungen. Ein Klick auf einen verfügbaren Knoten sendet `ResearchUnlockPayload`.
- Der Server prüft beim Payload: Das offene Menü ist ein `research_terminal` höchstens 8 Blöcke entfernt, die Rolle ist mindestens `engineer`, die Stufe ist offen, alle `requires` sind erforscht und die FP reichen.
- Texte unter `gui.infinite_horizon.research.*` in `lang_extra.json` (6.4).

Ein Knoten schaltet Rezepte frei: Nicht freigeschaltete Rezepte lassen sich im Montagetisch und in Maschinen nicht ausführen und sind in JEI/REI/EMI als gesperrt markiert. Mit `research_required = false` ist alles frei. Ein Knoten schaltet **alle** Rezepte frei, deren Ergebnis eines der genannten Items ist, auch Alternativrezepte. **Items, die in keinem Knoten von 11.4 stehen, sind immer herstellbar** (z. B. `circuit`, `advanced_circuit`, `pressure_hull`, `heat_shield`, Upgrades, Ports, `analyzer`, `research_terminal`, `sample_tool`, `field_scanner`). Ob man sie herstellen kann, hängt dann nur von den Zutaten ab.

**Rezeptsperre (ab M4):**

- `ResearchRecipeLock.isUnlocked(recipe, owner)`: Ein Rezept ist gesperrt, wenn eines seiner Ergebnisse in `unlocks` eines Knotens steht, den die Agentur `OwnershipResolver.current(owner)` nicht erforscht hat. Ohne Besitzer oder mit `research_required = false` ist es frei.
- Maschinen und Montagetisch prüfen alle passenden Rezepte in der Reihenfolge der Rezept-ID und nehmen das erste freie. Sind alle gesperrt, steht der Block auf `idle` mit dem Tooltip `gui.infinite_horizon.status.locked` (3A.3, 22.3).
- Werkbank-Rezepte haben keine gesperrten Ergebnisse.
- `research_required = false` gibt Rezepte und Handbuch-Kapitel frei, nicht die Sondenziele (12.3).
- JEI/REI zeigen in den eigenen Kategorien ein Schloss und einen Tooltip (3.9); die Daten kommen aus `AgencyStatePayload`.
- **Welten aus M3:** Die Sperre trifft auch 10 Objekte aus M3: die 4 Anzugteile, beide Sauerstoffflaschen (Knoten `suit_t1`) und `pressure_bottle` (Knoten `gas_storage`, Stufe 1) sowie `dome_generator_t1`, `oxygen_generator` und `electric_lamp` (Knoten `dome_t1`, Stufe 2). Gebaute Blöcke laufen weiter. Das Handbuch nennt diese Objekte (27.6).

## 11.4 Stufen und Knoten

> **Maßgebliche Daten:** `spec/data/research.json` (6 Stufen mit Toren und Standardkosten, alle Knoten mit Ast, Kosten, Voraussetzungen, Freischaltungen und Position).
> **Lesefassung:** `docs/spec/generated/research.md`.

Regeln:

- Äste: `industry` (Industrie und Energie), `spaceflight` (Raumfahrt), `life_support` (Lebenserhaltung), `logistics` (Logistik).
- Stufe n öffnet sich erst, wenn Stufe n − 1 offen ist **und** ihr Tor erfüllt ist (Gate-Formate in 25.3). Tore schließen sich nie wieder.
- **Prüfung der Tore (ab M4):** `ResearchGates` ist eine reine Funktion für alle 5 Tor-Typen (JUnit). Geprüft wird beim Setzen eines Blocks mit Besitzer, beim Dimensionswechsel eines Mitglieds und nach dem Erforschen eines Knotens; offene Stufen stehen in `open_tiers` (23.1). Die Zähler für `crafted_items` und `machine_completed` kommen mit M5; Stufe 3 bleibt in M4 zu.
- Ein Knoten ist verfügbar, wenn seine Stufe offen ist und alle `requires` erforscht sind. Erforschen zieht die Kosten sofort von den FP der Agentur ab.
- **Sichtbare Knoten (ab M4):** `gen_resources.py` erzeugt alle 6 Dateien in `research_tier/`, aber nur die Knoten mit mindestens einer registrierten Freischaltung (`common/implemented_ids.json`) oder einem Sondenziel mit Planeten-Eintrag (M4: 9 Knoten, 27.6). Spätere Knoten erscheinen mit ihrem Meilenstein. Freischaltungen von IDs, die noch nicht registriert sind, werden mit dem Knoten gespeichert und wirken später. `validate_spec.py` warnt, wenn ein sichtbarer Knoten einen unsichtbaren voraussetzt.
- Sondenziele (`probe_target`) werden nur über Knoten freigeschaltet; Erde und `earth_orbit` sind immer bekannt. Ein freigeschaltetes Ziel (`probe_targets`) darf eine Sonde anfliegen; erst ein erreichtes Ziel (`probe_reached`) ist ein Raketenziel (12.1). Ziele ohne Planeten-Eintrag bleiben bis zu ihrem Meilenstein unsichtbar.
- `tools/validate_spec.py` prüft, dass alle Voraussetzungen existieren, keine Zyklen bestehen, keine Voraussetzung in einer höheren Stufe liegt und alle Zutaten freigeschalteter Items spätestens in derselben Stufe verfügbar sind.
