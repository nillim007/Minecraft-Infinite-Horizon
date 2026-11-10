# 1. Änderungen gegenüber dem Design-Dokument

Diese Entscheidungen lösen Widersprüche und Sackgassen. **Bitte prüfen**, bevor die Umsetzung beginnt.

| # | Problem im Design-Dokument | Lösung in dieser Spezifikation |
| --- | --- | --- |
| Ä1 | Anzug T2 brauchte Wolfram, Wolfram gibt es nur auf der Venus-Oberfläche, dafür braucht man Anzug T2 | Anzug T2 nutzt **Keramikplatten** (Basalt + Aluminiumstaub im Hochtemperaturofen). Säureschutz bleibt. |
| Ä2 | Kühlsystem (Stufe 2) brauchte flüssiges Helium, der Kryo-Verflüssiger kam erst in Stufe 3 | **Kryo-Verflüssiger nach Stufe 2** verschoben. Das Kühlsystem braucht 1.000 mB statt eines ganzen Tanks flüssiges Helium. |
| Ä3 | Anzug T3 brauchte Isoliergewebe vom Frostkriecher (Triton, nur mit T3 betretbar) | Isoliergewebe ist **herstellbar** (Kunststoff, Silizium, Lithium). Der Frostkriecher lässt es zusätzlich fallen. |
| Ä4 | Der Boss sollte optional sein, die Sternenkarte aber Pflicht | Der Wächter des Senders ist **Pflicht** für die Sternenkarte. Er kann pro Agentur erneut beschworen werden. |
| Ä5 | Säureregen: Schutz nur durch Kuppel T3 (Stufe 4), Venus kommt aber in Stufe 2 | **Kuppel T2 schützt vor Säureregen**. |
| Ä6 | Kältegrenzen der Anzüge passten nicht zu Merkur, Europa und dem Mond | Neue Grenzwerte in Kapitel 9 und Planetentemperaturen in Kapitel 13. |
| Ä7 | Unklar, welche Rakete welche Ziele erreicht | Feste Zieltabelle (12.1) und Treibstoffformel (12.2). |
| Ä8 | Begründung für Wasserstoff beim Fernflug-Modul war nicht schlüssig | Entfernt. Das Fernflug-Modul nutzt Wasserstoff und Sauerstoff wie die T2. |
| Ä9 | „Kühlmodul im Umkehrbetrieb“ und „Heizmodul“ | Zwei getrennte Module: `cooling_module` (Stufe 2) und `heating_module` (Stufe 5). |
| Ä10 | „Kleiner Kernreaktor“ nicht definiert | Entfernt. Außenposten nutzen `fission_reactor` oder Solar + `lithium_battery_bank`. |
| Ä11 | Orbit für jeden Planeten und Mond (zu viele Dimensionen) | Orbit-Dimensionen nur für 8 Planeten (ohne Pluto). Monde haben keinen eigenen Orbit: Raketen fliegen direkt zu ihnen, die Landefähre verbindet sie mit dem Orbit ihres Planeten. |
| Ä12 | Erdöl-Förderung in Phase 1 fehlte | Neue Maschine `fluid_pump` (Phase 1). |
| Ä13 | Energiebedarf von Kuppel T3 (1.480 J/t inkl. Sauerstoff) über einem Fusionsreaktor | Beibehalten, aber dokumentiert. Neues `superconductor_cable` (32.768 J/t), weil Aluminiumkabel für das Endgame nicht reicht. |
| Ä14 | Venus: wie man in die Wolkenschicht kommt und wieder hinunter | Automatische Aerostat-Plattform bei der ersten Landung, dazu ein `cloud_elevator` für Abstiege (12.8). |
| Ä15 | Proben waren nicht definiert | Proben entstehen mit der `sample_tool`. Probenart = Block-ID (Kapitel 11). |
| Ä16 | Drops ohne Verwendung | Jeder Drop hat jetzt ein Rezept oder einen Zweck (Kapitel 18). |
| Ä17 | Nickel und Gips ohne Verwendung | Nickel in Lithium-Akku, Bohrturm und Magnetstiefeln, Gips im Kompost-Boden. |
| Ä18 | Treibstoff reichte für Mars (T1), Io, Europa und das äußere System nicht für Hin- und Rückflug | T1 3–5 Tanks, T2 4–8 Tanks, Fernflug-Modul +128.000 mB je Komponente (Lesart „8 zusätzliche Tanks je Komponente“), Schub T2 = 48. Rückflugtabelle in 12.2. |
| Ä19 | Fortgeschrittene Sonde brauchte Iridium-Chips, Iridium gibt es aber erst dort, wo die Sonde hinführt | Sonde braucht 4 Fortgeschrittene Schaltkreise statt 2 Iridium-Chips. |
| Ä20 | Säureregen und Staubsturm (Stufe 2) brauchten Kuppel T2 (Stufe 3) | Kuppel T2 nach Stufe 2 verschoben. |
| Ä21 | Bewegung in der Station ohne Jetpack (Stufe 4) war undefiniert | Magnetstiefel kommen mit dem Stationskern (Stufe 3) und brauchen Nickel statt Supraleiter. |
| Ä22 | Frachtraketen (32.000 mB) schafften Hin- und Rückflug ins äußere System nicht, und die Werft konnte nicht betanken | Frachtrakete mit 64.000 mB Methan. `cargo_terminal` mit 216 Slots, 8 Frachttanks und eigenem Treibstofftank. |
