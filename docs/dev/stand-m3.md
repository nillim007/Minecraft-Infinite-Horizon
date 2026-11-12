# Stand Meilenstein 3 (Erste Welt)

Teilschritte 3a–3e sind umgesetzt (27.5). Alle M3-Tests laufen auf beiden Loadern in einem Durchlauf (74 Server-GameTests je Loader, JUnit, Fabric- und NeoForge-Client-Test). `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen.

## Abnahmekriterien (Kapitel 27)

| Kriterium | Test |
| --- | --- |
| Ohne Anzug im Override Mond: nach 190 t unter 100 %, nach 200 t 100 % Sauerstoff- und Druck-Belastung | `environment_moonoverride_hazardfullafter200ticks` |
| Spieler in aktiver Kuppel im Override Mond hat `breathable` | `dome_moonoverride_breathable` |
| Kuppel mit 1 mB O₂: nach 24 t `failing`, 600 t später aus, danach nicht mehr `breathable` | `dome_1mb_failing24_off600` |
| Fackel außerhalb der Kuppel nicht setzbar, innerhalb schon | `dome_torch_onlyinside` |
| Schwerkraft 0,0136 und sichere Fallhöhe 17,6 (Override und echte Dimension) | `gravity_moonoverride_gravityandsafefall`, `moon_player_gravitywithoutoverride` |
| Anzug mit 600 mB: nach 200 t 0 % Belastung, 590 mB | `suit_moonoverride_nostrain590mb` |
| Leere Flasche im Füllslot des Sauerstoffgenerators nach 1 Tick voll | `oxygengenerator_fillslot_canisterfullafter1tick` |
| `moon_aluminum_ore` droppt 1 `bauxite` | `moon_aluminumore_dropsonebauxite` |
| Agentur: Besitz beim Platzieren, kein Besitz per `setBlock`, `merged_into` | `agency_*` |

Screenshots (Branch `ci-screenshots`): Mondgelände, Monster mit Atemmaske, Erde am Himmel, HUD ohne und mit Anzug, Kuppel.

## Manuell zu prüfen

1. `/infinite_horizon goto moon` in einer neuen Welt: Landung auf festem Boden, schwarzer Himmel mit Erde, Sterne am Tag.
2. Ohne Anzug auf dem Mond: HUD-Leisten erscheinen mit Warnton, nach 10 s Schaden; mit Anzug und Flasche kein Schaden.
3. Kuppelgenerator mit Strom und Sauerstoffgenerator daneben: Lichtpunkte auf der Hülle, drinnen atmen, Fackel setzen, Bett benutzen; Strom kappen: rote Punkte, Warnung in der Aktionsleiste, nach 30 s aus.
4. Fackeln draußen erlöschen nach einiger Zeit zu erloschenen Fackeln; Wasser aus dem Eimer wird Eis.
5. Mondlandschaft auf Großkrater (Zentralberg) und Lavaröhren absuchen (`/locate structure infinite_horizon:great_crater`).
6. NeoForge: neue Welt ohne Hinweis „Experimentelle Einstellungen“.

## Bekannte Grenzen

- Kuppelhülle als Partikel statt halbtransparenter Fläche (Spec-Gap #60).
- Warntöne sind Notenblock-Klänge (Platzhalter, 24.6).
- Agentur-Rechte und Kuppelschutz wirken ab M4 (23.1).
