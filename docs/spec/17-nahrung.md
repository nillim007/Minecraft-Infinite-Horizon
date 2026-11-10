# 17. Nahrung

- Hunger wie Vanilla.
- `space_ration`: Nahrung und Sättigung = Summe der 3 Eingaben (max. 20). Stapelbar bis 16. Data Component `nutrition`.
- Planetenboden ist nicht bepflanzbar. `compost_soil` wird mit der Hacke zu `compost_farmland`, das sich wie Vanilla-Ackerland verhält.
- `hydroponics_basin`: ein Pflanzslot (Vanilla-Samen/Setzlinge). Die Pflanze wächst um 1 Altersstufe pro 2.000 t, das ist etwa doppelt so schnell wie auf idealem Vanilla-Ackerland (mit `ammonia_fertilizer` pro 1.333 t). Ausgewachsen wird nach der Vanilla-Loot-Table des Pflanzenblocks geerntet, Ertrag in den Ausgabeslot, Samen zurück in den Pflanzslot. `greenhouse` verhält sich wie 9 Becken. Setzlinge: 2 Stufen à 2.000 t, Ertrag 4 Stämme der Holzart + 1 Setzling.
- `ammonia_fertilizer`: Rechtsklick auf Becken oder Gewächshaus → +50 % Wachstum für 24.000 t.
- Alle Pflanzen brauchen Licht und eine aktive Kuppel (außer auf der Erde).
