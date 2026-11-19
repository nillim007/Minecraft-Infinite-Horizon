# 20. Logistik und Endgame

## 20.1 Außenposten

- Ein **Außenposten** = ein `cargo_terminal` plus alle Produzenten (`drilling_rig`, `pump_station`, `gas_collector`, `magnetic_trap`) im Umkreis von 16 Blöcken.
- Das Terminal hat 216 Item-Slots (8 × 27, GUI scrollbar), 8 Frachttanks à 64.000 mB und einen separaten Treibstofftank (64.000 mB, nur `methane`). Produzenten geben direkt ins Terminal. Damit passt die volle Ladung eines `heavy_freighter` (8 × 27 Slots, 8 × 64.000 mB) hinein. Eingehendes Methan (Rohr, Leerslot, Produzent) füllt zuerst den Treibstofftank, erst der Überschuss geht in die Frachttanks. Am `shipyard_dock` gilt dasselbe: zuerst Treibstofftank, dann Werftlager.
- **Offline-Produktion:** Jeder Außenposten wird in einer globalen `SavedData` (Overworld) registriert: Position, Dimension, Produzenten, Rate, letzter Tick. Ist der Chunk nicht geladen, wird die Produktion beim nächsten Zugriff (Laden, Logistik-Abfrage, Frachtlandung) nachgerechnet: `vergangene_Ticks × Rate × outpost_offline_factor (0,5)`, begrenzt durch den freien Platz und durch vorhandene Eingaben (z. B. leere `antimatter_cell` der Magnetfalle). Energie wird offline nicht simuliert: Ein Produzent zählt offline nur, wenn er beim Entladen des Chunks den Status `running` hatte. „Rate“ = Ausgabe pro Vorgang ÷ Vorgangsdauer laut 6.1. Ein Produzent gehört immer zum nächstgelegenen `cargo_terminal`. Mit `outposts_offline = false` gibt es keine Offline-Produktion.

## 20.2 Bohrturm

Beim Platzieren zählt der Bohrturm alle Erzblöcke (Tag `c:ores` und Planeten-Erze) im Chunk unter sich. Er erzeugt alle 100 t 1 Erz-Item, zufällig gewichtet nach diesen Anzahlen. Die Welt wird nicht verändert (unendliche Lagerstätte). Ohne Erze: Status `error`.

## 20.3 Frachtraketen

| ID | Laderaum | Tank | Treibstoff pro Flug | Stufe |
| --- | --- | --- | --- | --- |
| `cargo_rocket` | 4 × 27 Slots | 64.000 mB `methane` | 2.000 + 1.500 × \|ΔDP\| mB | 5 |
| `heavy_freighter` | 8 × 27 Slots | 1 `fuel_rod` = 10 Flüge | – | 6 |

- **Zuordnung:** Jedes `cargo_terminal` hat 4 Hangar-Slots. Eine Frachtrakete im Hangar-Slot gehört zu diesem Terminal und kann im Logistik-Computer Routen zugewiesen bekommen. Vor jedem Start tankt sie automatisch aus dem Treibstofftank des Terminals. Routen starten immer am Heimatterminal der Rakete; der Logistik-Computer bietet für eine Route nur Raketen dieses Terminals an. Während des Flugs bleibt das Item im Hangar-Slot und ist gesperrt.
- **`heavy_freighter`:** Data Component `flights_left` (0–10); ein Flug ist Hin- und Rückflug. Steht der Wert auf 0, nimmt der Frachter vor dem Start automatisch 1 `fuel_rod` aus dem Item-Lager des Heimatterminals. Gibt es keinen, bekommt er den Status „ohne Treibstoff“.
- **Fluidfracht:** Neben den Item-Slots hat die `cargo_rocket` 4 Fluid-Laderäume à 64.000 mB (`heavy_freighter`: 8). Fluide lädt sie direkt aus den Fluidtanks des Start-Terminals (Filter der Route) und entlädt in die des Ziel-Terminals bzw. ins Werftlager.
- **Rückflug** immer leer.
- Gestrandete Raketen („ohne Treibstoff“) versuchen alle 1.200 t erneut zu tanken.
- Ein `trading_post` als Ziel nimmt nur Items an; Fluidfracht bleibt in der Rakete.
- **Antriebe nachrüsten:** Rakete + `atomic_drive` bzw. `antimatter_drive` im Montagetisch. Gespeichert in der Data Component `infinite_horizon:drive`; es zählt nur der beste Antrieb.
- **Flugzeit** = 1.000 + 3.950 × |ΔDP| Ticks (Erde → Mond 1.000 t, Erde → Pluto ca. 72.000 t). `atomic_drive` (eingebaut im `heavy_freighter`, nachrüstbar in `cargo_rocket`): Zeit ÷ 2. `antimatter_drive` (nachrüstbar): Zeit ÷ 10.
- Frachtflüge sind **keine fliegenden Entities**. Ein Flug ist ein Datensatz (Rakete, Route, Startzeit, Ankunftszeit, Fracht). Beim Start und bei der Landung spielt das Terminal eine kurze Animation, wenn der Chunk geladen ist.
- Mit vollem Tank (64.000 mB) schafft jede Frachtrakete Hin- und Rückflug zu jedem Ziel (längster: Pluto, 58.000 mB).
- Vor dem Rückflug tankt die Rakete am Ziel aus dessen Treibstofftank nach (`cargo_terminal` und `shipyard_dock` haben je einen, 64.000 mB `methane`). Hat sie Methan als Fracht geladen, behält sie beim Entladen so viel, wie der Rückflug braucht. Reicht es dann nicht, bleibt sie dort (Status „ohne Treibstoff“).
- Der Logistik-Computer warnt schon beim Anlegen einer Route, wenn Hin- plus Rückflug (z. B. Pluto: 2 × 29.000 = 58.000 mB) den Tank übersteigt und am Ziel kein Methan gelagert ist.

## 20.4 Logistik-Computer

`logistics_computer`: GUI mit allen Raketen und Routen der Agentur.

| Feld | Werte |
| --- | --- |
| Start / Ziel | beliebiges `cargo_terminal`, `shipyard_dock` oder freigegebenes Terminal bzw. `trading_post` einer Partneragentur |
| Fracht | Filterliste (Items/Fluide) oder „alles“ |
| Startbedingung | „wenn voll“ · „alle N Ticks“ · „wenn Ziel weniger als N von X hat“ · „bis der Bedarf der Werft gedeckt ist“ |
| Rakete | eine oder mehrere zugewiesene Frachtraketen |

Statusliste: unterwegs (mit Restzeit), geladen, wartend, ohne Treibstoff.

## 20.5 Antriebe

| ID | Wirkung |
| --- | --- |
| `atomic_drive` | Frachtraketen: Flugzeit ÷ 2 |
| `antimatter_drive` | Frachtraketen: Flugzeit ÷ 10; T3 braucht 4 Stück |

## Festlegungen aus der Umsetzung (M1–M8)

Annahmen aus der Umsetzung, die übernommen wurden. Die Nummer verweist auf den Eintrag in `docs/spec-gaps.md`, dort steht auch die Code-Stelle.

- **#111 (M8b, 20.2, 6.1, 17, 18):** Bohrturm, Pumpstation, Ponton: offene Einzelheiten → Bohrturm: „Chunk unter sich“ = die Chunk-Säule vom Grund der Welt bis unter den Turm; Planeten-Erze stehen im Tag `#infinite_horizon:planet_ores` (alle `ores` aus planets.json); Erz-Item = Item des Erzblocks; gezählt wird einmal beim ersten Takt nach dem Setzen; das nächste Erz wird zu Beginn eines Vorgangs gezogen; ohne Erze `error` (auch auf der Erde zuerst der Ortshinweis). Pumpstation: Quelle ist der Block darunter, auf einem Ponton der Block unter dem Ponton; Fluide nur aus Quellblöcken; Bonus aus `reward.pump_station_multiplier` des laufenden Wetters der Dimension. Ponton: 1 Pixel hoch wie ein Teppich, nur über einem Fluid-Quellblock, bricht weg, wenn das Fluid verschwindet; Item wie die Seerose. Ammoniak-Dünger: Rechtsklick auf die Anlagen aus `applies_to`, nutzt die vorhandene Düngung des Hydrokultur-Beckens
- **#112 (M8c, 20.1, 3A.4, 24.4):** Außenposten: offene Einzelheiten → Terminal-GUI: 6 sichtbare Zeilen mit Bildlaufleiste statt 8 Seiten à 27 (gleiche 216 Slots); Klick auf eine Tankleiste wählt den Tank für den Füllslot; Treibstofftank nicht über die Ausgangsseite. Produzenten geben jeden Tick Ausgabeslots und Ausgangstanks ans nächste Terminal im Umkreis (Kugel, Gleichstand: kleinere Position). Schnappschuss (Status, Ausgabe je Vorgang, Dauer, letzter Tick) jede Sekunde; „Status beim Entladen“ = letzter Schnappschuss. Nachgerechnet wird beim ersten Tick des Produzenten nach dem Laden oder nach einer Tick-Pause eines geladenen Chunks (mehr als 2 s ohne Schnappschuss); ist das Terminal dann nicht geladen, wartet er bis zu 1 s, danach geht die Ausbeute in die eigenen Ausgaben. Vorgänge offline nutzen die Rezeptlogik des Produzenten (Eingaben und Platz begrenzen); ein Zufallswert je Block unterscheidet ihn von einem neu gesetzten Block. Logistik-Abfrage und Frachtlandung (8d/8e) rufen dasselbe Nachrechnen auf
- **#113 (M8d, 20.3, 20.5, 25.4):** Frachtraketen und Antriebe: offene Einzelheiten → Hangar-Slots liegen im Terminal auf der Rolle „Formslot“ hinter dem Lager (nur Frachtraketen, gesperrt während des Flugs). Raketen-Treibstoff steht als `infinite_horizon:fluid` im Item; vor dem Start wird bis zur Tankgröße nachgetankt, gestartet wird, wenn die Hinstrecke reicht. Der Frachter verbraucht `flights_left` beim Start (Hin- und Rückflug). Fracht = ganze Stapel aus dem Lager (Filter), je Fluid-Laderaum der Inhalt eines Frachttanks bis zur Laderaumgröße. Am Ziel: erst Methan für den Rückflug behalten, dann entladen, dann aus dem Treibstofftank des Ziels nur den Fehlbetrag tanken. Was am Ziel nicht passt, fliegt zurück und geht ins Heimatlager (sonst fällt es daneben). Ziel- und Heimat-Chunk werden bei der Landung kurz geladen. Wird das Terminal abgebaut, enden seine Flüge (Fracht unterwegs geht verloren). Nachrüst-Rezepte fehlen in recipes.json: erzeugt aus `retrofit_into` in logistics.json (Rakete + Antrieb, `copy_components_from` 0, Ergebnis mit `drive`). „Nur gefüllte antimatter_cell (charge 1)“ steht nur in `notes`: der Generator macht daraus Zutat-Komponenten. Start ohne Logistik-Computer nur über `CargoFlights.launch` (Routen in 8e)
- **#114 (M8e, 20.4, 3A.4, 3A.5, 24.3, 26):** Logistik-Computer: offene Einzelheiten → Routen liegen mit den Flügen in `CargoFlights` (= `CargoData`). „Wenn voll“ = passende Fracht füllt alle Item-Slots oder alle Fluid-Laderäume; ohne passende Fracht startet keine Route. „Alle N Ticks“ zählt ab dem letzten Start; eine neue Route startet sofort. „Bis der Bedarf gedeckt ist“ startet bei Bedarf > 0 des Ziels (Werft, 8f) und lädt je Item höchstens den Bedarf. Status „geladen“ = Hinflug mit Fracht, „unterwegs“ = sonst; „ohne Treibstoff“ am Start: neuer Versuch nach `stranded_retry_ticks` (nicht gespeichert). Routen mit nicht geladenem Start werden nur alle `stranded_retry_ticks` geprüft; dabei und bei der Landung wird der Außenposten nachgerechnet. Eine Rakete gehört zu höchstens einer Route. Ziele in 8e nur eigene Frachtterminals (Werftdock 8f, Partner und Handelsposten M9). Der Block hat keine Block-Entity (die Routen gehören der Agentur). Erfolg `network` fehlt in misc.json und kommt mit M9
