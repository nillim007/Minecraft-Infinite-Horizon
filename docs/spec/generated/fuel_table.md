<!-- Erzeugt von tools/render_docs.py aus spec/data. Nicht von Hand ändern. -->

# Treibstoff für Hin- und Rückflug

Formel (Kapitel 12.2): Start = 32.000 × g, Transfer = 6.000 × |ΔDP| + 2.000 bei Körperwechsel, Landung = 0,25 × 32.000 × g. Werte je Treibstoffkomponente in mB, Hin- und Rückflug.

| Ziel | ID | ab Erdoberfläche | ab Station im Erdorbit | Mindestausstattung ab Erde |
| --- | --- | --- | --- | --- |
| Erdorbit | `earth_orbit` | 36.000 | – | T2, 4 Tanks |
| Mond | `moon` | 42.800 | 10.800 | T1, 3 Tanks |
| Venus | `venus` | 64.000 | 32.000 | T1, 5 Tanks (4 ohne Reserve) |
| Venus-Orbit | `venus_orbit` | 48.000 | 16.000 | T2, 4 Tanks |
| Mars | `mars` | 69.200 | 37.200 | T1, 5 Tanks |
| Mars-Orbit | `mars_orbit` | 54.000 | 22.000 | T2, 4 Tanks |
| Merkur | `mercury` | 75.200 | 43.200 | T2, 5 Tanks |
| Merkur-Orbit | `mercury_orbit` | 60.000 | 28.000 | T2, 4 Tanks |
| Asteroidengürtel | `asteroid_belt` | 72.960 | 40.960 | T2, 5 Tanks |
| Ceres | `ceres` | 73.200 | 41.200 | T2, 5 Tanks |
| Europa | `europa` | 101.200 | 69.200 | T2, 7 Tanks |
| Io | `io` | 103.200 | 71.200 | T2, 7 Tanks |
| Jupiter-Orbit | `jupiter_orbit` | 96.000 | 64.000 | T2, 7 Tanks (6 ohne Reserve) |
| Enceladus | `enceladus` | 132.400 | 100.400 | T2 + Modul, 4 Tanks |
| Saturn-Orbit | `saturn_orbit` | 132.000 | 100.000 | T2 + Modul, 4 Tanks |
| Titan | `titan` | 136.480 | 104.480 | T2 + Modul, 4 Tanks |
| Uranus-Orbit | `uranus_orbit` | 180.000 | 148.000 | T2 + Modul, 4 Tanks |
| Neptun-Orbit | `neptune_orbit` | 216.000 | 184.000 | T2 + Modul, 6 Tanks |
| Triton | `triton` | 219.200 | 187.200 | T2 + Modul, 6 Tanks |
| Pluto | `pluto` | 254.400 | 222.400 | T2 + Modul, 8 Tanks |
