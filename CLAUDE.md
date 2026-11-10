# Sternenwerft – Arbeitsanweisung für KI-Sitzungen

Minecraft-Mod (Minecraft 26.3, Java 25, NeoForge + Fabric, MultiLoader). Die Mod wird **Meilenstein für Meilenstein** nach der Spezifikation gebaut.

## Quellen

- **Spezifikation:** `docs/spec/` – Einstieg und Meilenstein-Matrix in `docs/spec/README.md`.
- **Maßgebliche Daten:** `spec/data/` – IDs, Namen, Rezepte, Forschung, Planeten, Raketenteile, Balance. Format: `spec/data/SCHEMA.md`.
- **Architektur:** `docs/spec/03a-architektur.md` – Paketstruktur und zentrale Klassen sind verbindlich.
- **Offene Annahmen:** `docs/spec-gaps.md`.

Bei Widerspruch gilt: `spec/data` vor Text, sonst die Regeln in `docs/spec/00-lesehinweise.md`.

## Ablauf je Sitzung

1. Lies `docs/spec/README.md`, dann die Pflichtkapitel (0, 2, 3, 3A, 27) und die Kapitel und Daten des aktuellen Meilensteins laut Matrix.
2. Lies `docs/spec-gaps.md`, damit frühere Annahmen gleich bleiben.
3. Plane kurz, welche Klassen, Registrierungen, Datagen-Teile und Tests nötig sind. Halte dich an die Paketstruktur aus 3A.2.
4. Setze **nur** den aktuellen Meilenstein um. Nichts aus späteren Meilensteinen vorziehen, außer es ist für ein Abnahmekriterium zwingend nötig (dann in `docs/spec-gaps.md` vermerken).
5. Prüfe vor dem Abschluss:
   - `./gradlew build` ist grün (beide Loader).
   - Alle GameTests und JUnit-Tests laufen: `./gradlew test` und `./gradlew runGameTestServer` (je Loader-Projekt).
   - `./gradlew runData` erzeugt keine ungeprüften Änderungen mehr.
   - Alle Abnahmekriterien des Meilensteins aus Kapitel 27 sind erfüllt.
6. Committe mit einer Nachricht der Form `M<n>: <was>` und fasse zusammen, was fertig ist und was nicht.

## Regeln

- **Keine Zahl im Code.** Werte kommen aus `BalanceManager` bzw. den Registries, die Datagen aus `spec/data` erzeugt.
- **Spezifikation nicht eigenmächtig ändern.** Fehlt etwas oder ist etwas widersprüchlich:
  - einfachste Vanilla-nahe Lösung wählen,
  - im Code `// SPEC-GAP: <Beschreibung>` setzen,
  - in `docs/spec-gaps.md` eintragen (Format dort),
  - am Ende der Sitzung erwähnen.
- **`spec/data` nur ändern, wenn ausdrücklich darum gebeten.** Danach immer `python tools/validate_spec.py` und `python tools/render_docs.py` ausführen.
- **Reine Funktionen für Regeln** (3A.1) und JUnit-Tests dafür.
- **Server entscheidet**, Client stellt nur dar.
- **Platzhalter-Assets** über `tools/gen_placeholders.py` (Farben in 24.5), keine echten Texturen erfinden.
- Übersetzungen kommen aus `items.json` (`name_de`, `name_en`); kein fest eingebauter Text.

## Befehle

```bash
./gradlew build
```

```bash
./gradlew runData
```

```bash
./gradlew :neoforge:runGameTestServer
```

```bash
./gradlew :fabric:runGameTestServer
```

```bash
python tools/validate_spec.py
```
