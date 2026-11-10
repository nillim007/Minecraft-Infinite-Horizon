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
- **Fremde:** Nicht-Mitglieder haben an Block-Entities fremder Agenturen überall Gast-Rechte, auch außerhalb von Kuppeln.

| Rolle | Rechte |
| --- | --- |
| `leader` | alles, einladen, Rollen vergeben, Partner freigeben |
| `engineer` | bauen, Maschinen bedienen, Raketen starten, Routen ändern, forschen |
| `guest` | Kuppeln betreten, Container öffnen (Slot-Klicks werden serverseitig verworfen), nichts abbauen oder setzen |

## 23.2 Schutz

- In aktiven Kuppeln dürfen nur Mitglieder der Besitzer-Agentur (ab `engineer`) abbauen und setzen. Alle Spieler können in jeder Kuppel atmen.
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
