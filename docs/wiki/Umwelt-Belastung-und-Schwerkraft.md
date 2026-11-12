<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->

[📖 Wiki](README.md) › Willkommen › **Umwelt, Belastung und Schwerkraft**

# Umwelt, Belastung und Schwerkraft

Auf der Erde musst du dir um Luft, Druck und Temperatur keine Gedanken machen. Auf anderen Himmelskörpern schon: Jede Welt hat eigene **Umweltwerte**, und jede Gefahr, gegen die du nicht geschützt bist, baut eine **Belastung** auf.

## Die Umweltwerte

Der Server prüft alle 10 t (0,5 s), wo du stehst, und berechnet:

- **Atemluft:** Gibt es eine atembare Atmosphäre? Auf dem Mond nicht.
- **Temperatur:** Sie folgt dem Tageslicht. Auf dem Mond steigt sie mittags auf 120 °C und fällt nachts auf -170 °C. In der Dämmerung liegt sie dazwischen.
- **Druck:** von Vakuum über niedrig und normal bis hoch.
- **Strahlung:** Stufe 0 bis 3. Stehen mindestens 3 feste Blöcke über dir, sinkt sie um 2 Stufen.

Bestimmte Bereiche haben eigene Werte. Im Polarkrater des Mondes bleibt es immer bei -190 °C, und Sonnenenergie gibt es dort nicht.

Dimensionen ohne eigene Umweltwerte, etwa der Nether, das End oder Welten anderer Mods, gelten wie die Erde.

## Was du ohne Anzug verträgst

| Gefahr | Ohne Anzug sicher |
| --- | --- |
| Temperatur | -20 bis 45 °C |
| Druck | niedrig bis normal |
| Strahlung | bis Stufe 1 |
| Atemluft | nur in atembarer Luft |

## Die Belastung

Jede Gefahr hat ihre eigene Belastung. Bist du ungeschützt, füllt sie sich in 200 t (10 s) von 0 auf 100 %. Erst bei 100 % nimmst du Schaden, und Rüstung hilft dagegen nicht. Ohne Sauerstoff oder im Vakuum kostet das 1 Lebenspunkt alle 20 t (1 s).

Ist die Gefahr vorbei, sinkt die Belastung in 100 t (5 s) wieder auf 0. Mehrere Gefahren zählen getrennt: Ohne Anzug auf dem Mond steigen Sauerstoff und Druck gleichzeitig, dazu je nach Tageszeit Hitze oder Kälte.

Im Kreativ- und im Zuschauermodus gibt es keine Belastung. Nach dem Tod beginnt sie wieder bei 0.

> [!WARNING]
> Die Belastung kündigt sich nur kurz an: Nach 200 t (10 s) im Vakuum beginnt der Schaden. Geh nie ohne Plan nach draußen.

## Die Anzeige im HUD

Über der Hungerleiste erscheint für jede Gefahr eine eigene Leiste, sobald ihre Belastung steigt. Sie ist grün, wird ab 50 % gelb und ab 80 % rot. Beim Erscheinen ertönt ein Warnton.

Trägst du einen vollständigen [Druckanzug](Der-Druckanzug.md) außerhalb atembarer Luft, zeigt eine weitere Leiste seinen Sauerstoffvorrat. Liegen Temperatur oder Strahlung außerhalb dessen, was du ohne Anzug verträgst, stehen ihre Werte darüber.

## Schwerkraft

Jede Welt hat ihre eigene Schwerkraft. Auf dem Mond beträgt sie das 0,17-Fache der Erde. Du fällst langsamer, springst viel höher und überstehst tiefe Stürze ohne Schaden: Die sichere Fallhöhe (3 Blöcke auf der Erde) wird durch die Schwerkraft geteilt, und Fallschaden wird im selben Maß kleiner.

Die Sprungkraft selbst bleibt gleich. Darum springst du auf dem Mond ein Vielfaches höher als auf der Erde.

> [!TIP]
> Strahlung lässt sich abschirmen: Schon 3 feste Blöcke über dem Kopf senken sie um 2 Stufen. Eine Höhle oder ein Dach ist auf fremden Welten oft die schnellste Deckung.

---

_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._

← [Maschinen bedienen](Maschinen-bedienen.md) · [Ausblick – was noch kommt](Ausblick-was-noch-kommt.md) →
