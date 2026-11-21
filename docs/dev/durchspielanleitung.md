# Durchspielanleitung: schnellste Route durch Infinite Horizon

Schritt für Schritt vom ersten Eisen bis zum Start des Sternenschiffs. Gedacht für den Spieltest nach
[testplan-balancing.md](testplan-balancing.md): Wer der Route folgt, misst die Zeiten vergleichbar.

**So liest du die Anleitung**

- Mengen sind **Barren** (bzw. Items), aus den Rezepten der Mod gerechnet und leicht aufgerundet (Stand 20.11.2026).
  Mit dem Zerkleinerer brauchst du nur **halb so viel Roherz** (1 Roherz → 2 Staub → 2 Barren).
- Rezepte zeigen JEI/REI und das Ingenieurs-Handbuch (Taste `H`, auch über einem Item im Inventar).
- Treibstoff gilt **je Komponente**: Eine Rakete T1 braucht dieselbe Menge Kerosin *und* Sauerstoff, T2 Wasserstoff
  *und* Sauerstoff. Ein Tank fasst 16.000 mB je Komponente.
- **[Notiz: …]** = bitte aufschreiben. Spielzeit immer aus *Menü → Statistiken → Gespielte Zeit*, dazu Stichworte.
  Diese Notizen sind die Rückmeldung, mit der das Balancing weitergeht (Abschnitt „Rückmeldung“ am Ende).
- Die Zielzeiten je Phase stehen in Kapitel 27 der Spezifikation; sie sind hier bewusst nicht wiederholt, damit du
  nicht gegen die Uhr spielst.

## Vorbereitung

0. Neue Welt, Überleben, Schwierigkeit normal, keine Befehle. Standard-Einstellungen; für einen zweiten Durchgang
   `recipe_amount_multiplier` = 0,5 in `<welt>/serverconfig/infinite_horizon.json`.
   **[Notiz: Loader (Fabric/NeoForge), Multiplikator, Datum]**

## Phase 1 · Erdindustrie

1. **Rohstoffe farmen:** 180 Eisen, 120 Kupfer, 32 Redstone, 20 Glas, 8 Quarzsand (Strand, Flussufer), 2 Stapel
   Kohle. Dabei schon **Bauxit** (Savanne, Wüste, Tafelberge) und **Titanerz** (tief, auch Tiefenschiefer) abbauen:
   Phase 2 braucht 250 Aluminium und 80 Titan.
   **[Notiz: Spielzeit nach dem Farmen; Bauxit und Titan gefunden? Wo, wie schnell?]**
2. **3 Maschinenrahmen** bauen.
3. **2 Verbrennungsgeneratoren** + 24 Kupferkabel, mit Kohle befeuern.
   **[Notiz: Reicht der Strom? Wie oft nachlegen?]**
4. **Zerkleinerer + Elektroofen.** Ab jetzt jedes Roherz zerkleinern (doppelte Ausbeute).
5. **Presse + 3 Pressformen** (Platte, Stange, Draht): Kupferdraht und Eisenplatten auf Vorrat.
6. **Silizium** aus Quarzsand im Elektroofen.
7. **Montagetisch** bauen.
8. **Erdöl:** Ölpfütze an der Oberfläche von Wüste oder Tafelbergen suchen (schwarz glänzende Mulde), Flüssigkeitspumpe direkt darüber, Tank daneben (die Pumpe gibt an allen Seiten außer vorn ab). Jede Quelle liefert im Schnitt knapp 2 Eimer, bevor sie verschwindet; nimm mehrere Pfützen mit.
   Ziel bis zum Mondflug: rund **110.000 mB Erdöl** (110 Eimer).
   **[Notiz: Wie lange hat die Suche nach einer Ölpfütze gedauert? Hat das Handbuch gereicht?]**
9. **Raffinerie** (3 × 3 × 5): Controller, 40 Gehäuse, 1 Item-Port, 2 Fluid-Ports, 1 Energie-Port. 1.000 mB Erdöl
   ergeben 600 mB Kerosin **und** 1 Kunststoff.
   **[Notiz: Aufbau verständlich? Hat die Geisterprojektion geholfen?]**
10. **Schaltkreise** (Silizium + Kupferdraht + Kunststoff) auf Vorrat, etwa 10.
11. **Elektrolyseur** an Wasser (unendliche Quelle 2 × 2 + Pumpe): 200 mB Wasser → 200 mB Wasserstoff + 100 mB
    Sauerstoff. **Wasserstoff aufheben**, ab Phase 4 ist er Raketentreibstoff.
12. **Akku, Brennstoffgenerator** (Kerosin) und 2 Geschwindigkeits-Upgrades.
    **[Notiz: Ende Phase 1 = Forschungsterminal gesetzt (Schritt 13): Spielzeit, Strombedarf aller laufenden
    Maschinen in J/t, wo hast du gewartet?]**

## Phase 2 · Raumfahrtprogramm

Material der Phase: 250 Aluminium, 120 Silizium, 90 Kupfer, 80 Titan, 50 Eisen, 55 Kunststoff, dazu 110.000 mB
Erdöl (→ Kerosin) und 100.000 mB Wasser (→ Sauerstoff).

13. **Forschungsterminal** setzen (öffnet Stufe 1).
14. **Probenzange + Analysator.** Von 10 Erdarten je 10 Proben (Bauxit, Titan, Quarzsand, Kohle, Eisen, Kupfer, Gold,
    Redstone, Lapis, Diamant; Tiefenschiefer zählt als dieselbe Art) = 1 FP je Probe, höchstens 100 FP. Gebraucht
    werden 60 FP.
    **[Notiz: Wie lange hat das Proben gedauert? Lästig oder in Ordnung?]**
15. **Forschen** (je 10 FP): Druckanzug, Solarenergie, Gasspeicher, Startrampe, dann Mondsonde, dann Rakete T1.
16. **Startrampe:** 25 Startrampen-Blöcke (5 × 5), Turm 5 Blöcke hoch direkt neben dem Rand, Treibstoffpumpe am
    Rand, Steuerpult; 4 Solarpanels.
17. **Sonde** bauen und von der Rampe starten (braucht keinen Treibstoff, fliegt etwa 1 Minute). Erst danach ist
    der Mond ein Raketenziel (+10 FP).
    **[Notiz: War klar, dass zuerst die Sonde fliegen muss?]**
18. **Rakete T1:** Triebwerk, 3 Tanks, 4 Flossen, Kapsel. Tanken: **48.000 mB Kerosin + 48.000 mB Sauerstoff**
    (3 Tanks voll). Das reicht hin und zurück: Erde → Mond 35.360, Mond → Erde 7.440, zusammen 42.800 mB.
    **[Notiz: Wie lange hat das Tanken gedauert? War die Checkliste im Raketen-Computer klar?]**
19. **Druckanzug** (4 Teile) + 2 volle Sauerstoffflaschen, Probenzange, Spitzhacke, Essen, freie Inventarplätze.
20. **Zum Mond:** in die Kapsel, Inventartaste → Raketen-Computer → Start → Mond wählen.
    **[Notiz: Ende Phase 2 = Mond betreten (Erfolg „Ein kleiner Schritt“): Spielzeit, Strombedarf. Countdown und
    Flug verständlich?]**

## Phase 3 · Ring 1 (Mond, Mars, Venus)

Material der Phase: 170 Aluminium, 75 Kupfer, 62 Silizium, 41 Titan, 37 Kunststoff, 17 Glas, 16 Eisen,
240 Mondregolith, 16 Mondbasalt. **Für Phase 4 schon sammeln:** 260 Magnesium (Mars) und 260 Wolfram (Venus).

21. **Auf dem Mond:** Proben (5 FP je Probe, 10 je Art voll), Lavaröhren im Mare (20 FP je Röhre), 240 Mondregolith,
    16 Mondbasalt, Monderze (Aluminium, Titan). Zurück, bevor der Sauerstoff knapp wird.
    **[Notiz: Hat der Sauerstoff gereicht? Kam die HUD-Warnung früh genug, also mindestens 10 Sekunden vorher?]**
22. **Forschen Stufe 2** (je 25 FP, zusammen 225): Kuppel I, Regolith-Erhitzer, Hochtemperatur, Kryotechnik,
    Magnesium-Legierung, Planetensonden, Landefeuer, Hitzeanzug, Rakete T2. Wahlweise: Hydrokultur,
    Raumfahrtnahrung.
    **[Notiz: Woher kamen die FP grob (Proben, Lavaröhren, Sonden)? In Prozent schätzen]**
23. **Mondbasis:** Kuppelgenerator I + Sauerstoffgenerator + Solarpanels, Elektrolampen statt Fackeln, Landefeuer.
    **[Notiz: Reicht der Sauerstoffgenerator ohne Wasser für die Kuppel, oder musstest du Wasser hochbringen?]**
24. **Regolith-Erhitzer** (Helium aus Regolith), **Hochtemperaturofen**, **Kryo-Verflüssiger**.
25. **Rakete auf 5 Tanks** erweitern (+2 Tanks T1): 80.000 mB je Komponente (≈ 134.000 mB Erdöl,
    160.000 mB Wasser). Mars hin und zurück braucht 69.200, Venus 64.000.
26. **Sonden zu Mars und Venus** (je +10 FP).
27. **Mars:** 260 Magnesium (≈ 130 Roherz), Proben (5 FP), Vulkanschlote und Meteoritenfelder (20 FP).
    **[Notiz: Wie lange hat der Marsaufenthalt gedauert? Was war im Weg?]**
28. **Venus:** Hitzeanzug + Kühlmodul anziehen. Die Rakete landet in der Wolkenschicht; mit Aerostat-Plattform und
    Wolkenaufzug geht es hinunter. Unten läuft die Hitzezeit (3 Minuten). 260 Wolfram (≈ 130 Roherz).
    **[Notiz: Reichte die Hitzezeit? Kam die Warnung rechtzeitig? Wie oft musstest du hoch und wieder runter?]**
29. **Stufe 3 öffnen:** je einen Magnesiumbarren, einen Wolframbarren und Helium herstellen.
    **[Notiz: Ende Phase 3 = Stufe 3 gemeldet: Spielzeit, Strombedarf. Schlüsselmaterial-Test aus dem Testplan:
    16 Wolframbarren einmal von Hand, einmal mit Trichtern, beide Zeiten]**

## Phase 4 · Orbit

Material der Phase: 1.410 Mondregolith, 424 Silizium, 332 Aluminium, 276 Titan, 256 Magnesium, 256 Wolfram,
247 Kupfer, 127 Kunststoff, 82 Gold, 54 Eisen, 35 Glas.

30. **Forschen Stufe 3** (je 50 FP): Stationskern → Stationsmodule → Supraleiter → Kernfusion; dazu Landefähre und
    Tankdepot.
31. **Rampe T2 und Rakete T2:** Rampe auf 7 × 7 erweitern (+24 Blöcke), Turm 6 hoch. Rakete: Triebwerk, 4 Tanks,
    4 Flossen, Kapsel. Treibstoff **64.000 mB Wasserstoff + 64.000 mB Sauerstoff** (Erde → Orbit 34.000, zurück
    2.000); das sind 128.000 mB Wasser im Elektrolyseur, plus den Wasserstoff aus Phase 1 und 2.
    **[Notiz: Wie lange hat die Wasser-Elektrolyse gedauert? Mit wie vielen Elektrolyseuren?]**
32. **Stationskern** mitnehmen; im Orbit Rechtsklick aus der Kapsel, er erscheint unter der Rakete. Magnetstiefel
    anziehen, dann aussteigen.
    **[Notiz: War das Aussteigen auf der Station klar und sicher?]**
33. **Station ausbauen:** Andockbucht, Tankdepot, 4 Solarflügel, Wohnmodul, Schwerelos-Fertiger, Landefähre.
34. **Fusionsreaktor** (Hülle 5 × 5 × 5, innen hohl: Controller und 97 Gehäuse, davon einige als Ports). Er braucht
    Wasserstoff und Helium als Brennstoff und flüssiges Helium als Kühlung (Kryo-Verflüssiger), dazu einen
    Energie-Port.
    **[Notiz: Hat dich die Abschaltung bei fehlender Kühlung überrascht? Kam eine Warnung?]**
35. **Stufe 4 öffnen:** Der Schwerelos-Fertiger schließt ein Rezept ab (z. B. Supraleiterdraht).
    **[Notiz: Ende Phase 4 = Stufe 4 gemeldet: Spielzeit, Strombedarf]**

## Phase 5 · Ring 2

Material der Phase: 704 Mondregolith, 236 Silizium, 168 Magnesium, 168 Aluminium, 156 Kupfer, 110 Kunststoff,
97 Lithium, 40 Iridium, 38 Gold, 30 Titan, 23 Nickel, 20 Wolfram, 20 Platin, 20 Glas, 16 Mondbasalt,
8 Mikrobenkulturen, 16.000 mB Wasser.

**Ab jetzt immer von der Station starten:** Ein Start aus dem Orbit kostet nichts. Von der Station zum Merkur
17.040 mB, zum Asteroidengürtel 20.000, zum Jupiter-Orbit 32.000, zum Saturn-Orbit 50.000 (nur mit
Fernflug-Modul). Nachtanken am Tankdepot.

36. **Forschen Stufe 4** (je 80 FP): Fortgeschrittene Sonde (nur 20 FP), Strahlenschutz, Lithiumspeicher,
    Iridium-Chips, Fernflug, Gasgewinnung, Magnetfalle; nach Bedarf Eisbohrung, Klon-Kammer, Außeneinsatz.
37. **Fortgeschrittene Sonde** zu allen Ring-2-Zielen schicken (je +10 FP).
38. **Merkur:** Hitzeanzug + Strahlenschild. 100 Lithium aus Lithiumsalz, daraus Lithium-Akkus.
39. **Asteroidengürtel und Ceres:** 20 Platin, 40 Iridium (für Phase 6 und die Werft wird es viel mehr, siehe dort),
    Eisvulkan auf Ceres (20 FP).
40. **Jupiter-Orbit:** Gas-Sammler (Wasserstoff), Magnetfalle füllt 2 Antimaterie-Zellen; mit der Landefähre zu Io.
41. **Europa:** Eisbohrer + Tauchmodul, 8 Mikrobenkulturen aus den Quellen, Klon-Kammer bauen.
42. **Fernflug-Modul** an die Rakete, dann in den Saturn-Orbit.
    **[Notiz: Ende Phase 5 = Saturn-Orbit betreten (Stufe 5): Spielzeit, Strombedarf. Welche Wege waren reine
    Fleißarbeit?]**

## Phase 6 · Netzwerk und Finale

Material der Phase (ohne Werft): 960 Mondregolith, 656 Silizium, 576 Aluminium, 496 Magnesium, 360 Kupfer,
231 Titan, 224 Kunststoff, 139 Gold, 128 Iridium, 96 Wolfram, 64 Platin, 64 Lithium, 56 Eisen, 48 Uran, 32 Nickel.

43. **Forschen Stufe 5** (je 120 FP): Tiefraumanzug, Kernspaltung, Bohrturm, Pumpstation, Frachtlogistik,
    Atomantrieb.
44. **Pluto:** Tiefraumanzug + Heizmodul. Von der Station 110.480 mB hin und 111.920 zurück, also volle Rakete T2
    mit 8 Tanks und Fernflug-Modul (256.000 mB). 50 Uran abbauen; unter dem Herzgletscher den Sender finden, den
    Wächter besiegen und die **Sternenkarte** mitnehmen.
    **[Notiz: Wie schwer und wie lang war der Kampf gegen den Wächter? Wie oft bist du gestorben?]**
45. **Kernreaktor** (Hülle wie der Fusionsreaktor, mit Kernspaltungs-Gehäuse) + 4 Brennstäbe.
46. **Stufe 6 öffnen:** einen Uranbarren herstellen und die Magnetfalle ein Rezept abschließen lassen. Forschen (je
    250 FP): Antimaterie-Antrieb, dann Orbitalwerft.
47. **Förderung und Fracht:** Bohrtürme und Pumpstationen auf den Planeten, 3 Frachtterminals, 2 Frachtraketen mit
    Atomantrieb, Logistik-Computer mit Routen zu den Werft-Docks.
    **[Notiz: Wie lange hat das Netzwerk gebraucht, bis es lief? Was war unklar?]**
48. **Orbitalwerft** im Erdorbit: Werft-Terminal und Docks an der Station. Die acht Abschnitte (Bauzeit je
    48.000 t = 40 Minuten):

    | Abschnitt | Material |
    | --- | --- |
    | 1 Rumpf | 512 Leichtbauplatten, 256 Titanplatten, 128 Aluminiumplatten |
    | 2 Schilde | 192 Wolframplatten, 64 Hitzeschilde |
    | 3 Hauptantrieb | 4 Antimaterie-Antriebe |
    | 4 Energie | 1 Kernreaktor, 64 Lithium-Akkus, 32 Brennstäbe |
    | 5 Bordcomputer | 128 Iridium-Chips, 64 Platinbarren, 1 Sternenkarte |
    | 6 Lebenserhaltung | 1 Kuppelgenerator III, 8 Sauerstoffgeneratoren, 1 Gewächshaus, 1 Klon-Kammer |
    | 7 Kühlung | 1.024.000 mB flüssiger Stickstoff, 512.000 mB flüssiges Helium, 512.000 mB Ammoniak |
    | 8 Manövriertriebwerke | 512.000 mB Methan, 256.000 mB Wasserstoff, 64 Schwefel |

    **[Notiz: Spielzeit beim Start jedes Abschnitts. Abschnitt 7 ist mit Abstand am größten: Wie lange hat er
    gedauert, mit wie vielen Regolith-Erhitzern, Kryo-Verflüssigern und Geysiren? Spannende Aufgabe oder nur
    Warten?]**
49. **Sternenschiff starten** am Werft-Terminal.
    **[Notiz: Ende Phase 6 = Erfolg „Infinite Horizon“: Spielzeit gesamt. Wie hat sich das Finale angefühlt?]**

## Rückmeldung

Schick mir nach dem Durchgang:

1. Alle **[Notiz]**-Einträge in der Reihenfolge der Schritte, auch kurze Stichworte.
2. Den ausgefüllten **Bogen** aus [testplan-balancing.md](testplan-balancing.md) (Spielzeit und Strombedarf je
   Phase, Grundregeln).
3. Alles, was **gefehlt** hat oder **kaputt** war: Fehlermeldungen, Abstürze (Datei `logs/latest.log`), unklare
   Texte, Rezepte, die du nicht gefunden hast.
4. Die drei **größten Zeitfresser** und die drei Stellen, die am meisten **Spaß** gemacht haben.

Daraus mache ich Vorschläge mit Zahlen. Werte in `spec/data` ändere ich erst, wenn du sie freigibst.
