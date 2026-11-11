"""Wandelt die wichtigsten Fehler aus einem Gradle-Log in GitHub-Anmerkungen um.

Anmerkungen lassen sich ohne Anmeldung über die GitHub-API lesen (tools/ci_status.py); das Log selbst nicht.
GitHub zeigt höchstens 10 Fehler und 10 Warnungen je Schritt – deshalb wird hier zusammengefasst.
"""
import re
import sys


def esc(s):
    return s.replace("%", "%25").replace("\r", "").replace("\n", "%0A")


def main():
    try:
        lines = open(sys.argv[1], encoding="utf-8", errors="replace").read().splitlines()
    except OSError:
        print("::error::build.log fehlt")
        return

    errors = []
    # 1) Compiler-Fehler (javac) mit Datei, Zeile und der Folgezeile (meist das Symbol)
    rx = re.compile(r"^(/.*?/(common|fabric|neoforge)/.*?\.java):(\d+): error: (.*)$")
    for i, line in enumerate(lines):
        m = rx.match(line)
        if m:
            path = m.group(1)[m.group(1).index(m.group(2) + "/"):]
            detail = " | ".join(x.strip() for x in lines[i + 1:i + 4] if x.strip() and not rx.match(x))
            errors.append((path, m.group(3), f"{m.group(4)} || {detail}"))
    # 2) Fehlgeschlagene Tests
    tests = [l.strip() for l in lines if " FAILED" in l and ">" in l]
    # 3) Gradle-Zusammenfassung
    summary, on = [], False
    for l in lines:
        if "What went wrong:" in l:
            on = True
            continue
        if on and (l.startswith("* Try:") or l.startswith("* Exception is:")):
            on = False
        if on and l.strip():
            summary.append(l.strip())

    # 4) GameTest-Fehler: Zeilen mit gametest/test + Fehlerwort, dazu 3 Folgezeilen
    gt_rx = re.compile(r"failed|fail|timed out|exception|error", re.I)
    gt_hits = []
    for i, l in enumerate(lines):
        low = l.lower()
        if ("gametest" in low or "test" in low) and gt_rx.search(l):
            follow = " | ".join(x.strip() for x in lines[i + 1:i + 4] if x.strip())
            gt_hits.append(l.strip() + (" || " + follow if follow else ""))
    if gt_hits:
        print(f"::error title=GameTest::{esc(' ## '.join(gt_hits[:20])[:6000])}")

    for path, line, msg in errors[:9]:
        print(f"::error file={path},line={line}::{esc(msg)}")
    if len(errors) > 9:
        rest = "; ".join(f"{p}:{l} {m.split(' || ')[0]}" for p, l, m in errors[9:40])
        print(f"::error title=Weitere Compiler-Fehler ({len(errors) - 9})::{esc(rest)}")
    if tests:
        print(f"::error title=Fehlgeschlagene Tests::{esc(' ; '.join(tests[:30]))}")
    if summary:
        print(f"::error title=Gradle::{esc(' | '.join(summary[:25]))}")
    # Rückfallebene: Zeilen mit 'error'/'Exception' aus dem Log
    if not errors and not tests:
        hits = [l.strip() for l in lines if re.search(r"error|exception|failed|caused by", l, re.I)][:40]
        print(f"::warning title=Log-Auszug::{esc(' | '.join(hits)[:6000])}")
        print(f"::warning title=Log-Ende::{esc(' | '.join(l.strip() for l in lines[-40:])[:6000])}")


if __name__ == "__main__":
    main()
