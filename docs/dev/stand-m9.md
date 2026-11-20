# Stand Meilenstein 9 (Feinschliff)

Teilschritte 9a–9g sind umgesetzt (Aufteilung in `docs/handbook/handbook.json`, Lücke #117). Alle Tests laufen auf beiden Loadern in einem Durchlauf (Server-GameTests je Loader, JUnit, Python-Tests der Werkzeuge, Fabric- und NeoForge-Client-Test, Spieltests M7–M9 im Fabric-Client). `python tools/gen_resources.py` und `python tools/gen_wiki.py --check` erzeugen keine Änderungen.

## Abnahmekriterium (Kapitel 27)

| Kriterium | Prüfung |
| --- | --- |
| **`tools/balance_sheet.py` liest alle Rezept-JSONs und gibt die Rohstoffsumme je Rakete und Werftabschnitt aus** (Rohstoff = ohne eigenes Rezept, erstes Rezept nach ID, Nebenprodukte und Pressformen ignoriert, Mengen durch Ausgabemenge, Fluide in mB, Tags erstes Element) | `tools/test_balance_sheet.py` (Regeln einzeln und mit den echten Daten); die CI rechnet die Übersicht in jedem Lauf und zeigt sie in der Zusammenfassung des Jobs „Spezifikation prüfen“ |

## Teilschritte

| Teilschritt | Tests |
| --- | --- |
| 9a Agentur: Rollenwechsel, Leitung übergeben, Umbenennen | `agency_guest_role_rights`, JUnit `AgencyDataTest` |
| 9b Partner, Freigaben, Handelsposten, Fracht zu Partnern, gemeinsame Werft | `agency_partner_both_leaders`, `trading_post_trade_anyone`, `cargoflight_partner_delivery`, `shipyard_joint_contributors`, JUnit `TradingRulesTest`, `AgencyDataTest` |
| 9c Erfolge: alle 14 aus 24.3, Protokoll | `advancements_all_loaded`, `advancement_venus_return`, `advancement_sun_in_a_bottle`, `advancement_network_routes`, JUnit `VenusDescentTest`, `RouteRulesTest` |
| 9d Klang: Anzug-Schleifen, Dämpfung im Vakuum, Maschinen, Musik je Planet | JUnit `SoundDampingTest`; Client-Test (Mond: Vakuum und Schleifen, in der Kuppel aus) |
| 9e Handbuch: Blaupausen-Optik, Planetenseiten aus dem Atlas, englische Texte | Client-Test (Handbuch-Bilder, gesperrte Planetenseite); `gen_resources.py` warnt bei abweichendem Aufbau der englischen Seiten |
| 9f Balancing-Skript | oben |
| 9g Abnahme | Spieltest M9 |

## Spieltest im Client

Ein automatischer Spieltest im echten Fabric-Client (`fabric/.../gametest/IhClientPlaytestM9.java`, läuft in der CI mit dem Client-Test; lokal allein: `./gradlew :fabric:runClientGameTest -PclientTestOnly=playtest_m9`) prüft mit echten Mausklicks und macht Bilder (`play9_*`):

1. Forschungsterminal: Reiter „Agentur“ per Klick, Rolle eines Mitglieds wechseln, Agentur über das Textfeld umbenennen.
2. Reiter „Partner“: Angebot einer anderen Agentur annehmen.
3. Mars: Freigabe am eigenen Frachtterminal per Knopf; am Handelsposten der Partner-Agentur ohne Operator-Rechte (Lager unsichtbar) tauschen; Frachtflug vom eigenen Terminal zum Handelsposten des Partners.
4. Erfolge „Rote Erde“ (Mars) und „Drei Minuten Hölle“ (Venus unter die Wolken und zurück); Bild des Erfolge-Reiters.
5. Mond: Vakuum erkannt, Atem- und Funkschleife; ein laufender Zerkleinerer spielt sein Geräusch.
6. Englisches Handbuch: die Mars-Seite nach dem Besuch mit Atlas-Kasten.

Gefunden und behoben in M9:

- Forschungs- und Frachtterminal fragten die linke Maustaste mit 0 ab (26.3: 1); Reiter, Knoten und Tankauswahl reagierten nicht auf echte Klicks (9b).
- Erfolgs-Bedingungen haben in 26.3 ein neues Format (eine Loot-Bedingung mit `type`, Komponenten-Schlüssel im Prädikat), sonst startet der Server nicht (9c).
- Klang-Schleifen endeten ohne Audiogerät nicht (die Engine tickt nur gespielte Klänge, 9d).
- Ohne Planetenfeld galt jede Handbuchseite als gesperrte Planetenseite (9e, vor dem Commit im Client-Test gefunden).
- Im Agentur-Reiter überdeckte ein langer Mitgliedsname die Knöpfe „Gast“ und „Leiter“; der Name endet jetzt davor (9g, Spieltest M9).

## Nicht enthalten

Echte Klänge und Musik (Platzhalter aus Vanilla, Lücke #121, offene Punkte in Kapitel 28), Modelle für Sternenschiff und Gerüst, weitere Sternensysteme. Die Annahmen #117–#123 (und alle früheren) sind seit 19.11.2026 in die Spezifikation übernommen; offen bleiben nur die Punkte in Kapitel 28, die Menschen brauchen (Spieltest nach `docs/dev/testplan-balancing.md`, Grafik, Klang, Gegenlesen der englischen Namen).
