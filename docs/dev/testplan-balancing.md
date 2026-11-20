# Testplan Balancing (Kapitel 28: „Mengen und Zeiten nach Testplan im Spiel prüfen“)

Ziel: prüfen, ob die Balancing-Ziele aus Kapitel 27 (Zielzeit und Strombedarf je Phase, Grundregeln) im echten Spiel
stimmen, und die offenen Balancing-Punkte aus Kapitel 28 entscheiden. Automatisch geprüft ist schon, dass alles
funktioniert (GameTests, Client-Spieltests M4–M9); hier geht es nur um Mengen, Zeiten und Spielgefühl.

Die Route Schritt für Schritt mit Mengen und Notiz-Hinweisen: [durchspielanleitung.md](durchspielanleitung.md).

## Aufbau

- **3–5 Tester**, jeder spielt eine eigene neue Welt im Einzelspieler, ohne Befehle und ohne Kreativmodus.
- **Zwei Durchgänge**: alle mit Standard-Einstellungen; mindestens ein Tester zusätzlich mit
  `recipe_amount_multiplier` = 0,5 in `<welt>/serverconfig/infinite_horizon.json` (24.4).
- Jar aus `./gradlew build` (`fabric/build/libs` bzw. `neoforge/build/libs`) vom Stand eines grünen CI-Laufs; Loader
  egal, beide sind gleichwertig.
- Rezepte und Ablauf nur aus dem Ingenieurs-Handbuch (Taste `H`) und JEI/REI, kein Wiki-Spoiler vorab.
- Gemessen wird die **Spielzeit** (Menü → Statistiken → „Gespielte Zeit“), nicht die Uhrzeit.

## Messpunkte

Das Spiel meldet jede neue Forschungsstufe im Chat, und das Handbuch schaltet das Kapitel der nächsten Phase frei.
Bei jedem Messpunkt die gespielte Zeit notieren:

| Ende von | Messpunkt im Spiel | Bedingung (`research.json`) |
| --- | --- | --- |
| Phase 1 · Erdindustrie | Stufe 1 „Raumfahrtprogramm“ erreichbar | `research_terminal` gesetzt |
| Phase 2 · Raumfahrtprogramm | Stufe 2 „Nahe Welten“, Erfolg „Ein kleiner Schritt“ | Mond betreten |
| Phase 3 · Ring 1 | Stufe 3 „Orbit“ | Magnesium, Wolfram und Helium hergestellt |
| Phase 4 · Orbit | Stufe 4 „Inneres System“ | Schwerelos-Fertiger hat ein Rezept fertig |
| Phase 5 · Ring 2 | Stufe 5 „Äußeres System“ | Saturn-Orbit betreten |
| Phase 6 · Netzwerk und Finale | Erfolg „Infinite Horizon“ | Sternenschiff gestartet |

Zusätzlich je Messpunkt den **Strombedarf**: Summe der Leistung (J/t aus dem Steckbrief im Handbuch) aller Maschinen,
die zu diesem Zeitpunkt gleichzeitig laufen sollen.

## Grundregeln (Kapitel 27) beobachten

1. **Raketenstufen:** `python tools/balance_sheet.py` zeigt am Ende das Verhältnis der Rohstoffe (Rakete T2 / T1,
   Sternenschiff / T2). Im Spiel prüfen: Fühlt sich jede Stufe nach etwa so viel mehr Arbeit an?
2. **Hand gegen Automatisierung (ab Phase 3):** Für ein Schlüsselmaterial (z. B. `tungsten_ingot`) einmal 16 Stück von
   Hand (Maschinen selbst befüllen und leeren) und einmal mit einfacher Automatisierung (Trichter, Rohre) herstellen und
   beide Zeiten notieren. Ziel: von Hand mindestens 3-mal so lang.
3. **Forschungspunkte:** Grob notieren, woher die FP kamen (Proben und Erkundungsziele gegen Analysator und
   Maschinen). Ziel: etwa die Hälfte aus Erkundung.
4. **Warnungen:** Jede Gefahr (Sauerstoff, Druck, Hitze, Kälte, Strahlung, Ereignisse, Reaktoren) soll mindestens
   200 t vorher warnen. Jede Gefahr notieren, die ohne Warnung Schaden machte.
5. **Warten:** Jede Phase notieren, in der man länger als etwa 10 Minuten nur warten konnte (nichts Sinnvolles zu tun).

## Offene Fragen aus Kapitel 28

- **Sternenschiff, Werftabschnitt 7 (Kühlung):** braucht mit Abstand am meisten (Kapitel 28). Wie lange dauert der
  Abschnitt mit der Automatisierung, die man zu diesem Zeitpunkt hat? Ist das eine gute Endspiel-Aufgabe oder nur
  Warten?
- **Sauerstoff (Ä29, bleibt so):** Reicht der Sauerstoffgenerator ohne Wasser auf dem Mond, oder zwingt er zum
  Wassertransport?
- **Multiplikator 0,5:** Fühlt sich das Spiel spürbar kürzer an, ohne dass Maschinen sinnlos werden?

## Bogen je Tester

Kopieren und ausfüllen (Zeiten als Spielzeit h:mm):

| Messpunkt | Spielzeit | Strombedarf (J/t) | Engpässe, Warten, Auffälliges |
| --- | --- | --- | --- |
| Ende Phase 1 | | | |
| Ende Phase 2 | | | |
| Ende Phase 3 | | | |
| Ende Phase 4 | | | |
| Ende Phase 5 | | | |
| Ende Phase 6 | | | |

| Grundregel | Ergebnis |
| --- | --- |
| Hand gegen Automatisierung (Material, Zeiten) | |
| FP aus Erkundung (grob in %) | |
| Gefahren ohne Warnung | |
| Phasen mit Warten | |
| Werftabschnitt 7 (Dauer, Eindruck) | |
| Sauerstoff | |

## Auswertung

Je Phase Mittelwert und Spanne der Spielzeiten mit der Zielzeit aus Kapitel 27 vergleichen. Abweichungen über etwa
25 % oder wiederholt genannte Engpässe kommen als Vorschlag mit Zahlen nach Kapitel 28; Werte ändern sich erst nach
Entscheidung in `spec/data` (danach `validate_spec.py`, `render_docs.py`, `gen_resources.py`).
