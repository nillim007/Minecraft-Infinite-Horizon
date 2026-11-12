"""Zeigt den Stand des GitHub-Builds für einen Commit, inklusive Fehlermeldungen (Anmerkungen).

Aufruf:
  python tools/ci_status.py                 # aktueller HEAD-Commit
  python tools/ci_status.py --wait          # wartet, bis alle Jobs fertig sind (max. 30 min)
  python tools/ci_status.py --sha <commit>

Ohne Anmeldung erlaubt GitHub 60 Abfragen pro Stunde. Mit der Umgebungsvariable GITHUB_TOKEN
gelten höhere Grenzen. Endet mit Code 0 (grün), 1 (rot) oder 2 (unklar/Zeitüberschreitung).
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
import urllib.error
import urllib.request

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")

REPO = "nillim007/Minecraft-Infinite-Horizon"
API = f"https://api.github.com/repos/{REPO}"


def get(url):
    req = urllib.request.Request(url, headers={"Accept": "application/vnd.github+json", "User-Agent": "infinite_horizon-ci-status"})
    token = os.environ.get("GITHUB_TOKEN")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        print(f"GitHub-API-Fehler {e.code} bei {url}: {e.read()[:200]!r}")
        if e.code in (403, 429):
            print("Abfragegrenze erreicht – später erneut versuchen oder GITHUB_TOKEN setzen.")
        sys.exit(2)


def head_sha():
    return subprocess.check_output(["git", "rev-parse", "HEAD"], text=True).strip()


def show(sha):
    runs = get(f"{API}/actions/runs?head_sha={sha}&per_page=10").get("workflow_runs", [])
    if not runs:
        return None, "Noch kein Build für diesen Commit gestartet."
    done = all(r["status"] == "completed" for r in runs)
    lines, ok = [], True
    for run in runs:
        lines.append(f"Workflow '{run['name']}': {run['status']} / {run['conclusion']}  {run['html_url']}")
        if run["status"] != "completed":
            ok = False
            continue
        jobs = get(f"{API}/actions/runs/{run['id']}/jobs").get("jobs", [])
        for job in jobs:
            lines.append(f"  Job '{job['name']}': {job['conclusion']}")
            if job["conclusion"] not in ("success", "skipped"):
                ok = False
                failed = [s["name"] for s in job.get("steps", []) if s.get("conclusion") == "failure"]
                if failed:
                    lines.append(f"    fehlgeschlagen: {', '.join(failed)}")
                notes = get(f"https://api.github.com/repos/{REPO}/check-runs/{job['id']}/annotations?per_page=100")
                for a in notes:
                    loc = f"{a['path']}:{a['start_line']}" if a.get("path") and a["path"] != ".github" else ""
                    lines.append(f"    [{a['annotation_level']}] {a.get('title') or ''} {loc} {a['message']}".rstrip())
            elif done and job["conclusion"] == "success" and ("bauen" in job["name"] or "Client" in job["name"]):
                # Bei Erfolg nur die eigenen Zusammenfassungen (z. B. GameTests) zeigen
                notes = get(f"https://api.github.com/repos/{REPO}/check-runs/{job['id']}/annotations?per_page=100")
                for a in notes:
                    if "Zusammenfassung" in (a.get("title") or ""):
                        lines.append(f"    [{a['annotation_level']}] {a['title']} {a['message']}")
    return (ok if done else None), "\n".join(lines)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sha")
    ap.add_argument("--wait", action="store_true")
    ap.add_argument("--interval", type=int, default=90)
    ap.add_argument("--timeout", type=int, default=1800)
    args = ap.parse_args()
    sha = args.sha or head_sha()
    print(f"Commit {sha[:7]}")
    start = time.time()
    while True:
        ok, text = show(sha)
        if ok is not None or not args.wait or time.time() - start > args.timeout:
            print(text)
            if ok is None:
                print("Build noch nicht fertig.")
                sys.exit(2)
            print("Ergebnis: GRÜN" if ok else "Ergebnis: ROT")
            sys.exit(0 if ok else 1)
        time.sleep(args.interval)


if __name__ == "__main__":
    main()
