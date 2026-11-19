# 17. Nahrung

- Hunger wie Vanilla.
- `space_ration`: Nahrung und Sättigung = Summe der 3 Eingaben (max. 20). Stapelbar bis 16. Data Component `nutrition`.
- Planetenboden ist nicht bepflanzbar. `compost_soil` wird mit der Hacke zu `compost_farmland`, das sich wie Vanilla-Ackerland verhält.
- `hydroponics_basin`: ein Pflanzslot (Vanilla-Samen/Setzlinge). Die Pflanze wächst um 1 Altersstufe pro 2.000 t, das ist etwa doppelt so schnell wie auf idealem Vanilla-Ackerland (mit `ammonia_fertilizer` pro 1.333 t). Ausgewachsen wird nach der Vanilla-Loot-Table des Pflanzenblocks geerntet, Ertrag in den Ausgabeslot, Samen zurück in den Pflanzslot. `greenhouse` verhält sich wie 9 Becken. Setzlinge: 2 Stufen à 2.000 t, Ertrag 4 Stämme der Holzart + 1 Setzling.
- `ammonia_fertilizer`: Rechtsklick auf Becken oder Gewächshaus → +50 % Wachstum für 24.000 t.
- Alle Pflanzen brauchen Licht und eine aktive Kuppel (außer auf der Erde).

## Festlegungen aus der Umsetzung (M1–M8)

Annahmen aus der Umsetzung, die übernommen wurden. Die Nummer verweist auf den Eintrag in `docs/spec-gaps.md`, dort steht auch die Code-Stelle.

- **#94 (M5h, 17, 6.1):** Nahrung: Pflanzenauswahl, Ernte, Inhalt von `compostable`, Hacke, Gewächshaus und Dünger → Das Becken nimmt Blöcke vom Typ `CropBlock` und `SaplingBlock` (keine Stängel, Kakao oder Beeren). Ernte nach der Loot-Table des reifen Blocks, ein Exemplar des gepflanzten Items daraus bleibt im Pflanzslot (fehlt es, bleibt der Samen trotzdem). Stamm zum Setzling über den Namen (`_sapling`/`_propagule` → `_log`, sonst `oak_log`). Licht = Rohhelligkeit über dem Becken ohne Tageszeit wie bei Vanilla-Feldfrüchten; Kuppelpflicht über die Regel aus 8.5. Speed-Upgrades verkürzen die Stufen. `compostable` = pflanzliche Vanilla-Abfälle (Laub, Setzlinge, Blumen, Samen, Feldfrüchte). Die Hacke wirkt über einen Mixin in `BlockTransformer`, Kompost-Ackerland steht in `supports_crops`, `grows_crops` und `supports_vegetation`. `greenhouse` (Orbit, M6) und `ammonia_fertilizer` (Ammoniak, M8) kommen mit ihren Meilensteinen; das Becken kann den Dünger schon speichern (`fertilize`). Eine Ration ohne Komponente (Kreativ) sättigt 0
