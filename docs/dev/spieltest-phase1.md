# Spieltest Phase 1 – Änderungswünsche (21.11.2026)

Rückmeldung des Auftraggebers nach Phase 1 (bis zum Beginn der Proben). Jede Zeile wird in einem Paket umgesetzt,
getestet und hier auf „erledigt“ gesetzt. Entscheidungen nach Empfehlung (siehe Spalte „Umsetzung“); Änderungen an der
Spezifikation stehen als Ä31 in Kapitel 1.

| # | Paket | Wunsch | Umsetzung | Status |
| --- | --- | --- | --- | --- |
| 1 | A | Presse: Shift-Klick zeigt Eisenbarren im Ausgang (Grafikfehler) | Client-Vorschau beim Shift-Klick korrigieren | erledigt (Paket A) |
| 2 | A | Pumpe schickt kein Öl durch Rohre, Tank nimmt nichts an | Pumpe gibt auf allen Seiten außer vorn ab; Tank nimmt auf allen Seiten an und gibt auf allen ab (ohne Hin- und Herschieben zwischen Tanks) | erledigt (Paket A) |
| 3 | A | Montagetisch craftet nur bei exakter Menge | Startet, sobald genug da ist; Überschuss bleibt liegen | erledigt (Paket A) |
| 4 | A | Kupferrohr: Grafikfehler, verbindet nicht mit Maschinen | Verbindungslogik und Modell prüfen | erledigt (Paket A) |
| 5 | A | Probenzange: Block bleibt stehen | Block wird abgebaut, man bekommt die Probe | erledigt (Paket A) |
| 6 | A | Maschinen starten ohne genug Strom | Vorgang startet erst, wenn Strom für den ganzen Vorgang im Speicher ist | erledigt (Paket A) |
| 7 | A | Tank verliert Inhalt beim Abbauen | Inhalt bleibt im Item, Tooltip „Gefüllt mit Erdöl, X mB“ | erledigt (Paket A) |
| 8 | A | Elektrolyse: beide Gase in Druckflaschen | Eigene Ausgangsseite je Ausgangstank (Tank-Knopf wählt, Seiten-Knopf stellt ein) | erledigt (Paket A) |
| 9 | A | Vorgefüllte Öltanks (Kreativ, Befehl) | Gefüllter Tank im Kreativ-Tab und per `/give` mit Inhalt | erledigt (Paket A) |
| 10 | B | Kohle füllt ganzen Verbrennungsgenerator | Speicher so, dass eine Kohle ihn zur Hälfte füllt | erledigt (Paket B) |
| 11 | B | Pumpe zu schnell | Vorgang 4-mal so lang | erledigt (Paket B) |
| 12 | B | Ölblock beim Abpumpen | Wird mit 60 % Wahrscheinlichkeit verbraucht | erledigt (Paket B) |
| 13 | B | Quarzsand-Adern zu groß | Höchstens 5 Blöcke je Ader, Häufigkeit gleich | erledigt (Paket B) |
| 14 | B | Öl unterirdisch | Kleine Ölpfützen an der Oberfläche statt Blasen | erledigt (Paket B) |
| 15 | B | Maschinen zu laut | Maschinenklänge halb so laut | erledigt (Paket B) |
| 16 | C | Planetenatlas zu groß | Scrollbare Liste, passt auf den Bildschirm | offen |
| 17 | C | „H-Handbook“ oben bei besonderen Items | Tooltip zeigt nur den Namen und Inhalte; der Hinweis auf die Taste H erscheint nur mit gedrückter Umschalttaste | erledigt (Paket A) |
| 18 | C | Akku-Item ohne Füllstand | Tooltip mit Ladung | erledigt (Paket A) |
| 19 | C | Druckflasche ohne Inhalt-Anzeige | Tooltip mit Gas und Füllstand | erledigt (Paket A) |
| 20 | D | Agentur gemeinsam gründen | Einfacher Block ab Spielbeginn zum Gründen und Beitreten | offen |
| 21 | E | Handbuch und Atlas als Buch | Item-Modell aus der Vanilla-Buchtextur (eingefärbt) | offen |
| 22 | E | Erze, Staub, Barren, Öleimer, Quarzsand aus Vanilla-Vorlagen | Vanilla-Texturen werden zur Laufzeit referenziert und eingefärbt (keine Kopie von Mojang-Grafiken im Repo) | offen |
| 23 | E | Tank-Textur mit Füllstand | Füllstand als gerenderte Flüssigkeit im Block | offen |
| 24 | E | Druckflasche als Flasche, Gas erkennbar | Eigenes Item-Modell, Farbe je Gas | offen |
| 25 | E | Silizium-Textur | Überarbeiten | offen |
| 26 | E | Maschinen mit 3D-Modell und Animation | Zuerst passende Texturen, dann Modelle je Maschine | offen |
| 27 | F | FTB Quests als Anleitung mit Belohnungen | FTB Quests gibt es für 26.3 nicht (neueste: 26.1.2); stattdessen Erfolgs-Questreihe mit Belohnungen. FTB-Questdatei, sobald FTB Quests für 26.3 erscheint | offen |
| 28 | F | Erfolge für wichtige Maschinen | Teil der Questreihe | offen |
| 29 | G | Handbuch sehr ausführlich | Jede Maschine: Aufbau mit Bild, Funktion, GUI-Bild mit Erklärung | offen |
