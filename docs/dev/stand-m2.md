# Stand Meilenstein 2 (Erdindustrie)

Stand: 11.11.2026. Umfang und Teilschritte: `docs/spec/27-meilensteine.md`, Abschnitte 27.1 bis 27.3. Lückentest: `docs/dev/lueckentest-m2.md`.

## Erledigt

- **Spezifikation:** Ä23 (Nachzug M1), Ä24 (33 Lücken), Ä25. Ä25 enthält:
  - Erdöl-Quellblöcke werden nur mit 10 % Wahrscheinlichkeit verbraucht (`source_consume_chance`).
  - Drei Rezeptpaare sind getrennt.
  - Die Rakete T2 braucht 132 statt 128 `lightweight_plate`.
- **M2a** ist fertig. Der GitHub-Build ist grün, die GameTests melden auf beiden Loadern „All 5 required tests passed“.
  - `MachineMenu` und `MachineScreen` für alle Maschinen: GUI aus Rechtecken, Energie und Fortschritt als 16-Bit-Teile synchronisiert. Geöffnet wird je Loader über `IRegistryHelper.openMenu`.
  - `MachineLayout`: Eingänge, Formslot, Ausgänge, Upgrades, Brennstoff. Die Slot-Regeln stehen in `MachineBlockEntity`.
  - `UpgradeMath` mit JUnit-Test (63 t / 101 J/t). Der Fortschritt ist ein Anteil.
  - `ProcessingRecipe` mit `MachineRecipeInput` (mehrere Eingänge, Pressform), Rezeptarten `crushing`, `pressing` und `electric_smelting`. Der Elektroofen nutzt zusätzlich Vanilla-`smelting`.
  - Die einfachen Items kommen aus `sternenwerft/simple_items.json`, das `gen_resources.py` aus `implemented_ids.json` erzeugt.

## Nicht automatisch geprüft

- Aussehen und Bedienung des GUI. Manuell prüfen mit `./gradlew :neoforge:runClient` bzw. `:fabric:runClient`.

## Nächster Schritt

M2b: Erze und Worldgen (27.3). Die Annahmen stehen in `docs/spec-gaps.md` bis #20.
