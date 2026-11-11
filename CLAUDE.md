# Sternenwerft – Arbeitsanweisung für KI-Sitzungen

Minecraft-Mod (Minecraft 26.3, Java 25, NeoForge + Fabric, MultiLoader). Die Mod wird **Meilenstein für Meilenstein** nach der Spezifikation gebaut.

## Quellen

- **Spezifikation:** `docs/spec/` – Einstieg und Meilenstein-Matrix in `docs/spec/README.md`.
- **Maßgebliche Daten:** `spec/data/` – IDs, Namen, Rezepte, Forschung, Planeten, Raketenteile, Balance. Format: `spec/data/SCHEMA.md`.
- **Architektur:** `docs/spec/03a-architektur.md` – Paketstruktur und zentrale Klassen sind verbindlich.
- **Offene Annahmen:** `docs/spec-gaps.md`.
- **Handbuch und Wiki:** Quelle `docs/handbook/`, erzeugtes Wiki `docs/wiki/` (Abschnitt „Handbuch und Wiki“ unten).

Bei Widerspruch gilt: `spec/data` vor Text, sonst die Regeln in `docs/spec/00-lesehinweise.md`.

## Ablauf je Sitzung

1. Lies `docs/spec/README.md`, dann die Pflichtkapitel (0, 2, 3, 3A, 27) und die Kapitel und Daten des aktuellen Meilensteins laut Matrix.
2. Lies `docs/spec-gaps.md`, damit frühere Annahmen gleich bleiben.
3. Plane kurz, welche Klassen, Registrierungen, Datagen-Teile und Tests nötig sind. Halte dich an die Paketstruktur aus 3A.2.
4. Setze **nur** den aktuellen Meilenstein um. Nichts aus späteren Meilensteinen vorziehen, außer es ist für ein Abnahmekriterium zwingend nötig (dann in `docs/spec-gaps.md` vermerken).
5. Prüfe vor dem Abschluss:
   - `./gradlew build` ist grün (beide Loader).
   - Alle GameTests und JUnit-Tests laufen: `./gradlew test` und `./gradlew runGameTestServer` (je Loader-Projekt).
   - `python tools/gen_resources.py` erzeugt keine ungeprüften Änderungen mehr (die CI prüft das).
   - Alle Abnahmekriterien des Meilensteins aus Kapitel 27 sind erfüllt.
   - **Handbuch und Wiki sind nachgezogen** (Abschnitt unten) und `python tools/gen_wiki.py --check` ist grün.
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
- **Addon-freundlich** (Wunsch des Auftraggebers): einfache Schnittstelle mit vielen Einstellmöglichkeiten. Einstellwerte in Daten (Balance/Config, aus allen Namespaces zusammengeführt), Basisklassen statt Kopien, öffentliche Typen in `de.sternenwerft.api`. Details: `docs/dev/addon-api.md`.
- **Platzhalter-Assets** über `tools/gen_placeholders.py` (Farben in 24.5), keine echten Texturen erfinden.
- Übersetzungen kommen aus `items.json` (`name_de`, `name_en`); kein fest eingebauter Text.

## Bauen und Testen

**Lokal (schnell, bevorzugt zum Iterieren):** Wenn ein JDK 25 vorhanden ist (`java -version`), lokal nur das Fabric-Projekt bauen – das geht deutlich schneller als NeoForge oder die CI:

```bash
./gradlew :fabric:build
```

```bash
./gradlew :fabric:runGameTestServer
```

Ohne JDK 25 lokal nicht bauen, sondern GitHub nutzen (unten). Ein JDK nur mit Zustimmung des Auftraggebers herunterladen.

**GitHub Actions (vollständig, vor jedem Abschluss):** baut beide Loader, führt JUnit, Server-GameTests (NeoForge + Fabric) und den Fabric-Client-Test mit Screenshots aus (`.github/workflows/build.yml`).

1. Änderungen committen und pushen.
2. `python tools/ci_status.py --wait` im Hintergrund starten; es meldet GRÜN/ROT, Compiler-Fehler mit Datei und Zeile und die GameTest-Zusammenfassungen.
3. Bei ROT die Meldungen beheben und erneut pushen. Höchstens 60 GitHub-Abfragen pro Stunde (ohne Token).
4. **GUI prüfen:** Der Client-Test (`fabric/.../gametest/SwClientGameTests.java`) öffnet jedes Maschinen-GUI und fotografiert es; die CI legt die Bilder im Branch `ci-screenshots` ab. Abrufen ohne Anmeldung: `git clone --depth 1 -b ci-screenshots https://github.com/nillim007/Minecraft-New-Horizon.git <scratch>/shots` und die PNGs ansehen. Neue GUIs dort ergänzen.

Der Projekt-Skill `.claude/skills/minecraft-mod-ci-screenshots/SKILL.md` beschreibt diesen Ablauf ausführlich (inkl. Stolperstellen in 26.3); Claude-Sitzungen in diesem Repo laden ihn automatisch.

API-Spickzettel für 26.3: `docs/dev/api-neoforge-26.3.md`, `docs/dev/api-fabric-26.3.md`.

## Ressourcen

Ressourcen (Übersetzungen, Modelle, Blockzustände, Loot-Tables, Rezepte, Tags, Balance) erzeugt `python tools/gen_resources.py` aus `spec/data` für alle IDs in `common/implemented_ids.json`; Platzhalter-Texturen `python tools/gen_placeholders.py --only <ids>`. Neue Blöcke/Items: zuerst in `implemented_ids.json` eintragen, dann beide Skripte ausführen. Oberflächentexte stehen in `common/lang_extra.json`.

## Handbuch und Wiki (bei jedem Meilenstein und Teilschritt)

Das Spieler-Wiki (`docs/wiki/`) und das Ingenieurs-Handbuch im Spiel entstehen aus **einer Quelle**: `docs/handbook/` (Format: `docs/handbook/README.md`, Regeln: Kapitel 24.1, 25.6, 27). Wer etwas Spielbares einbaut, schreibt die Seite dazu, nicht erst am Ende.

1. **Neue Objekte dokumentieren.** Jedes neu registrierte Item, jeder Block und jedes Fluid erscheint auf einer Seite in `docs/handbook/pages/<kapitel>/`: entweder auf einer neuen Seite (Maschinen, Generatoren, Multiblöcke, Fahrzeuge, Planeten) oder in `items:` einer passenden bestehenden Seite (Materialien, Bauteile). Kapitel = Phase aus `items.json` (`phase_1` … `phase_6`).
2. **Inhalt einer Seite:** wozu es gut ist, wie man es benutzt und aufbaut (bei Multiblöcken Schritt für Schritt), Rezepte mit `@recipes_for`/`@recipes_type`, Steckbrief mit `@machine`, mindestens ein `@tip`, bei Gefahren ein `@warning`. Links mit `[[seite]]` und `<item:id>` auf verwandte Seiten.
3. **Keine Zahlen von Hand.** Werte immer über `{{value:<balance-datei>:<pfad>}}` oder `{{value:planet:…}}`; Rezepte nie abtippen. Fehlt ein Name für eine Vanilla-ID oder einen Tag, in `docs/handbook/handbook.json` unter `names` ergänzen.
4. **Ablauf pflegen.** Jede Phase hat eine Seite „Erste Schritte“ mit dem Ablauf als nummerierte Liste. Neue Schritte dort einfügen. Abschnitte zu Dingen, die erst später kommen, mit `@if <id>` … `@endif` umschließen; sie erscheinen dann von selbst.
5. **Tipps** sammeln: Was beim Testen auffällt (Stolperfallen, gute Aufbauten), kommt als `@tip` auf die Seite oder in `reference/tipps.md` (Fehlersuche).
6. **Erzeugen und prüfen:** `python tools/gen_resources.py` (Handbuch im Spiel) und `python tools/gen_wiki.py` (Wiki). Warnungen beheben (fehlende Namen, tote Links). Ausgaben nie von Hand ändern.
7. **README:** Statustabelle „Entwicklungsstand“ in `README.md` auf den neuen Stand bringen. Bei neuen Phasen oder großen Funktionen auch die Abschnitte „Was dich erwartet“ und „Einblicke“ prüfen; neue CI-Screenshots dürfen nach `docs/images/screenshots/` kopiert werden.
8. **Englisch** (`<seite>.en.md`) ist bis M9 freiwillig; ohne englische Fassung zeigt das Spiel den deutschen Text.

Das Buch-GUI selbst (Links, Item-Links, Rezeptanzeige, Suche, Taste `H`) gehört zu M2f (27.3). Bis dahin entstehen die Seiten trotzdem schon, damit das Wiki aktuell ist.

## Befehle

```bash
./gradlew build
```

```bash
python tools/gen_resources.py
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

```bash
python tools/gen_wiki.py
```
