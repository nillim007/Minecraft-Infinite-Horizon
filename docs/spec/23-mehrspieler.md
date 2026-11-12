# 23. Mehrspieler und Agenturen

## 23.1 Agentur

- Globale `SavedData` (Overworld): Agenturen mit ID, Name, Mitgliedern (UUID, Rolle), FP, erforschten Knoten, Atlas, Sondenzielen, Routen, Terminals, Partnerschaften.
- Jeder Spieler bekommt beim ersten Join eine eigene Agentur („Agentur von <Name>“).
- Einladen, Beitreten und Austreten am `research_terminal` (Reiter „Agentur“, ab M4); Rollen vergeben, Umbenennen und Partner ab M9 (siehe „Beitritt und Rechte“ unten). Beim Beitritt verlässt man die alte Agentur, ihre Fortschritte bleiben dort.
- **Geteilt:** FP, Forschung, Atlas, Sondenergebnisse, Routen, Stationen, Außenposten.
- **Pro Spieler:** Inventar, Anzug, Klon-Kammer-Bindung, Advancements.
- **Besitz:** Jede Block-Entity und jedes Fahrzeug speichert beim Platzieren `owner_agency` (Agentur des platzierenden Spielers). Darauf beziehen sich Kuppelschutz, Tore (11.4) und Logistik.
- **Lebenszyklus:**
  - Tritt der Leiter aus, wird das am längsten beigetretene `engineer`-Mitglied Leiter, sonst das am längsten beigetretene Mitglied.
  - **Beitritt:** Wird die alte Agentur durch einen Beitritt leer, geht ihr gesamter Besitz automatisch an die neue Agentur über (Block-Entities, Fahrzeuge, Kuppeln, Stationen, Terminals, Routen). Technisch speichert die alte Agentur `merged_into`, und `owner_agency` wird beim nächsten Zugriff umgeschrieben. FP, Forschung und Atlas werden **nicht** übertragen, es gilt der Stand der neuen Agentur.
  - Eine Agentur, die auf andere Weise leer wird (alle verlassen den Server dauerhaft), bleibt ruhend bestehen und kann über den Server-Befehl `/infinite_horizon agency transfer <von> <an>` einer anderen Agentur übertragen werden.
  - Ehemalige Mitglieder kann man wieder einladen.
- **Fremde:** Nicht-Mitglieder haben an Block-Entities fremder Agenturen überall Gast-Rechte, auch außerhalb von Kuppeln. Das wirkt ab M4 (siehe „Beitritt und Rechte“ unten).

| Rolle | Rechte |
| --- | --- |
| `leader` | alles, einladen, Rollen vergeben, Partner freigeben |
| `engineer` | bauen, Maschinen bedienen, Raketen starten, Routen ändern, forschen |
| `guest` | Kuppeln betreten, Container öffnen (Slot-Klicks werden serverseitig verworfen), nichts abbauen oder setzen |

**Agentur-Kern (M3):**

- **Daten:** Agentur-ID = UUID. `AgencyData` (3A.4) speichert in M3 je Agentur `id`, `name`, `members` (UUID, Rolle, Beitritts-Tick), `merged_into` und `visited_dimensions`, dazu `data_version` 1. Spätere Felder sind optional und haben einen Standardwert.
- **Name:** `name` = `null` heißt Anzeige „Agentur von <Name>“ über den Schlüssel `agency.infinite_horizon.default_name` mit dem Namen des Gründers.
- **Entstehung:** Bei jedem Login prüft der Server, ob der Spieler zu einer Agentur gehört. Wenn nicht, entsteht eine neue mit ihm als `leader`. Das gilt auch für Spieler aus Welten vor M3.
- **Besuche:** `visited_dimensions` wird beim Dimensionswechsel gefüllt (auch über den Befehl `goto`, 12.10). M4 liest es für das Tor `enter_dimension` (25.3) und den Entdecker (23.3).
- **Befehle:** `/infinite_horizon agency info [<spieler>]` (alle Spieler) und `/infinite_horizon agency transfer <von> <an>` (Rechtestufe 2).
- **Rechte:** M3 speichert Besitz nur. Die Prüfung läuft über `Permissions.can(player, owner, action)` mit dem Standard `true`. Gast-Rechte und Kuppelschutz (23.2, 10.3) wirken ab M4, wenn man am `research_terminal` beitreten kann.
- **Client:** In M3 gibt es keinen Agentur-Sync; `AgencyStatePayload` kommt mit M4.
- **Ohne Besitzer:** Blöcke ohne `owner_agency` (aus Welten vor M3, gesetzt per Befehl, Werfer, Struktur oder GameTest) sind ohne Besitzer. In M3 hat das keine Wirkung. Ab M4 wird der erste Spieler, der das GUI öffnet, mit seiner Agentur Besitzer; bis dahin gilt die Rezeptsperre als offen, und der Block zählt für keine Tore.
- Besitz haben nur Blöcke mit Block-Entity (3A.3); Kabel, Rohre und Gehäuse nicht.

**Beitritt und Rechte (ab M4):** Werte in `misc.json` (`agency_rules`).

- **Reiter „Agentur“** im `research_terminal`: Name, Mitglieder mit Rolle und die FP der Agentur. Aktionen über `AgencyActionPayload` (3A.5).
- **Einladen:** Der `leader` lädt Spieler ein, die online sind. Die Einladung steht in `AgencyData.invites`, bis sie angenommen oder abgelehnt wird. Der Eingeladene bekommt einen Chat-Hinweis (`message.infinite_horizon.agency.invited`).
- **Annehmen** geht an jedem `research_terminal`, auch an einem fremden (dort hat man das Gast-Recht `OPEN`). Es gilt die Regel „Beitritt“ oben (`merged_into`).
- **Austreten** erzeugt sofort eine neue eigene Agentur mit dem Spieler als `leader`; die Nachfolge des Leiters gilt wie oben.
- **Rollen** sind in M4 fest: der Gründer ist `leader`, Beigetretene sind `engineer`. Rollenwechsel, Umbenennen, Partner und Handel kommen mit M9.
- **Rechte** (`Permissions`, Aktionen `OPEN`, `USE_SLOTS`, `CONFIGURE`, `BUILD`, `BREAK`, `LAUNCH`): Mitglieder dürfen alles; nur der `leader` lädt ein. Nicht-Mitglieder dürfen an fremden Block-Entities nur `OPEN`; ihre Slot-Klicks verwirft der Server. Ohne Besitzer ist alles erlaubt; wer das GUI zuerst öffnet, wird Besitzer (oben). Operatoren (Rechtestufe 2) sind ausgenommen wie beim Vanilla-Spawnschutz.
- **Kuppelschutz:** In einer aktiven fremden Kuppel dürfen Nicht-Mitglieder keinen Block setzen oder abbauen, auch keinen ohne Block-Entity (23.2, 10.3).
- **Forschen** (`ResearchUnlockPayload`) dürfen Mitglieder ab der Rolle `engineer` (11.3).

**Agentur-Felder ab M4:** Alle Felder sind optional mit leerem Standard; `data_version` bleibt 1 (3A.4).

| Feld | Inhalt |
| --- | --- |
| `fp` | FP in Hundertsteln (11.1) |
| `researched` | erforschte Knoten |
| `open_tiers` | offene Stufen (11.4) |
| `probe_targets` | Ziele, zu denen eine Sonde fliegen darf (aus Knoten, 25.3) |
| `probe_reached` | Ziele, an denen eine Sonde angekommen ist (Bedingung für Raketen, 12.1) |
| `probe_flights` | laufende Sondenflüge: Ziel, Ankunft in Overworld-Zeit (12.3) |
| `sample_counts` | Probenart → Zahl analysierter Proben (11.1) |
| `structure_counts` | Strukturtyp → Zahl gefundener Instanzen (11.1) |
| `found_structures` | gefundene Erkundungsziele: Dimension, Struktur, Start-Chunk (11.1, 24.2) |
| `visited_biomes` | Dimension → besuchte Biome (24.2) |
| `beacons` | Landefeuer: Dimension, Position, Name (12.6) |
| `invites` | offene Einladungen (Spieler-UUID) |
| `pending_advancements` | Spieler → Erfolg-IDs, die beim nächsten Login vergeben werden (24.3) |

Dazu oben in `AgencyData` (nicht je Agentur): `discoverers` (Dimension → Entdecker-Agentur, 23.3). `AgencyStatePayload` geht bei Join und bei jeder Änderung an alle Online-Mitglieder (3A.5).

## 23.2 Schutz

- In aktiven Kuppeln dürfen nur Mitglieder der Besitzer-Agentur (ab `engineer`) abbauen und setzen (ab M4), auch Blöcke ohne Block-Entity. Operatoren (Rechtestufe 2) sind ausgenommen. Alle Spieler können in jeder Kuppel atmen.
- Raketen und Rover darf jeder besteigen, starten nur Mitglieder der Besitzer-Agentur. Bei Raketen zählt auf der Rampe der Besitzer von `launch_control`, bei einer gelandeten Rakete ihr eigener Besitzer (12.5).

## 23.3 Zwischen Agenturen

- `trading_post`: Block mit bis zu 8 Angeboten (gib X, erhalte Y) und eigenem Lager (54 Slots). Jeder Spieler kann tauschen, auch Gäste und Nicht-Mitglieder (Ausnahme von den Gast-Rechten in 23.1). Frachtraketen von Partneragenturen können ihn als Ziel nutzen.
- **Partnerschaft:** Leiter beider Agenturen bestätigen am Terminal. Partner dürfen freigegebene `cargo_terminal`s und Werften beliefern.
- **Gemeinsame Werft:** Alle Agenturen, die mindestens 1 Item geliefert haben, bekommen beim Start das Advancement.
- **Wettrennen:** Die erste Agentur, deren Mitglied ein Ziel betritt, wird serverweit angekündigt und im Atlas als Entdecker eingetragen.
  - Entdecker ist die erste Agentur, deren Mitglied die Dimension ab M4 betritt, auch per `goto` (12.10). Besuche aus M3 zählen nur für das Tor `enter_dimension` (25.3).
  - Die Ankündigung `message.infinite_horizon.discovered` geht an alle Spieler; der Eintrag steht in `AgencyData.discoverers`.

## 23.4 Sitze

T1: 1, T2: 2, Landefähre: 2, Rover: 2, T3: 8.

## 23.5 Serverleistung

- Planetendimensionen werden nur geladen, solange dort ein Spieler ist oder eine Rakete landet (keine Force-Loads durch die Mod, außer 1 Chunk für 200 t bei Landungen).
- Kuppelprüfung: alle 10 t pro Spieler zusammen mit den Umweltwerten (8.1), nur gegen Kuppeln derselben Dimension.
- Frachtflüge und Außenposten nur rechnerisch (Kapitel 20).
