# 23. Mehrspieler und Agenturen

## 23.1 Agentur

- Globale `SavedData` (Overworld): Agenturen mit ID, Name, Mitgliedern (UUID, Rolle), FP, erforschten Knoten, Atlas, Sondenzielen, Routen, Terminals, Partnerschaften.
- Jeder Spieler bekommt beim ersten Join eine eigene Agentur („Agentur von <Name>“).
- Gründen, Einladen, Beitreten, Austreten und Rollen vergeben am `research_terminal` (Reiter „Agentur“). Beim Beitritt verlässt man die alte Agentur, ihre Fortschritte bleiben dort.
- **Geteilt:** FP, Forschung, Atlas, Sondenergebnisse, Routen, Stationen, Außenposten.
- **Pro Spieler:** Inventar, Anzug, Klon-Kammer-Bindung, Advancements.
- **Besitz:** Jede Block-Entity und jedes Fahrzeug speichert beim Platzieren `owner_agency` (Agentur des platzierenden Spielers). Darauf beziehen sich Kuppelschutz, Tore (11.4) und Logistik.
- **Lebenszyklus:**
  - Tritt der Leiter aus, wird das am längsten beigetretene `engineer`-Mitglied Leiter, sonst das am längsten beigetretene Mitglied.
  - **Beitritt:** Wird die alte Agentur durch einen Beitritt leer, geht ihr gesamter Besitz automatisch an die neue Agentur über (Block-Entities, Fahrzeuge, Kuppeln, Stationen, Terminals, Routen). Technisch speichert die alte Agentur `merged_into`, und `owner_agency` wird beim nächsten Zugriff umgeschrieben. FP, Forschung und Atlas werden **nicht** übertragen, es gilt der Stand der neuen Agentur.
  - Eine Agentur, die auf andere Weise leer wird (alle verlassen den Server dauerhaft), bleibt ruhend bestehen und kann über den Server-Befehl `/sternenwerft agency transfer <von> <an>` einer anderen Agentur übertragen werden.
  - Ehemalige Mitglieder kann man wieder einladen.
- **Fremde:** Nicht-Mitglieder haben an Block-Entities fremder Agenturen überall Gast-Rechte, auch außerhalb von Kuppeln. Das wirkt ab M4 (siehe „Agentur-Kern“ unten).

| Rolle | Rechte |
| --- | --- |
| `leader` | alles, einladen, Rollen vergeben, Partner freigeben |
| `engineer` | bauen, Maschinen bedienen, Raketen starten, Routen ändern, forschen |
| `guest` | Kuppeln betreten, Container öffnen (Slot-Klicks werden serverseitig verworfen), nichts abbauen oder setzen |

**Agentur-Kern (M3):**

- **Daten:** Agentur-ID = UUID. `AgencyData` (3A.4) speichert in M3 je Agentur `id`, `name`, `members` (UUID, Rolle, Beitritts-Tick), `merged_into` und `visited_dimensions`, dazu `data_version` 1. Spätere Felder sind optional und haben einen Standardwert.
- **Name:** `name` = `null` heißt Anzeige „Agentur von <Name>“ über den Schlüssel `agency.sternenwerft.default_name` mit dem Namen des Gründers.
- **Entstehung:** Bei jedem Login prüft der Server, ob der Spieler zu einer Agentur gehört. Wenn nicht, entsteht eine neue mit ihm als `leader`. Das gilt auch für Spieler aus Welten vor M3.
- **Besuche:** `visited_dimensions` wird beim Dimensionswechsel gefüllt (auch über den Befehl `goto`, 12.10). M4 liest es für das Tor `enter_dimension` (25.3) und den Entdecker (23.3).
- **Befehle:** `/sternenwerft agency info [<spieler>]` (alle Spieler) und `/sternenwerft agency transfer <von> <an>` (Rechtestufe 2).
- **Rechte:** M3 speichert Besitz nur. Die Prüfung läuft über `Permissions.can(player, owner, action)` mit dem Standard `true`. Gast-Rechte und Kuppelschutz (23.2, 10.3) wirken ab M4, wenn man am `research_terminal` beitreten kann.
- **Client:** In M3 gibt es keinen Agentur-Sync; `AgencyStatePayload` kommt mit M4.
- **Ohne Besitzer:** Blöcke ohne `owner_agency` (aus Welten vor M3, gesetzt per Befehl, Werfer, Struktur oder GameTest) sind ohne Besitzer. In M3 hat das keine Wirkung. Ab M4 wird der erste Spieler, der das GUI öffnet, mit seiner Agentur Besitzer; bis dahin gilt die Rezeptsperre als offen, und der Block zählt für keine Tore.
- Besitz haben nur Blöcke mit Block-Entity (3A.3); Kabel, Rohre und Gehäuse nicht.

## 23.2 Schutz

- In aktiven Kuppeln dürfen nur Mitglieder der Besitzer-Agentur (ab `engineer`) abbauen und setzen (ab M4). Alle Spieler können in jeder Kuppel atmen.
- Raketen und Rover darf jeder besteigen, starten nur Mitglieder der Besitzer-Agentur.

## 23.3 Zwischen Agenturen

- `trading_post`: Block mit bis zu 8 Angeboten (gib X, erhalte Y) und eigenem Lager (54 Slots). Jeder Spieler kann tauschen, auch Gäste und Nicht-Mitglieder (Ausnahme von den Gast-Rechten in 23.1). Frachtraketen von Partneragenturen können ihn als Ziel nutzen.
- **Partnerschaft:** Leiter beider Agenturen bestätigen am Terminal. Partner dürfen freigegebene `cargo_terminal`s und Werften beliefern.
- **Gemeinsame Werft:** Alle Agenturen, die mindestens 1 Item geliefert haben, bekommen beim Start das Advancement.
- **Wettrennen:** Die erste Agentur, deren Mitglied ein Ziel betritt, wird serverweit angekündigt und im Atlas als Entdecker eingetragen.

## 23.4 Sitze

T1: 1, T2: 2, Landefähre: 2, Rover: 2, T3: 8.

## 23.5 Serverleistung

- Planetendimensionen werden nur geladen, solange dort ein Spieler ist oder eine Rakete landet (keine Force-Loads durch die Mod, außer 1 Chunk für 200 t bei Landungen).
- Kuppelprüfung: alle 10 t pro Spieler zusammen mit den Umweltwerten (8.1), nur gegen Kuppeln derselben Dimension.
- Frachtflüge und Außenposten nur rechnerisch (Kapitel 20).
