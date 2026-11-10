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
- **Antriebe nachrüsten:** Rakete + `atomic_drive` bzw. `antimatter_drive` im Montagetisch. Gespeichert in der Data Component `sternenwerft:drive`; es zählt nur der beste Antrieb.
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
