"""Ingenieurs-Handbuch und Wiki aus einer gemeinsamen Quelle (Kapitel 24.1, 25.6).

Quelle:   docs/handbook/handbook.json  (Kapitel, Rezeptarten, Namen für Vanilla-IDs)
          docs/handbook/pages/<kapitel>/<seite>.md  (Seiten, Format in docs/handbook/README.md)
Ausgabe:  build() liefert die Handbuch-Ressourcen (assets/sternenwerft/handbook/…) und die
          Übersetzungsschlüssel für tools/gen_resources.py sowie die Wiki-Seiten für tools/gen_wiki.py.

Seiten erscheinen nur, wenn ihr Meilenstein erreicht ist und alle IDs aus `requires` registriert
sind (common/implemented_ids.json). Rezepte und Tabellen kommen immer aus spec/data. So wächst
das Handbuch mit jedem Meilenstein von selbst mit.
"""
from __future__ import annotations

import json
import re
import sys
from dataclasses import dataclass, field
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import specdata as sd  # noqa: E402

NS = "sternenwerft"
SRC = sd.ROOT / "docs" / "handbook"
PAGES = SRC / "pages"
CONFIG = SRC / "handbook.json"
IMPL = sd.ROOT / "common" / "implemented_ids.json"
LANGS = ("de", "en")
MC_LANG = {"de": "de_de", "en": "en_us"}

INLINE_LINK = re.compile(r"\[\[([^\]|]+)(?:\|([^\]]+))?\]\]")
INLINE_ITEM = re.compile(r"<item:([#a-z0-9_:/.\-]+)>")
INLINE_VALUE = re.compile(r"\{\{value:([a-z_]+):([^}]+)\}\}")


# ---------------------------------------------------------------- Zahlen und Namen

def num(v) -> str:
    if isinstance(v, float) and v.is_integer():
        v = int(v)
    if isinstance(v, int):
        return f"{v:,}".replace(",", ".")
    if isinstance(v, float):
        return f"{v:g}".replace(".", ",")
    return str(v)


def ticks_text(t) -> str:
    sec = t / 20
    return f"{num(t)} t ({num(round(sec, 1))} s)"


def snake(s: str) -> str:
    return re.sub(r"[^a-z0-9_]", "_", s.lower())


def wiki_slug(title: str) -> str:
    t = title
    for a, b in (("ä", "ae"), ("ö", "oe"), ("ü", "ue"), ("Ä", "Ae"), ("Ö", "Oe"), ("Ü", "Ue"), ("ß", "ss")):
        t = t.replace(a, b)
    t = re.sub(r"[^A-Za-z0-9]+", "-", t).strip("-")
    return t


@dataclass
class Page:
    chapter: str
    stem: str
    meta: dict
    body: dict  # Sprache -> Quelltext
    elements: dict = field(default_factory=dict)  # Sprache -> Elementliste

    @property
    def id(self) -> str:
        return f"{NS}:{self.chapter}/{self.stem}"

    def title(self, lang: str) -> str:
        return self.meta.get(f"title_{lang}") or self.meta["title"]

    @property
    def wiki(self) -> str:
        return self.meta.get("wiki") or wiki_slug(self.meta["title"])

    @property
    def targets(self) -> list:
        return self.meta.get("targets") or ["wiki", "game"]


@dataclass
class Handbook:
    pages: list
    chapters: list
    files: dict          # Ressourcenpfad -> JSON (für gen_resources)
    lang: dict           # "de_de"/"en_us" -> {Schlüssel: Text}
    wiki: dict           # Dateiname -> Markdown
    warnings: list


# ---------------------------------------------------------------- Quelle lesen

def parse_meta_value(v: str):
    v = v.strip()
    if v in ("null", "~", ""):
        return None
    if v[:1] in "[{" or v.isdigit() or v in ("true", "false"):
        return json.loads(v)
    return v


def read_page_file(path: Path):
    text = path.read_text(encoding="utf-8").replace("\r\n", "\n")
    meta = {}
    if text.startswith("---\n"):
        head, _, text = text[4:].partition("\n---\n")
        for line in head.splitlines():
            if line.strip() and not line.lstrip().startswith("#"):
                k, _, v = line.partition(":")
                meta[k.strip()] = parse_meta_value(v)
    return meta, text


def load_pages() -> list:
    pages = []
    for p in sorted(PAGES.glob("*/*.md")):
        if p.name.endswith(".en.md"):
            continue
        meta, body = read_page_file(p)
        bodies = {"de": body}
        en = p.with_name(p.stem + ".en.md")
        if en.exists():
            en_meta, en_body = read_page_file(en)
            bodies["en"] = en_body
            if en_meta.get("title"):
                meta["title_en"] = en_meta["title"]
        pages.append(Page(chapter=p.parent.name, stem=p.stem, meta=meta, body=bodies))
    return pages


# ---------------------------------------------------------------- Kontext

class Ctx:
    def __init__(self, spec, impl, config):
        self.spec = spec
        self.impl = impl
        self.config = config
        self.blocks = set(impl["blocks"])
        self.items = set(impl["items"])
        self.fluids = {f"{NS}:{f}" for f in impl.get("fluids", [])}
        self.registered = self.blocks | self.items | self.fluids
        self.milestone = impl.get("milestone", 1)
        self.step = impl.get("step") or str(self.milestone)
        self.warnings = []
        self.item_page = {}   # ID -> Page (Seite, die das Objekt beschreibt)
        self.pages_by_stem = {}

    # -- Sichtbarkeit
    def known(self, i: str) -> bool:
        return not i.startswith(NS + ":") or i in self.registered

    def recipe_ok(self, r) -> bool:
        ids = set(sd.recipe_results(r)) | set(sd.recipe_inputs(r))
        if r.get("mold"):
            ids.add(r["mold"])
        station = self.config["recipe_types"].get(r["type"], {}).get("station", "")
        if station.startswith(NS + ":"):
            ids.add(station)
        return all(self.known(i) for i in ids if not i.startswith("#"))

    def recipes(self):
        return [r for r in self.spec.recipes if self.recipe_ok(r)]

    # -- Namen
    def name(self, i: str, lang: str = "de") -> str:
        e = self.spec.items.get(i)
        if e:
            return e[f"name_{lang}"]
        n = self.config["names"].get(i)
        if n:
            return n[lang]
        m = re.match(r"#c:(\w+)/(\w+)$", i)  # Konventions-Tag, z. B. #c:plates/titanium
        if m:
            form = {"plates": "plate", "ingots": "ingot", "rods": "rod", "wires": "wire", "dusts": "dust"}.get(m.group(1))
            cand = f"{NS}:{m.group(2)}_{form}" if form else None
            if cand and cand in self.spec.items:
                return self.spec.items[cand][f"name_{lang}"]
        self.warnings.append(f"Kein Name für {i} (in docs/handbook/handbook.json unter names ergänzen)")
        return i

    def value(self, file: str, path: str):
        """{{value:<balance-datei>:<pfad>}} oder {{value:planet:<planet>.<pfad>}}; Pfadteile ohne Namespace erlaubt."""
        if file == "planet":
            cur = {k.split(":", 1)[1]: v for k, v in self.spec.planets.items()}
        else:
            cur = self.spec.balance.get(file)
        if cur is None:
            raise SystemExit(f"Handbuch: Datei {file} unbekannt")
        for key in path.split("."):
            if isinstance(cur, list) and key.isdigit() and int(key) < len(cur):
                cur = cur[int(key)]
            elif isinstance(cur, dict) and key in cur:
                cur = cur[key]
            elif isinstance(cur, dict) and f"{NS}:{key}" in cur:
                cur = cur[f"{NS}:{key}"]
            else:
                raise SystemExit(f"Handbuch: Wert {file}:{path} nicht gefunden")
        return cur

    def machine_values(self, block: str) -> dict:
        for f in ("machines", "generators", "storage", "transport"):
            v = self.spec.balance.get(f, {}).get(block)
            if isinstance(v, dict):
                return {"file": f, **v}
        return {}


# ---------------------------------------------------------------- Seiten in Elemente zerlegen

def resolve_values(ctx: Ctx, text: str) -> str:
    """Setzt Werte aus spec/data ein. Formate: {{value:datei:pfad}}, …|% (Anteil als Prozent), …|t (Ticks mit Sekunden)."""
    def sub(m):
        path, _, fmt = m.group(2).partition("|")
        v = ctx.value(m.group(1), path)
        if fmt == "%":
            return f"{num(round(v * 100, 1))} %"
        if fmt == "t":
            return ticks_text(v)
        return num(v)
    return INLINE_VALUE.sub(sub, text)


def parse_body(ctx: Ctx, page: Page, src: str) -> list:
    """Zerlegt den Seitentext in Elemente. Bedingungen (@if/@endif) werden hier ausgewertet."""
    elements = []
    para, lst, lst_ordered = [], [], False
    skip = []  # Stapel offener @if-Blöcke (True = überspringen)

    def flush_para():
        if para:
            elements.append({"type": "text", "text": resolve_values(ctx, " ".join(para))})
            para.clear()

    def flush_list():
        nonlocal lst_ordered
        if lst:
            elements.append({"type": "list", "ordered": lst_ordered, "items": [resolve_values(ctx, x) for x in lst]})
            lst.clear()

    for raw in src.split("\n"):
        line = raw.rstrip()
        s = line.strip()
        if s.startswith("@if "):
            ids = s[4:].split()
            skip.append(not all(ctx.known(i) for i in ids))
            continue
        if s.startswith("@ifnot "):
            ids = s[7:].split()
            skip.append(all(ctx.known(i) for i in ids))
            continue
        if s == "@endif":
            skip.pop()
            continue
        if any(skip):
            continue
        if s.startswith("<!--"):
            continue
        if not s:
            flush_para()
            flush_list()
            if elements and elements[-1].get("open"):
                elements[-1].pop("open")
            continue
        if s.startswith("|"):
            flush_para(); flush_list()
            cells = [resolve_values(ctx, c.strip()) for c in s.strip("|").split("|")]
            if all(re.fullmatch(r":?-{3,}:?", c) for c in cells):
                continue
            if elements and elements[-1].get("type") == "table" and elements[-1].get("open"):
                elements[-1]["rows"].append(cells)
            else:
                elements.append({"type": "table", "headers": cells, "rows": [], "open": True})
            continue
        if elements and elements[-1].get("open"):
            elements[-1].pop("open")
        if s.startswith("#"):
            flush_para(); flush_list()
            level = len(s) - len(s.lstrip("#"))
            elements.append({"type": "heading", "level": level, "text": resolve_values(ctx, s.lstrip("#").strip())})
            continue
        m = re.match(r"^(-|\d+\.)\s+(.*)$", s)
        if m:
            flush_para()
            ordered = m.group(1) != "-"
            if lst and ordered != lst_ordered:
                flush_list()
            lst_ordered = ordered
            lst.append(m.group(2))
            continue
        if s.startswith("@"):
            flush_para(); flush_list()
            cmd, _, arg = s[1:].partition(" ")
            arg = arg.strip()
            if cmd in ("tip", "warning", "note"):
                elements.append({"type": "callout", "style": cmd, "text": resolve_values(ctx, arg)})
            elif cmd == "recipe":
                r = next((x for x in ctx.spec.recipes if x["id"] == arg), None)
                if r is None:
                    raise SystemExit(f"{page.id}: Rezept {arg} unbekannt")
                if ctx.recipe_ok(r):
                    elements.append({"type": "recipes", "recipes": [r["id"]], "style": "single"})
            elif cmd == "recipes_for":
                ids = arg.split()
                rs = [r["id"] for r in ctx.recipes() if set(ids) & set(sd.recipe_results(r))]
                if rs:
                    elements.append({"type": "recipes", "recipes": rs, "style": "single" if len(rs) <= 2 else "table"})
            elif cmd == "recipes_type":
                types = arg.split()
                rs = [r["id"] for r in ctx.recipes() if r["type"] in types]
                rs.sort(key=lambda rid: ctx.name(sd_results(ctx, rid)[0]))
                if rs:
                    elements.append({"type": "recipes", "recipes": rs, "style": "table"})
            elif cmd == "uses":
                ids = set(arg.split())
                rs = [r["id"] for r in ctx.recipes() if ids & set(sd.recipe_inputs(r))]
                if rs:
                    elements.append({"type": "recipes", "recipes": rs, "style": "table"})
            elif cmd == "machine":
                if ctx.known(arg):
                    elements.append({"type": "machine", "block": arg})
            elif cmd == "ores":
                elements.append(ores_table(ctx, arg or "minecraft:overworld"))
            elif cmd == "items":
                elements.append(items_table(ctx))
            elif cmd == "image":
                # @image <wiki-pfad> [<textur-id> <breite> <höhe>]
                parts = arg.split()
                el = {"type": "image", "wiki": parts[0]}
                if len(parts) >= 4:
                    el.update({"texture": parts[1], "width": int(parts[2]), "height": int(parts[3])})
                elements.append(el)
            elif cmd == "wiki":
                elements.append({"type": "wiki_only", "markdown": arg})
            else:
                raise SystemExit(f"{page.id}: unbekannter Befehl @{cmd}")
            continue
        flush_list()
        para.append(s)
    flush_para()
    flush_list()
    for el in elements:
        el.pop("open", None)
    if skip:
        raise SystemExit(f"{page.id}: @if ohne @endif")
    return elements


def sd_results(ctx: Ctx, rid: str) -> list:
    r = next(x for x in ctx.spec.recipes if x["id"] == rid)
    res = sd.recipe_results(r)
    return res or ["?"]


def ores_table(ctx: Ctx, planet: str) -> dict:
    p = ctx.spec.planets[planet]
    rows = []
    for o in p.get("ores", []):
        if not ctx.known(o["block"]):
            continue
        if o["placement"] == "vein":
            where = f"y {num(o['y_min'])} bis {num(o['y_max'])}"
            amount = f"{num(o['per_chunk'])} Adern je Chunk, je bis {num(o['vein_size'])} Blöcke"
        else:
            r = o.get("radius") or [0, 0]
            where = "Oberfläche (Scheibe wie Ton)"
            amount = f"{num(o['per_chunk'])} je Chunk, Radius {num(r[0])}–{num(r[1])}"
        biomes = ", ".join(f"<item:{b}>" for b in o.get("biomes", []))
        drops = o.get("drops")
        drop = ", ".join(f"{d['count']} <item:{d['id']}>" for d in drops) if isinstance(drops, list) else "der Block selbst"
        if o.get("deepslate_block"):
            where += " (darunter als Tiefenschiefer-Variante)"
        rows.append([f"<item:{o['block']}>", drop, where, biomes, amount])
    return {"type": "table", "headers": ["Block", "Ergibt", "Höhe", "Biome", "Menge"], "rows": rows}


def items_table(ctx: Ctx) -> dict:
    how = {"recipe": "Rezept", "worldgen": "Weltgenerierung", "machine_output": "Maschine", "block_drop": "Abbau",
           "join_gift": "Geschenk beim ersten Betreten", "generated": "Füllslot oder Eimer", "mob_drop": "Kreatur"}
    rows = []
    entries = [e for e in ctx.spec.all_entries if e["id"] in ctx.registered and e["kind"] in ("item", "block", "fluid")]
    for e in sorted(entries, key=lambda e: e["name_de"]):
        rows.append([f"<item:{e['id']}>", e["kind"].replace("block", "Block").replace("item", "Item").replace("fluid", "Fluid"),
                     ", ".join(how.get(o, o) for o in e.get("obtained_by") or [])])
    return {"type": "table", "headers": ["Name", "Art", "Herkunft"], "rows": rows}


# ---------------------------------------------------------------- Inline-Markup

def link_target(ctx: Ctx, ref: str):
    ref = ref.strip()
    if ":" in ref:
        p = next((x for x in ctx.pages if x.id == ref), None)
    else:
        p = ctx.pages_by_stem.get(ref)
    if p is None:
        ctx.warnings.append(f"Link auf unbekannte oder noch nicht sichtbare Seite: {ref}")
    return p


def inline_game(ctx: Ctx, text: str, lang: str = "de") -> str:
    """Markup für das Spiel: [[sternenwerft:kapitel/seite|Text]], <item:id>, **fett**.

    <item:id> bleibt nur für registrierte eigene Items und Blöcke stehen (das Handbuch zeigt den
    übersetzten Namen und verlinkt auf die Seite, die das Objekt in `items` nennt). Alles andere
    (Vanilla, Tags, Biome, Fluide) wird hier schon durch den Namen in der Sprache ersetzt.
    """
    if not text:
        return text

    def link(m):
        p = link_target(ctx, m.group(1))
        label = m.group(2) or (p.title(lang) if p else m.group(1))
        return f"[[{p.id}|{label}]]" if p else label

    def item(m):
        i = m.group(1)
        if i in ctx.blocks or i in ctx.items:
            return m.group(0)
        return ctx.name(i, lang)
    return INLINE_ITEM.sub(item, INLINE_LINK.sub(link, text))


def inline_wiki(ctx: Ctx, text: str, lang: str = "de") -> str:
    def link(m):
        p = link_target(ctx, m.group(1))
        label = m.group(2) or (p.title(lang) if p else m.group(1))
        return f"[{label}]({p.wiki}.md)" if p and "wiki" in p.targets else label

    def item(m):
        i = m.group(1)
        name = ctx.name(i, lang)
        p = ctx.item_page.get(i)
        if p is None or "wiki" not in p.targets or p is getattr(ctx, "current", None):
            return name  # keine Links auf die eigene Seite
        return f"[{name}]({p.wiki}.md)"
    return INLINE_ITEM.sub(item, INLINE_LINK.sub(link, text))


# ---------------------------------------------------------------- Rezepte darstellen

def recipe_station(ctx: Ctx, r) -> dict:
    return ctx.config["recipe_types"][r["type"]]


def recipe_time(ctx: Ctx, r):
    station = recipe_station(ctx, r)["station"]
    mv = ctx.machine_values(station)
    ticks = r.get("ticks") or mv.get("ticks")
    ept = r.get("energy_per_tick") or mv.get("energy_per_tick")
    return ticks, ept


def ing_text(ctx: Ctx, ing: dict, lang="de") -> str:
    ids = sd.ingredient_ids(ing)
    alt = " oder ".join(f"<item:{i}>" for i in ids)
    return f"{ing.get('count', 1)} × {alt}"


def recipe_parts(ctx: Ctx, r):
    ins = [ing_text(ctx, g) for g in r.get("ingredients") or []]
    if r.get("fluid"):
        ins.append(f"{num(r['fluid']['amount'])} mB <item:{r['fluid']['fluid']}>")
    if r.get("mold"):
        ins.append(f"Form: <item:{r['mold']}>")
    outs = []
    for x in r["results"]:
        chance = f" ({num(round(x['chance'] * 100))} %)" if x.get("chance", 1) != 1 else ""
        outs.append(f"{x['count']} × <item:{x['id']}>{chance}")
    for x in r.get("fluid_results") or []:
        outs.append(f"{num(x['amount'])} mB <item:{x['fluid']}>")
    return ins, outs


def recipe_display(ctx: Ctx, r) -> dict:
    """Anzeige-Daten für das Spiel. Der Client kennt seit 1.21.2 nicht alle Rezepte, daher eingebettet."""
    ticks, ept = recipe_time(ctx, r)
    d = {"id": r["id"], "type": f"{NS}:{r['type']}" if not r["type"].startswith(("crafting", "smelting")) else f"minecraft:{r['type']}",
         "station": recipe_station(ctx, r)["station"]}
    if r.get("pattern"):
        d["pattern"] = r["pattern"]
        d["key"] = {k: sd.ingredient_ids(v) for k, v in r["key"].items()}
    else:
        d["ingredients"] = [{"items": sd.ingredient_ids(g), "count": g.get("count", 1)} for g in r.get("ingredients") or []]
    if r.get("fluid"):
        d["fluid"] = {"fluid": r["fluid"]["fluid"], "amount": r["fluid"]["amount"]}
    if r.get("mold"):
        d["mold"] = r["mold"]
    d["results"] = [{"item": x["id"], "count": x["count"], **({"chance": x["chance"]} if x.get("chance", 1) != 1 else {})}
                    for x in r["results"]]
    if r.get("fluid_results"):
        d["fluid_results"] = [{"fluid": x["fluid"], "amount": x["amount"]} for x in r["fluid_results"]]
    if ticks and r["type"] not in ("crafting_shaped", "crafting_shapeless"):
        d["ticks"] = ticks
        if ept:
            d["energy_per_tick"] = ept
    return d


def wiki_grid(ctx: Ctx, r) -> str:
    """Werkbank-Rezept als 3×3-Raster mit Legende."""
    pat = [row.ljust(3) for row in r["pattern"]]
    while len(pat) < 3:
        pat.append("   ")
    cell = lambda c: c if c != " " else "·"
    lines = ["┌───┬───┬───┐"]
    for i, row in enumerate(pat):
        lines.append("│ " + " │ ".join(cell(c) for c in row) + " │")
        lines.append("├───┼───┼───┤" if i < 2 else "└───┴───┴───┘")
    legend = " · ".join(f"`{k}` = {inline_wiki(ctx, ' oder '.join(f'<item:{i}>' for i in sd.ingredient_ids(v)))}"
                        for k, v in sorted(r["key"].items()))
    return "```text\n" + "\n".join(lines) + "\n```\n\n" + legend


def wiki_recipe_single(ctx: Ctx, r) -> str:
    res = r["results"][0]
    title = inline_wiki(ctx, f"<item:{res['id']}>") if r["results"] else "?"
    count = f"{res['count']} × " if res["count"] != 1 else ""
    station = inline_wiki(ctx, f"<item:{recipe_station(ctx, r)['station']}>") if recipe_station(ctx, r)["station"].startswith(NS) \
        else recipe_station(ctx, r)["de"]
    head = f"**{count}{title}** · {station}"
    if r.get("pattern"):
        return head + "\n\n" + wiki_grid(ctx, r)
    ins, outs = recipe_parts(ctx, r)
    ticks, ept = recipe_time(ctx, r)
    extra = ""
    if r["type"] not in ("crafting_shaped", "crafting_shapeless", "smelting") and ticks:
        extra = f" · {ticks_text(ticks)}" + (f" · {num(ept)} J/t" if ept else "")
    return head + extra + "\n\n" + inline_wiki(ctx, " + ".join(ins)) + " → " + inline_wiki(ctx, " + ".join(outs))


def wiki_recipe_table(ctx: Ctx, rids) -> str:
    rows = []
    for rid in rids:
        r = next(x for x in ctx.spec.recipes if x["id"] == rid)
        ins, outs = recipe_parts(ctx, r)
        ticks, ept = recipe_time(ctx, r)
        t = "–" if r["type"] in ("crafting_shaped", "crafting_shapeless", "smelting") or not ticks else ticks_text(ticks)
        e = f"{num(ept)} J/t" if ept and t != "–" else "–"
        if r.get("pattern"):
            ins = [f"{sum(row.count(k) for row in r['pattern'])} × <item:{sd.ingredient_ids(v)[0]}>" for k, v in sorted(r["key"].items())]
        rows.append([inline_wiki(ctx, ", ".join(outs)), inline_wiki(ctx, ", ".join(ins)), recipe_station(ctx, r)["de"], t, e])
    return md_table(["Ergebnis", "Zutaten", "Wo", "Dauer", "Energie"], rows)


def md_table(headers, rows) -> str:
    out = ["| " + " | ".join(headers) + " |", "| " + " | ".join("---" for _ in headers) + " |"]
    for r in rows:
        out.append("| " + " | ".join(str(c).replace("|", "\\|") for c in r) + " |")
    return "\n".join(out)


# ---------------------------------------------------------------- Maschinen-Steckbrief

def machine_rows(ctx: Ctx, block: str) -> list:
    v = ctx.machine_values(block)
    rows = []
    if not v:
        return rows
    if "energy_per_tick" in v:
        rows.append(["Verbrauch", f"{num(v['energy_per_tick'])} J/t"])
    if v.get("ticks"):
        rows.append(["Dauer je Vorgang", ticks_text(v["ticks"])])
    if "output_per_tick" in v and not isinstance(v["output_per_tick"], dict):
        rows.append(["Leistung", f"{num(v['output_per_tick'])} J/t"])
    if v.get("throughput"):
        rows.append(["Durchsatz", f"{num(v['throughput'])} {v.get('unit', '')}"])
    if v.get("capacity"):
        rows.append(["Speicher", f"{num(v['capacity'])} {v.get('unit', '')}"])
    if v.get("io_per_tick"):
        rows.append(["Ein/Aus", f"{num(v['io_per_tick'])} J/t"])
    if isinstance(v.get("buffer"), int):
        rows.append(["Energiepuffer", f"{num(v['buffer'])} J"])
    slots = v.get("item_slots") or {}
    if slots.get("input"):
        rows.append(["Slots", f"{slots['input']} Eingabe, {slots.get('output', 0)} Ausgabe"])
    tanks = v.get("fluid_tanks") or []
    if tanks:
        rows.append(["Tanks", ", ".join(f"{num(t.get('capacity', 0))} mB" for t in tanks)])
    if v.get("upgrade_slots"):
        rows.append(["Upgrade-Slots", str(v["upgrade_slots"])])
    if v.get("location"):
        rows.append(["Ort", json.dumps(v["location"], ensure_ascii=False)])
    for f in v.get("fuel") or []:
        if f.get("fluid"):
            rows.append(["Brennstoff", f"1 mB <item:{f['fluid']}> je {num(f['per_ticks'])} t"])
        elif f.get("example"):
            rows.append(["Brennstoff", f"alles, was im Ofen brennt (z. B. <item:{f['example']['item']}>: {ticks_text(f['example']['per_ticks'])})"])
    return rows


# ---------------------------------------------------------------- Ausgabe Wiki

def wiki_elements(ctx: Ctx, page: Page) -> str:
    ctx.current = page
    out = []
    for el in page.elements["de"]:
        t = el["type"]
        if t == "heading":
            out.append("#" * max(2, el["level"]) + " " + inline_wiki(ctx, el["text"]))
        elif t == "text":
            out.append(inline_wiki(ctx, el["text"]))
        elif t == "list":
            out.append("\n".join((f"{n}. " if el["ordered"] else "- ") + inline_wiki(ctx, x)
                                 for n, x in enumerate(el["items"], 1)))
        elif t == "callout":
            label = {"tip": "TIP", "warning": "WARNING", "note": "NOTE"}[el["style"]]
            out.append(f"> [!{label}]\n> " + inline_wiki(ctx, el["text"]))
        elif t == "recipes":
            if el["style"] == "table":
                out.append(wiki_recipe_table(ctx, el["recipes"]))
            else:
                for rid in el["recipes"]:
                    out.append(wiki_recipe_single(ctx, next(x for x in ctx.spec.recipes if x["id"] == rid)))
        elif t == "machine":
            rows = [[k, inline_wiki(ctx, v)] for k, v in machine_rows(ctx, el["block"])]
            if rows:
                out.append(md_table(["Steckbrief: " + ctx.name(el["block"]), ""], rows))
        elif t == "table":
            out.append(md_table(el["headers"], [[inline_wiki(ctx, c) for c in row] for row in el["rows"]]))
        elif t == "image":
            out.append(f"![{page.title('de')}]({el['wiki']})")
        elif t == "wiki_only":
            out.append(inline_wiki(ctx, el["markdown"]))
    return "\n\n".join(out)


GEN_NOTE = "<!-- Erzeugt von tools/gen_wiki.py aus docs/handbook/ und spec/data. Nicht von Hand ändern. -->\n\n"


def build_wiki(ctx: Ctx, chapters: list) -> dict:
    files = {}
    wiki_pages = [p for p in ctx.pages if "wiki" in p.targets]
    by_chapter = {c["id"]: [p for p in wiki_pages if p.chapter == c["id"]] for c in chapters}
    status = f"Stand: Meilenstein {ctx.step}"
    for c in chapters:
        plist = by_chapter[c["id"]]
        for n, p in enumerate(plist):
            nav = f"[Startseite](Home.md) › {c['title_de']} › **{p.title('de')}**"
            prev_ = f"← [{plist[n - 1].title('de')}]({plist[n - 1].wiki}.md)" if n > 0 else ""
            next_ = f"[{plist[n + 1].title('de')}]({plist[n + 1].wiki}.md) →" if n + 1 < len(plist) else ""
            foot = " · ".join(x for x in (prev_, next_) if x)
            ingame = "\n\n---\n\n_Diese Seite steht auch im Spiel im **Ingenieurs-Handbuch**._" if "game" in p.targets else ""
            files[f"{p.wiki}.md"] = (GEN_NOTE + nav + "\n\n# " + p.title("de") + "\n\n" + wiki_elements(ctx, p) +
                                     ingame + ("\n\n" + foot if foot else "") + "\n")
    # Startseite
    props = sd.ROOT / "gradle.properties"
    version = ""
    if props.exists():
        m = re.search(r"^version=(.+)$", props.read_text(encoding="utf-8"), re.M)
        version = m.group(1).strip() if m else ""
    home = [GEN_NOTE.rstrip(), "", "# Sternenwerft – Wiki", ""]
    intro = SRC / "home.md"
    if intro.exists():
        home += [read_page_file(intro)[1].strip(), ""]
    home += ["Das Wiki zur Raumfahrt-Mod **Minecraft New Horizon – Sternenwerft**. Alle Inhalte stehen auch im Spiel im "
             "**Ingenieurs-Handbuch**; Rezepte und Werte kommen direkt aus den Daten der Mod und sind damit immer aktuell.",
             "", f"> {status}" + (f" · Mod-Version {version}" if version else ""), ""]
    for c in chapters:
        if by_chapter[c["id"]]:
            home.append(f"## {c['title_de']}\n")
            home += [f"- [{p.title('de')}]({p.wiki}.md)" + (f" – {p.meta['summary']}" if p.meta.get("summary") else "")
                     for p in by_chapter[c["id"]]]
            home.append("")
    files["Home.md"] = "\n".join(home)
    side = ["**[Startseite](Home.md)**", ""]
    for c in chapters:
        if by_chapter[c["id"]]:
            side.append(f"**{c['title_de']}**\n")
            side += [f"- [{p.title('de')}]({p.wiki}.md)" for p in by_chapter[c["id"]]]
            side.append("")
    side.append(f"_{status}_")
    files["_Sidebar.md"] = "\n".join(side) + "\n"
    files["_Footer.md"] = ("Sternenwerft · MIT-Lizenz · Dieses Wiki wird aus `docs/handbook/` erzeugt "
                           "(`python tools/gen_wiki.py`). Änderungen bitte dort.\n")
    return files


# ---------------------------------------------------------------- Ausgabe Spiel

def build_game(ctx: Ctx, chapters: list):
    files, lang = {}, {MC_LANG[l]: {} for l in LANGS}

    def key(k, texts: dict):
        for l in LANGS:
            lang[MC_LANG[l]][k] = texts.get(l) or texts["de"]
        return k

    for c in chapters:
        ck = f"{NS}.handbook.chapter.{c['id']}"
        key(ck, {"de": c["title_de"], "en": c["title_en"]})
        files[f"assets/{NS}/handbook/{c['id']}/_chapter.json"] = {
            "id": f"{NS}:{c['id']}", "title": ck, "icon": c["icon"], "order": c["order"], "unlock": c["unlock"]}

    chapter_unlock = {c["id"]: c["unlock"] for c in chapters}
    for p in ctx.pages:
        if "game" not in p.targets:
            continue
        base = f"{NS}.handbook.{p.chapter}.{p.stem}"
        els_de = p.elements["de"]
        els_en = p.elements.get("en")
        if els_en is not None and [e["type"] for e in els_en] != [e["type"] for e in els_de]:
            ctx.warnings.append(f"{p.id}: englische Fassung hat einen anderen Aufbau; nutze Deutsch")
            els_en = None
        out = []

        def both(de_text, en_text=None):
            # Ohne englische Fassung bleibt der deutsche Text (Übersetzungen folgen mit M9), Namen aber je Sprache.
            return {"de": inline_game(ctx, de_text, "de"),
                    "en": inline_game(ctx, en_text, "en") if en_text else inline_game(ctx, de_text, "en")}

        for n, el in enumerate(els_de):
            en = els_en[n] if els_en else {}
            k = f"{base}.e{n}"
            t = el["type"]
            if t in ("heading", "text", "callout"):
                o = {"type": t, "key": key(k, both(el["text"], en.get("text")))}
                if t == "heading":
                    o["level"] = el["level"]
                if t == "callout":
                    o["style"] = el["style"]
                out.append(o)
            elif t == "list":
                en_items = en.get("items") or []
                out.append({"type": "list", "ordered": el["ordered"], "keys": [
                    key(f"{k}.{i}", both(x, en_items[i] if i < len(en_items) else None))
                    for i, x in enumerate(el["items"])]})
            elif t == "recipes":
                out.append({"type": "recipes", "recipes": [recipe_display(ctx, next(x for x in ctx.spec.recipes if x["id"] == rid))
                                                           for rid in el["recipes"]]})
            elif t == "machine":
                rows = machine_rows(ctx, el["block"])
                out.append({"type": "machine", "block": el["block"], "rows": [
                    [key(f"{k}.{i}.label", both(a)), key(f"{k}.{i}.value", both(b))] for i, (a, b) in enumerate(rows)]})
            elif t == "table":
                out.append({"type": "table",
                            "headers": [key(f"{k}.h{i}", both(h)) for i, h in enumerate(el["headers"])],
                            "rows": [[key(f"{k}.r{ri}.{ci}", both(c)) for ci, c in enumerate(row)]
                                     for ri, row in enumerate(el["rows"])]})
            elif t == "image" and el.get("texture"):
                out.append({"type": "image", "texture": el["texture"], "width": el["width"], "height": el["height"]})
        title = key(f"{base}.title", {"de": p.title("de"), "en": p.title("en")})
        files[f"assets/{NS}/handbook/{p.chapter}/{p.stem}.json"] = {
            "id": p.id, "chapter": f"{NS}:{p.chapter}", "title": title, "icon": p.meta.get("icon"),
            "order": p.meta.get("order", 100),
            "unlock": p.meta["unlock"] if "unlock" in p.meta else chapter_unlock[p.chapter],
            "items": p.meta.get("items") or [], "elements": out}
    return files, lang


# ---------------------------------------------------------------- Einstieg

def build(spec=None, impl=None) -> Handbook:
    spec = spec or sd.load()
    impl = impl or json.loads(IMPL.read_text(encoding="utf-8"))
    config = sd.load_json(CONFIG)
    ctx = Ctx(spec, impl, config)

    all_pages = load_pages()
    chapter_ids = {c["id"] for c in config["chapters"]}
    for p in all_pages:
        if p.chapter not in chapter_ids:
            raise SystemExit(f"Handbuch: Kapitel {p.chapter} fehlt in handbook.json ({p.id})")
        for i in (p.meta.get("requires") or []) + (p.meta.get("items") or []) + ([p.meta["icon"]] if p.meta.get("icon") else []):
            if i.startswith(NS + ":") and i not in spec.items:
                raise SystemExit(f"Handbuch: {p.id} nennt unbekannte ID {i}")
    visible = [p for p in all_pages
               if (p.meta.get("milestone") or 1) <= ctx.milestone
               and all(ctx.known(i) for i in p.meta.get("requires") or [])]
    order = {c["id"]: c["order"] for c in config["chapters"]}
    visible.sort(key=lambda p: (order[p.chapter], p.meta.get("order", 100), p.stem))
    ctx.pages = visible
    for p in visible:
        ctx.pages_by_stem.setdefault(p.stem, p)
        for i in p.meta.get("items") or []:
            if ctx.known(i):
                ctx.item_page.setdefault(i, p)
    for p in visible:
        p.elements = {l: parse_body(ctx, p, b) for l, b in p.body.items()}

    chapters = [c for c in sorted(config["chapters"], key=lambda c: c["order"]) if any(p.chapter == c["id"] for p in visible)]
    files, lang = build_game(ctx, chapters)
    wiki = build_wiki(ctx, chapters)
    # Namen vorab auflösen, damit fehlende Namen als Warnung erscheinen
    for rid in [r["id"] for r in ctx.recipes()]:
        for i in sd.recipe_inputs(next(x for x in spec.recipes if x["id"] == rid)):
            ctx.name(i)
    return Handbook(pages=visible, chapters=chapters, files=files, lang=lang, wiki=wiki,
                    warnings=sorted(set(ctx.warnings)))
