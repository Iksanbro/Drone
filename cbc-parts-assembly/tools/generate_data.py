#!/usr/bin/env python3
"""Generates every data file of the mod from one list of cannon parts.

Run from the project root:

    python3 tools/generate_data.py [--validate DIR ...]

Outputs (all overwritten on every run):
  * src/generated/resources/data/cbcparts/recipe/...         sequenced assembly + helper recipes
  * src/generated/resources/data/<addon>/loot_table/blocks/  loot tables missing from addon jars
  * src/generated/resources/assets/cbcparts/models/item/     models of unfinished items
  * src/main/java/.../GeneratedParts.java                    list of unfinished items to register
  * RECIPES.md                                               human readable list of the recipes

--validate takes folders with extracted mod jars (CBC, CBC:AT, Neo Warfare) and checks
that every item id used by the recipes really exists in them.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
from dataclasses import dataclass, field

MOD_ID = "cbcparts"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEN_RES = os.path.join(ROOT, "src", "generated", "resources")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "io", "github", "iksanbro", "cbcparts", "GeneratedParts.java")

CBC = "createbigcannons"
CBCAT = "cbc_at"
NW = "cbcmodernwarfare"

CAST_IRON, BRONZE, STEEL, NETHERSTEEL = "cast_iron", "bronze", "steel", "nethersteel"
AUTOCANNON_MATERIALS = [CAST_IRON, BRONZE, STEEL]


def ingot(material: str) -> dict:
    return {"tag": f"c:ingots/{material}"}


# Part kinds decide what is done in every loop after the mould and the metal:
#   bore   - pressing, then the saw cuts the bore (barrels, rails, silencers, muzzle brakes)
#   plain  - pressing only (bearings, rocket pod breeches)
#   breech - pressing only, but the recipe starts from a breech mechanism instead of an ingot
#   recoil - pressing, then a spring is added (recoil springs, recoil barrels)
BORE, PLAIN, BREECH, RECOIL = "bore", "plain", "breech", "recoil"


@dataclass
class Part:
    mod: str
    block: str           # id of the finished part (block item)
    material: str
    kind: str
    mould: str           # full id of the cast mould held by the deployer
    ingots: int          # metal cost, taken from the casting volume (90 mB per ingot)
    base: dict | None = None    # starting item; an ingot of the material when None
    spring: str | None = None   # spring added every loop for RECOIL parts
    key: str | None = None      # name of the unfinished item when it differs from block

    @property
    def result(self) -> str:
        return f"{self.mod}:{self.block}"

    @property
    def unfinished(self) -> str:
        return f"unfinished_{self.key or self.block}"


@dataclass
class Component:
    """A helper item that the addon has no recipe for in 1.21.1."""
    mod: str
    item: str
    base: dict
    steps: list            # list of ("deploy", ingredient) / ("press",) / ("cut",)
    loops: int
    key: str
    conditions: list = field(default_factory=list)

    @property
    def unfinished(self) -> str:
        return f"unfinished_{self.key}"


def build_parts() -> list[Part]:
    parts: list[Part] = []

    def extractor(m: str) -> dict:
        return {"item": f"{CBC}:{m}_autocannon_breech_extractor"}

    # --- Create Big Cannons: autocannons -----------------------------------------------
    for m in AUTOCANNON_MATERIALS:
        parts += [
            Part(CBC, f"{m}_autocannon_barrel", m, BORE, f"{CBC}:autocannon_barrel_cast_mould", 3),
            Part(CBC, f"{m}_autocannon_breech", m, BREECH, f"{CBC}:autocannon_breech_cast_mould", 4,
                 base=extractor(m)),
            Part(CBC, f"{m}_autocannon_recoil_spring", m, RECOIL, f"{CBC}:autocannon_recoil_spring_cast_mould", 4,
                 spring=f"{CBC}:recoil_spring"),
        ]

    # --- CBC: Advanced Technologies --------------------------------------------------
    for m in AUTOCANNON_MATERIALS:
        parts += [
            Part(CBCAT, f"{m}_autocannon_silencer", m, BORE, f"{CBCAT}:autocannon_silencer_mould", 3),
            Part(CBCAT, f"{m}_autocannon_muzzle_brake", m, BORE, f"{CBCAT}:autocannon_muzzle_brake_mould", 3),
        ]
        for twin, big in (("twin", ()), ("vert_twin", ("recoil_spring", "breech", "silencer"))):
            for piece in ("barrel", "recoil_spring", "breech", "silencer", "muzzle_brake"):
                cost = 8 if piece in big else 6
                mould = f"{CBCAT}:{twin}_autocannon_{piece}_mould"
                block = f"{m}_{twin}_autocannon_{piece}"
                if piece == "breech":
                    parts.append(Part(CBCAT, block, m, BREECH, mould, cost, base=extractor(m)))
                elif piece == "recoil_spring":
                    parts.append(Part(CBCAT, block, m, RECOIL, mould, cost, spring=f"{CBC}:recoil_spring"))
                else:
                    parts.append(Part(CBCAT, block, m, BORE, mould, cost))
        for piece in ("barrel", "muzzle_brake", "silencer", "recoil_spring", "breech", "qfbreech"):
            mould = f"{CBCAT}:heavy_autocannon_{piece}_mould"
            block = f"{m}_heavy_autocannon_{piece}"
            if piece in ("breech", "qfbreech"):
                parts.append(Part(CBCAT, block, m, BREECH, mould, 6, base=extractor(m)))
            elif piece == "recoil_spring":
                parts.append(Part(CBCAT, block, m, RECOIL, mould, 6, spring=f"{CBC}:recoil_spring"))
            else:
                parts.append(Part(CBCAT, block, m, BORE, mould, 6))
        parts += [
            Part(CBCAT, f"{m}_rocket_pod_rail", m, BORE, f"{CBCAT}:rocket_pod_rail_mould", 10),
            Part(CBCAT, f"{m}_rocket_pod_breech", m, PLAIN, f"{CBCAT}:rocket_pod_breech_mould", 10),
        ]

    # --- CBC Neo Warfare: medium cannons -----------------------------------------------
    for m in (CAST_IRON, BRONZE, STEEL, NETHERSTEEL):
        parts += [
            Part(NW, f"{m}_mediumcannon_barrel", m, BORE, f"{NW}:mediumcannon_barrel_cast_mould", 3),
            Part(NW, f"{m}_mediumcannon_breech", m, BREECH, f"{NW}:mediumcannon_breech_cast_mould", 4,
                 base={"item": f"{NW}:{m}_mediumcannon_falling_breech"}),
            Part(NW, f"{m}_mediumcannon_recoil_barrel", m, RECOIL, f"{NW}:mediumcannon_recoil_barrel_cast_mould", 4,
                 spring=f"{NW}:heavy_recoil_spring"),
        ]

    # --- CBC Neo Warfare: rotary cannons (no cast iron variant exists) -----------------
    for m in (BRONZE, STEEL, NETHERSTEEL):
        parts += [
            Part(NW, f"{m}_rotarycannon_barrel", m, BORE, f"{NW}:rotarycannon_barrel_cast_mould", 3),
            Part(NW, f"{m}_rotarycannon_breech", m, BREECH, f"{NW}:rotarycannon_breech_cast_mould", 4,
                 base={"item": f"{NW}:{rotary_extractor_id(m)}"}),
            Part(NW, f"{m}_rotarycannon_bearing", m, PLAIN, f"{NW}:rotarycannon_bearing_cast_mould", 4),
        ]
    return parts


def rotary_extractor_id(material: str) -> str:
    # Neo Warfare registers the steel one with a typo
    return "steel_rotaryannon_breech_extractor" if material == STEEL else f"{material}_rotarycannon_breech_extractor"


def build_components() -> list[Component]:
    comps: list[Component] = []
    for m in (BRONZE, STEEL, NETHERSTEEL):
        comps.append(Component(NW, rotary_extractor_id(m), ingot(m),
                               [("deploy", {"tag": "c:plates/iron"}), ("cut",)], 2,
                               key=f"{m}_rotarycannon_breech_extractor"))
    for m in (CAST_IRON, BRONZE, STEEL, NETHERSTEEL):
        comps.append(Component(NW, f"{m}_mediumcannon_falling_breech", ingot(m),
                               [("deploy", {"item": "create:andesite_alloy"}), ("press",), ("cut",)], 2,
                               key=f"{m}_mediumcannon_falling_breech"))
    comps.append(Component(NW, "heavy_recoil_spring", {"item": f"{CBC}:recoil_spring"},
                           [("deploy", {"tag": "c:plates/iron"}), ("press",)], 2,
                           key="heavy_recoil_spring"))
    return comps


# Moulds that Neo Warfare 1.21.1 has no recipe for. Same recipe as the CBC moulds.
NW_MOULDS = [
    "mediumcannon_barrel_cast_mould", "mediumcannon_breech_cast_mould", "mediumcannon_recoil_barrel_cast_mould",
    "rotarycannon_barrel_cast_mould", "rotarycannon_breech_cast_mould", "rotarycannon_bearing_cast_mould",
]


# CBC:AT 0.1.4c ships these mould recipes in a format 1.21.1 cannot parse, so they never load.
CBCAT_BROKEN_MOULDS = ["twin_autocannon_barrel_mould", "rocket_pod_rail_mould", "rocket_pod_breech_mould"]


# ----------------------------------------------------------------------------------------
# JSON builders

def mod_loaded(*mods: str) -> list:
    return [{"type": "neoforge:mod_loaded", "modid": m} for m in mods]


def step_json(step: tuple, transitional: str) -> dict:
    t = {"item": transitional}
    out = [{"id": transitional}]
    kind = step[0]
    if kind == "deploy":
        js = {"type": "create:deploying", "ingredients": [t, step[1]], "results": out}
        if len(step) > 2 and step[2]:
            js["keep_held_item"] = True
        return js
    if kind == "press":
        return {"type": "create:pressing", "ingredients": [t], "results": out}
    if kind == "cut":
        return {"type": "create:cutting", "ingredients": [t], "results": out}
    raise ValueError(kind)


def sequenced(conditions: list, base: dict, steps: list, loops: int, transitional: str, result: str) -> dict:
    return {
        "neoforge:conditions": conditions,
        "type": "create:sequenced_assembly",
        "ingredient": base,
        "loops": loops,
        "results": [{"id": result}],
        "sequence": [step_json(s, transitional) for s in steps],
        "transitional_item": {"id": transitional},
    }


def part_plan(p: Part) -> tuple[dict, list, int]:
    """Returns (base ingredient, steps of one loop, loops)."""
    metal_left = p.ingots - 1  # the base (ingot or breech mechanism) counts as one ingot
    per_loop = 2 if p.ingots >= 6 else 1
    loops = max(1, metal_left // per_loop)
    steps: list = [("deploy", {"item": p.mould}, True)]
    steps += [("deploy", ingot(p.material))] * per_loop
    steps.append(("press",))
    if p.kind == BORE:
        steps.append(("cut",))
    elif p.kind == RECOIL:
        steps.append(("deploy", {"item": p.spring}))
    base = p.base if p.base is not None else ingot(p.material)
    return base, steps, loops


def drop_self_loot(block_id: str, mod: str) -> dict:
    return {
        "neoforge:conditions": mod_loaded(mod),
        "type": "minecraft:block",
        "pools": [{
            "bonus_rolls": 0.0,
            "conditions": [{"condition": "minecraft:survives_explosion"}],
            "entries": [{"type": "minecraft:item", "name": block_id}],
            "rolls": 1.0,
        }],
        "random_sequence": f"{block_id.split(':')[0]}:blocks/{block_id.split(':')[1]}",
    }


def write_json(path: str, data: dict) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ----------------------------------------------------------------------------------------

def check_unique_starts(parts: list[Part], comps: list[Component]) -> None:
    """Create picks a sequenced recipe by its base item and its first step.
    Two recipes with the same pair would be ambiguous, so fail early."""
    seen: dict[str, str] = {}
    entries = [(p.result, part_plan(p)[0], part_plan(p)[1][0]) for p in parts]
    entries += [(f"{c.mod}:{c.item}", c.base, c.steps[0]) for c in comps]
    for result, base, first in entries:
        key = json.dumps([base, list(first)], sort_keys=True)
        if key in seen:
            sys.exit(f"Ambiguous recipe start: {result} and {seen[key]}")
        seen[key] = result


def generate() -> tuple[list[Part], list[Component]]:
    parts = build_parts()
    comps = build_components()
    check_unique_starts(parts, comps)

    names = [p.unfinished for p in parts] + [c.unfinished for c in comps]
    dupes = {n for n in names if names.count(n) > 1}
    if dupes:
        sys.exit(f"Duplicate unfinished item names: {sorted(dupes)}")

    if os.path.isdir(GEN_RES):
        shutil.rmtree(GEN_RES)
    recipe_dir = os.path.join(GEN_RES, "data", MOD_ID, "recipe")
    model_dir = os.path.join(GEN_RES, "assets", MOD_ID, "models", "item")

    for p in parts:
        base, steps, loops = part_plan(p)
        transitional = f"{MOD_ID}:{p.unfinished}"
        write_json(os.path.join(recipe_dir, "sequenced_assembly", p.mod, f"{p.block}.json"),
                   sequenced(mod_loaded(p.mod), base, steps, loops, transitional, p.result))
        write_json(os.path.join(model_dir, f"{p.unfinished}.json"), {"parent": f"{p.mod}:item/{p.block}"})
        if p.mod in (CBCAT, NW):
            write_json(os.path.join(GEN_RES, "data", p.mod, "loot_table", "blocks", f"{p.block}.json"),
                       drop_self_loot(p.result, p.mod))

    for c in comps:
        transitional = f"{MOD_ID}:{c.unfinished}"
        write_json(os.path.join(recipe_dir, "sequenced_assembly", c.mod, "components", f"{c.key}.json"),
                   sequenced(mod_loaded(c.mod), c.base, c.steps, c.loops, transitional, f"{c.mod}:{c.item}"))
        write_json(os.path.join(model_dir, f"{c.unfinished}.json"), {"parent": f"{c.mod}:item/{c.item}"})

    for mould in NW_MOULDS:
        write_json(os.path.join(recipe_dir, "cutting", NW, f"{mould}.json"), {
            "neoforge:conditions": mod_loaded(NW),
            "type": "create:cutting",
            "ingredients": [{"tag": "minecraft:logs"}],
            "results": [{"id": f"{NW}:{mould}"}],
        })

    for mould in CBCAT_BROKEN_MOULDS:
        write_json(os.path.join(recipe_dir, "cutting", CBCAT, f"{mould}.json"), {
            "neoforge:conditions": mod_loaded(CBCAT),
            "type": "create:cutting",
            "ingredients": [{"tag": "minecraft:logs"}],
            "results": [{"id": f"{CBCAT}:{mould}"}],
        })

    # Moulds are blocks too; addon loot tables are at the pre-1.21 path, so add working ones.
    moulds = sorted({p.mould for p in parts if p.mod in (CBCAT, NW)})
    for mould in moulds:
        mod, path = mould.split(":")
        write_json(os.path.join(GEN_RES, "data", mod, "loot_table", "blocks", f"{path}.json"),
                   drop_self_loot(mould, mod))

    write_java(parts, comps)
    write_docs(parts, comps)
    return parts, comps


def write_java(parts: list[Part], comps: list[Component]) -> None:
    rows = [(p.unfinished, p.mod, p.block) for p in parts] + [(c.unfinished, c.mod, c.item) for c in comps]
    lines = [
        "package io.github.iksanbro.cbcparts;",
        "",
        "import java.util.List;",
        "",
        "/**",
        " * Unfinished items used as transitional items of the sequenced assembly recipes.",
        " * Generated by tools/generate_data.py, do not edit by hand.",
        " */",
        "public final class GeneratedParts {",
        "    private GeneratedParts() {}",
        "",
        "    /** One unfinished item: its own name, the mod of the finished item and its id there. */",
        "    public record UnfinishedPart(String name, String modId, String targetPath) {}",
        "",
        "    public static final List<UnfinishedPart> ALL = List.of(",
    ]
    for i, (name, mod, target) in enumerate(rows):
        comma = "," if i < len(rows) - 1 else ""
        lines.append(f'            new UnfinishedPart("{name}", "{mod}", "{target}"){comma}')
    lines += ["    );", "}", ""]
    with open(JAVA_OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


STEP_RU = {"press": "пресс", "cut": "пила"}
MOD_TITLE = {CBC: "Create Big Cannons", CBCAT: "CBC: Advanced Technologies", NW: "CBC Neo Warfare"}


def ing_name(ing: dict) -> str:
    return f"`#{ing['tag']}`" if "tag" in ing else f"`{ing['item']}`"


def steps_text(steps: list) -> str:
    out = []
    for st in steps:
        if st[0] == "deploy":
            keep = " (остаётся в деплоере)" if len(st) > 2 and st[2] else ""
            out.append(f"деплоер: {ing_name(st[1])}{keep}")
        else:
            out.append(STEP_RU[st[0]])
    return " → ".join(out)


def write_docs(parts: list[Part], comps: list[Component]) -> None:
    lines = [
        "# Рецепты последовательной сборки",
        "",
        "Файл создан `tools/generate_data.py`, руками не править.",
        "",
        "Каждая строка: стартовый предмет, затем шаги одного цикла, число циклов и итоговый расход слитков.",
        "Форма (cast mould) держится в деплоере и не тратится.",
        "",
    ]
    for mod in (CBC, CBCAT, NW):
        lines += [f"## {MOD_TITLE[mod]}", "", "| Деталь | Старт | Шаги одного цикла | Циклов | Слитков |", "|---|---|---|---|---|"]
        for p in (p for p in parts if p.mod == mod):
            base, steps, loops = part_plan(p)
            per_loop = sum(1 for st in steps if st[0] == "deploy" and st[1] == ingot(p.material))
            total = f"{per_loop * loops} + механизм" if p.base else str(1 + per_loop * loops)
            lines.append(f"| `{p.result}` | {ing_name(base)} | {steps_text(steps)} | {loops} | {total} |")
        lines.append("")
    lines += ["## Компоненты Neo Warfare (в 1.21.1 у них нет рецептов)", "",
              "| Предмет | Старт | Шаги одного цикла | Циклов |", "|---|---|---|---|"]
    for c in comps:
        lines.append(f"| `{c.mod}:{c.item}` | {ing_name(c.base)} | {steps_text(c.steps)} | {c.loops} |")
    lines += ["", "## Формы", "",
              "Распил любого бревна (`#minecraft:logs`) механической пилой:", ""]
    lines += [f"- `{NW}:{m}`" for m in NW_MOULDS]
    lines += [f"- `{CBCAT}:{m}` (рецепт CBC:AT 0.1.4c не загружается)" for m in CBCAT_BROKEN_MOULDS]
    lines.append("")
    with open(os.path.join(ROOT, "RECIPES.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


# ----------------------------------------------------------------------------------------
# Validation against extracted jars

def collect_ids(dirs: list[str]) -> set[str]:
    ids: set[str] = set()
    for d in dirs:
        assets = os.path.join(d, "assets")
        if not os.path.isdir(assets):
            continue
        for ns in os.listdir(assets):
            models = os.path.join(assets, ns, "models", "item")
            if os.path.isdir(models):
                ids |= {f"{ns}:{f[:-5]}" for f in os.listdir(models) if f.endswith(".json")}
    return ids


def validate(dirs: list[str], parts: list[Part], comps: list[Component]) -> int:
    known = collect_ids(dirs) | {"create:andesite_alloy"}
    needed: set[str] = set()
    for p in parts:
        needed |= {p.result, p.mould}
        if p.base and "item" in p.base:
            needed.add(p.base["item"])
        if p.spring:
            needed.add(p.spring)
    for c in comps:
        needed.add(f"{c.mod}:{c.item}")
        if "item" in c.base:
            needed.add(c.base["item"])
    for m in NW_MOULDS:
        needed.add(f"{NW}:{m}")
    for m in CBCAT_BROKEN_MOULDS:
        needed.add(f"{CBCAT}:{m}")
    missing = sorted(i for i in needed if i not in known)
    for i in missing:
        print("MISSING:", i)
    print(f"checked {len(needed)} item ids, missing {len(missing)}")
    return 1 if missing else 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--validate", nargs="*", default=None, help="folders with extracted mod jars")
    args = ap.parse_args()
    parts, comps = generate()
    print(f"parts: {len(parts)}, helper components: {len(comps)}, nw moulds: {len(NW_MOULDS)}")
    if args.validate:
        return validate(args.validate, parts, comps)
    return 0


if __name__ == "__main__":
    sys.exit(main())
