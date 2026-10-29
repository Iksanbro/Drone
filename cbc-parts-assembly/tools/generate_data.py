#!/usr/bin/env python3
"""Generates every data file of the mod from one list of cannon parts.

Run from the project root:

    python3 tools/generate_data.py [--validate DIR ...]

Outputs (all overwritten on every run):
  * src/generated/resources/data/cbcparts/recipe/...      sequenced assembly recipes
  * src/generated/resources/assets/cbcparts/models/item/  models of unfinished items
  * src/main/java/.../GeneratedParts.java                 list of unfinished items to register
  * RECIPES.md                                            human readable list of the recipes

Rules the recipes follow:
  * Only parts that are made exclusively in the world (cast in a Cannon Cast and/or bored
    with a Cannon Drill) get a recipe, and only when that world process actually works.
    Parts whose world process is broken or missing get nothing.
  * The recipes are added next to the world process, nothing is replaced.
  * They use the same resources as the world process: the cast mould (returned after
    casting, so it stays in the deployer), the metal, and the item the incomplete block
    needs (breech extractor, spring...). Only the metal is tripled; everything else keeps
    its original amount.

--validate takes folders with extracted mod jars (CBC, CBC:AT, Neo Warfare) and checks
that every item id used by the recipes really exists in them.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import sys
from dataclasses import dataclass

MOD_ID = "cbcparts"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
GEN_RES = os.path.join(ROOT, "src", "generated", "resources")
JAVA_OUT = os.path.join(ROOT, "src", "main", "java", "io", "github", "iksanbro", "cbcparts", "GeneratedParts.java")

CBC = "createbigcannons"
CBCAT = "cbc_at"
NW = "cbcmodernwarfare"
# Neo Warfare 1.21.1 ships no working casting/boring for medium cannons; this mod adds it.
NW_FIX = "cbcmw_recipe_fix"

CAST_IRON, BRONZE, STEEL, NETHERSTEEL = "cast_iron", "bronze", "steel", "nethersteel"
AUTOCANNON_MATERIALS = [CAST_IRON, BRONZE, STEEL]

METAL_MULTIPLIER = 3
INGOTS_PER_BLOCK = 9
MAX_METAL_DEPLOYS_PER_LOOP = 4

# CBC:AT 0.1.4c ships these mould recipes in a format 1.21.1 cannot parse. Without the mould
# the part cannot be cast, so its world process is broken and it gets no recipe.
CBCAT_BROKEN_MOULDS = {
    f"{CBCAT}:twin_autocannon_barrel_mould",
    f"{CBCAT}:rocket_pod_rail_mould",
    f"{CBCAT}:rocket_pod_breech_mould",
}


def ingot(material: str) -> dict:
    return {"tag": f"c:ingots/{material}"}


def storage_block(material: str) -> dict:
    return {"tag": f"c:storage_blocks/{material}"}


@dataclass
class Part:
    mods: tuple          # mods that must be loaded; the first one owns the part
    block: str           # id of the finished part (block item)
    material: str
    mould: str           # full id of the cast mould
    ingots: int          # metal of the casting (cast shape volume / 90 mB)
    component: str | None = None  # item the incomplete block needs after boring (one)

    @property
    def mod(self) -> str:
        return self.mods[0]

    @property
    def result(self) -> str:
        return f"{self.mod}:{self.block}"

    @property
    def unfinished(self) -> str:
        return f"unfinished_{self.block}"


@dataclass
class Skipped:
    result: str
    reason: str


def build_parts() -> tuple[list[Part], list[Skipped]]:
    parts: list[Part] = []
    skipped: list[Skipped] = []

    def add(part: Part) -> None:
        if part.mould in CBCAT_BROKEN_MOULDS:
            skipped.append(Skipped(part.result, f"рецепт формы `{part.mould}` в CBC:AT 0.1.4c не загружается"))
        else:
            parts.append(part)

    # --- Create Big Cannons: autocannons -----------------------------------------------
    # casting volumes: barrel 270 mB, breech 360 mB, recoil spring 360 mB
    for m in AUTOCANNON_MATERIALS:
        add(Part((CBC,), f"{m}_autocannon_barrel", m, f"{CBC}:autocannon_barrel_cast_mould", 3))
        add(Part((CBC,), f"{m}_autocannon_breech", m, f"{CBC}:autocannon_breech_cast_mould", 4,
                 component=f"{CBC}:{m}_autocannon_breech_extractor"))
        add(Part((CBC,), f"{m}_autocannon_recoil_spring", m, f"{CBC}:autocannon_recoil_spring_cast_mould", 4,
                 component=f"{CBC}:recoil_spring"))

    # --- CBC: Advanced Technologies (cast + bored, no incomplete stage) ---------------
    for m in AUTOCANNON_MATERIALS:
        add(Part((CBCAT,), f"{m}_autocannon_silencer", m, f"{CBCAT}:autocannon_silencer_mould", 3))
        add(Part((CBCAT,), f"{m}_autocannon_muzzle_brake", m, f"{CBCAT}:autocannon_muzzle_brake_mould", 3))
        for twin in ("twin", "vert_twin"):
            for piece in ("barrel", "recoil_spring", "breech", "silencer", "muzzle_brake"):
                cost = 8 if twin == "vert_twin" and piece in ("recoil_spring", "breech", "silencer") else 6
                add(Part((CBCAT,), f"{m}_{twin}_autocannon_{piece}", m,
                         f"{CBCAT}:{twin}_autocannon_{piece}_mould", cost))
        for piece in ("barrel", "muzzle_brake", "silencer", "recoil_spring", "breech", "qfbreech"):
            add(Part((CBCAT,), f"{m}_heavy_autocannon_{piece}", m, f"{CBCAT}:heavy_autocannon_{piece}_mould", 6))
        add(Part((CBCAT,), f"{m}_rocket_pod_rail", m, f"{CBCAT}:rocket_pod_rail_mould", 10))
        add(Part((CBCAT,), f"{m}_rocket_pod_breech", m, f"{CBCAT}:rocket_pod_breech_mould", 10))

    # --- CBC Neo Warfare: medium cannons (world process comes from CBCMW recipe fix) ---
    for m in (CAST_IRON, BRONZE, STEEL, NETHERSTEEL):
        add(Part((NW, NW_FIX), f"{m}_mediumcannon_barrel", m, f"{NW}:mediumcannon_barrel_cast_mould", 3))
        add(Part((NW, NW_FIX), f"{m}_mediumcannon_breech", m, f"{NW}:mediumcannon_breech_cast_mould", 4,
                 component=f"{NW}:{m}_mediumcannon_falling_breech"))
        add(Part((NW, NW_FIX), f"{m}_mediumcannon_recoil_barrel", m, f"{NW}:mediumcannon_recoil_barrel_cast_mould", 4,
                 component=f"{NW}:heavy_recoil_spring"))

    # --- CBC Neo Warfare: rotary cannons ------------------------------------------------
    for m in (BRONZE, STEEL, NETHERSTEEL):
        for piece in ("barrel", "breech", "bearing"):
            skipped.append(Skipped(f"{NW}:{m}_rotarycannon_{piece}",
                                   "в Neo Warfare 1.21.1 нет литья, форм и экстрактора; CBCMW recipe fix их не добавляет"))
    return parts, skipped


# ----------------------------------------------------------------------------------------
# Recipe plan

def metal_deploys(material: str, amount: int) -> list:
    blocks, ingots = divmod(amount, INGOTS_PER_BLOCK)
    return [("deploy", storage_block(material))] * blocks + [("deploy", ingot(material))] * ingots


def part_plan(p: Part) -> tuple[dict, list, int]:
    """Returns (base ingredient, steps of one loop, loops).

    The base is a storage block (9 ingots); the remaining metal is deployed in the loops.
    Every loop: mould in the deployer (kept), metal, press, saw (= boring). A part that needs
    an item after boring gets it once, so its recipe has a single loop.
    """
    metal = p.ingots * METAL_MULTIPLIER
    rest = metal - INGOTS_PER_BLOCK
    if rest < 0:
        raise ValueError(f"{p.result}: less metal than one block")
    mould = ("deploy", {"item": p.mould}, True)
    if p.component:
        steps = [mould] + metal_deploys(p.material, rest) + [("press",), ("cut",), ("deploy", {"item": p.component})]
        if len(steps) > 8:
            raise ValueError(f"{p.result}: too many steps for one loop")
        return storage_block(p.material), steps, 1
    for loops in range(1, rest + 2):
        if rest % loops:
            continue
        per_loop = metal_deploys(p.material, rest // loops)
        if len(per_loop) <= MAX_METAL_DEPLOYS_PER_LOOP:
            return storage_block(p.material), [mould] + per_loop + [("press",), ("cut",)], loops
    raise ValueError(f"{p.result}: no plan")


def plan_metal(p: Part) -> int:
    base, steps, loops = part_plan(p)
    per_loop = 0
    for st in steps:
        if st[0] == "deploy" and st[1] == ingot(p.material):
            per_loop += 1
        elif st[0] == "deploy" and st[1] == storage_block(p.material):
            per_loop += INGOTS_PER_BLOCK
    return INGOTS_PER_BLOCK + per_loop * loops


def plan_component_count(p: Part) -> int:
    _, steps, loops = part_plan(p)
    return sum(1 for st in steps if p.component and st[0] == "deploy" and st[1] == {"item": p.component}) * loops


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


def write_json(path: str, data: dict) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


# ----------------------------------------------------------------------------------------

def check(parts: list[Part]) -> None:
    """Create picks a sequenced recipe by its base item and first step: those pairs must be
    unique. Also re-check the cost rules."""
    seen: dict[str, str] = {}
    for p in parts:
        base, steps, _ = part_plan(p)
        key = json.dumps([base, list(steps[0])], sort_keys=True)
        if key in seen:
            sys.exit(f"Ambiguous recipe start: {p.result} and {seen[key]}")
        seen[key] = p.result
        if plan_metal(p) != p.ingots * METAL_MULTIPLIER:
            sys.exit(f"{p.result}: metal {plan_metal(p)} != {p.ingots} x {METAL_MULTIPLIER}")
        if plan_component_count(p) != (1 if p.component else 0):
            sys.exit(f"{p.result}: component used {plan_component_count(p)} times")
    names = [p.unfinished for p in parts]
    if len(names) != len(set(names)):
        sys.exit("Duplicate unfinished item names")


def generate() -> tuple[list[Part], list[Skipped]]:
    parts, skipped = build_parts()
    check(parts)

    if os.path.isdir(GEN_RES):
        shutil.rmtree(GEN_RES)
    recipe_dir = os.path.join(GEN_RES, "data", MOD_ID, "recipe", "sequenced_assembly")
    model_dir = os.path.join(GEN_RES, "assets", MOD_ID, "models", "item")

    for p in parts:
        base, steps, loops = part_plan(p)
        transitional = f"{MOD_ID}:{p.unfinished}"
        write_json(os.path.join(recipe_dir, p.mod, f"{p.block}.json"),
                   sequenced(mod_loaded(*p.mods), base, steps, loops, transitional, p.result))
        write_json(os.path.join(model_dir, f"{p.unfinished}.json"), {"parent": f"{p.mod}:item/{p.block}"})

    write_java(parts)
    write_docs(parts, skipped)
    return parts, skipped


def write_java(parts: list[Part]) -> None:
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
        "    /**",
        "     * One unfinished item: its own name, the mods its recipe needs (the first one owns the",
        "     * finished part), the id of the finished part, the ingots the world casting uses and the",
        "     * ingots the recipe uses.",
        "     */",
        "    public record UnfinishedPart(String name, List<String> requiredMods, String targetPath,",
        "                                 int castingIngots, int recipeIngots) {",
        "        public String modId() {",
        "            return requiredMods.get(0);",
        "        }",
        "    }",
        "",
        "    public static final List<UnfinishedPart> ALL = List.of(",
    ]
    for i, p in enumerate(parts):
        comma = "," if i < len(parts) - 1 else ""
        mods = ", ".join(f'"{m}"' for m in p.mods)
        lines.append(f'            new UnfinishedPart("{p.unfinished}", List.of({mods}), "{p.block}", '
                     f'{p.ingots}, {plan_metal(p)}){comma}')
    lines += ["    );", "}", ""]
    with open(JAVA_OUT, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines))


STEP_RU = {"press": "пресс", "cut": "пила"}
MOD_TITLE = {CBC: "Create Big Cannons", CBCAT: "CBC: Advanced Technologies",
             NW: "CBC Neo Warfare (нужен CBCMW recipe fix)"}


def ing_name(ing: dict) -> str:
    return f"`#{ing['tag']}`" if "tag" in ing else f"`{ing['item']}`"


def steps_text(steps: list) -> str:
    out = []
    for st in steps:
        if st[0] == "deploy":
            keep = " (остаётся)" if len(st) > 2 and st[2] else ""
            out.append(f"деплоер: {ing_name(st[1])}{keep}")
        else:
            out.append(STEP_RU[st[0]])
    return " → ".join(out)


def write_docs(parts: list[Part], skipped: list[Skipped]) -> None:
    lines = [
        "# Рецепты последовательной сборки",
        "",
        "Файл создан `tools/generate_data.py`, руками не править.",
        "",
        "Рецепт есть только у деталей, которые делаются исключительно в мире (литьё и сверление),",
        "и только если этот процесс в мире работает. Рецепт добавлен рядом с литьём, ничего не заменено.",
        "",
        "Ресурсы те же, что у литья. Металл ×3, остальное в прежнем количестве:",
        "форма держится в деплоере и не тратится (при литье она тоже возвращается),",
        "предмет для incomplete-блока (экстрактор, пружина) тратится один раз.",
        "Старт: блок материала (9 слитков).",
        "",
    ]
    for mod in (CBC, CBCAT, NW):
        lines += [f"## {MOD_TITLE[mod]}", "",
                  "| Деталь | Литьё, слитков | Рецепт, слитков | Шаги одного цикла | Циклов |",
                  "|---|---|---|---|---|"]
        for p in (p for p in parts if p.mod == mod):
            _, steps, loops = part_plan(p)
            lines.append(f"| `{p.result}` | {p.ingots} | {plan_metal(p)} | {steps_text(steps)} | {loops} |")
        lines.append("")
    lines += ["## Без рецепта", "", "Процесс в мире сломан или отсутствует, поэтому мод ничего не добавляет.", "",
              "| Деталь | Причина |", "|---|---|"]
    lines += [f"| `{s.result}` | {s.reason} |" for s in skipped]
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


def validate(dirs: list[str], parts: list[Part]) -> int:
    known = collect_ids(dirs)
    needed: set[str] = set()
    for p in parts:
        needed |= {p.result, p.mould}
        if p.component:
            needed.add(p.component)
    missing = sorted(i for i in needed if i not in known)
    for i in missing:
        print("MISSING:", i)
    print(f"checked {len(needed)} item ids, missing {len(missing)}")
    return 1 if missing else 0


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--validate", nargs="*", default=None, help="folders with extracted mod jars")
    args = ap.parse_args()
    parts, skipped = generate()
    print(f"recipes: {len(parts)}, skipped parts: {len(skipped)}")
    if args.validate:
        return validate(args.validate, parts)
    return 0


if __name__ == "__main__":
    sys.exit(main())
