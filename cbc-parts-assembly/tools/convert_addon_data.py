#!/usr/bin/env python3
"""Converts the pre-1.21 data that CBC: Advanced Technologies and CBC Neo Warfare ship.

Both addons keep their block loot tables in `data/<ns>/loot_tables/` and their block tags in
`data/<ns>/tags/blocks/`, the 1.20 paths. Minecraft 1.21 reads `loot_table/` and `tags/block/`,
so none of their blocks drop anything when broken or taken with the Create wrench, and blocks
that need a pickaxe are not in the pickaxe tag. The same goes for Neo Warfare's item, fluid and
entity type tags (`tags/items`, `tags/fluids`, `tags/entity_types`): its rounds, shells and
cartridges are not in the tags that decide what a cannon can be loaded with. This script writes
the same data at the 1.21 paths, converted to the 1.21 format, plus fixed copies of addon
recipes that point at a wrong recipe type.

Run from the project root with the extracted addon jars:

    python3 tools/convert_addon_data.py path/to/cbc_at path/to/neo_warfare

Output: src/generated/addon_fixes (overwritten; committed with the project).
"""

from __future__ import annotations

import glob
import json
import os
import re
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "generated", "addon_fixes")

# mod id of each addon, found from the namespace of its loot tables
ADDON_NAMESPACES = {"cbc_at", "cbcmodernwarfare"}

# Create Big Cannons' own blocks that need a pickaxe but are missing from the pickaxe tag
CBC_UNTAGGED_PICKAXE = ["createbigcannons:mortar_stone", "createbigcannons:mortar_stone_projectile"]

# Loot tables whose file name differs from the block they belong to (typos in the addons)
TABLE_RENAMES = {
    "cbc_at:bronze_rocket_breech_rail": "cbc_at:bronze_rocket_pod_breech",
}
TABLE_RENAME_PATTERNS = [
    # CBC:AT heavy autocannon moulds: tables are named *_cast_mould, the blocks *_mould
    (re.compile(r"^cbc_at:(heavy_autocannon_[a-z_]+)_cast_mould$"), r"cbc_at:\1_mould"),
]

# 1.20 tag folders and their 1.21 names
TAG_DIRS = {"blocks": "block", "items": "item", "fluids": "fluid", "entity_types": "entity_type"}
# NeoForge 1.21 uses the c: namespace; forge: tags are not read by anything any more
SKIP_TAG_NAMESPACES = {"forge"}

# Recipes of the addons that point at a recipe type that does not exist
FIXED_RECIPES = {
    # CBC:AT registers the rocket fuzing recipe type as cbc_at:rocket_fuzing, its recipe file
    # says cbc_at:rocket_munition_fuzing, so fuzes cannot be put into rockets on a crafting table
    "cbcparts/recipe/fixes/cbc_at/rocket_fuzing.json": {
        "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": "cbc_at"}],
        "type": "cbc_at:rocket_fuzing",
        "category": "misc",
    },
}

# Tables of blocks that have no item in the game (the table would only log a parse error)
SKIP_TABLES = {"cbcmodernwarfare:torpedo_screw_breech"}

# Blocks with an item but no loot table at all in the addon; they drop themselves like the
# addon's other moulds
DROP_SELF = [
    "cbc_at:heavy_autocannon_muzzle_brake_mould",
    "cbc_at:heavy_autocannon_silencer_mould",
    "cbc_at:heavy_autocannon_qfbreech_mould",
]

# Blocks that need a pickaxe but are in no tool tag, not even in the addons' old tag files
UNTAGGED_PICKAXE_PATTERNS = [
    re.compile(r"^cbc_at:(unbored_)?[a-z_]+_rocket_pod_(rail|breech)$"),
    re.compile(r"^cbc_at:wrought_iron_medium_rocket_rail(_breech)?$"),
    re.compile(r"^cbcmodernwarfare:rotarycannon_ammo_drum$"),
]

SILK_TOUCH_1_20 = {"enchantments": [{"enchantment": "minecraft:silk_touch", "levels": {"min": 1}}]}
SILK_TOUCH_1_21 = {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}


def convert(node, problems: list, where: str):
    """Returns a 1.21 copy of a 1.20 loot table node."""
    if isinstance(node, list):
        return [convert(n, problems, where) for n in node]
    if not isinstance(node, dict):
        return node
    if node.get("function") == "minecraft:copy_nbt":
        # Block entity data became data components: copy all of them from the block entity
        # (fuze, tracer, fluid, ammo...), the same thing the copy_nbt ops did.
        out = {"function": "minecraft:copy_components", "source": "block_entity"}
        if "conditions" in node:
            out["conditions"] = convert(node["conditions"], problems, where)
        return out
    if node.get("condition") == "minecraft:match_tool":
        if node.get("predicate") == SILK_TOUCH_1_20:
            return {"condition": "minecraft:match_tool", "predicate": SILK_TOUCH_1_21}
        problems.append(f"{where}: unknown match_tool predicate {node.get('predicate')}")
    return {k: convert(v, problems, where) for k, v in node.items()}


def write_json(path: str, data) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def known_blocks(dirs: list[str]) -> set[str]:
    blocks = set()
    for d in dirs:
        for f in glob.glob(os.path.join(d, "assets", "*", "blockstates", "*.json")):
            ns = f.split(os.sep)[-3]
            blocks.add(f"{ns}:{os.path.basename(f)[:-5]}")
    return blocks


def rename_table(block: str) -> str:
    if block in TABLE_RENAMES:
        return TABLE_RENAMES[block]
    for pattern, repl in TABLE_RENAME_PATTERNS:
        if pattern.match(block):
            return pattern.sub(repl, block)
    return block


def main(dirs: list[str]) -> int:
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    problems: list[str] = []
    tables = 0
    skipped: list[str] = []
    tags: dict[str, list] = {}
    blocks = known_blocks(dirs)
    for d in dirs:
        data = os.path.join(d, "data")
        for f in sorted(glob.glob(os.path.join(data, "*", "loot_tables", "**", "*.json"), recursive=True)):
            rel = os.path.relpath(f, data).split(os.sep)
            ns, path = rel[0], "/".join(rel[2:])
            if ns not in ADDON_NAMESPACES:
                continue
            text = open(f, encoding="utf-8").read()
            if path.startswith("blocks/"):
                old = f"{ns}:{path[len('blocks/'):-len('.json')]}"
                new = rename_table(old)
                if new not in blocks or new in SKIP_TABLES:
                    skipped.append(old)  # table of a block the addon does not have
                    continue
                if new != old:
                    # the item named in the table had the same typo
                    text = text.replace(f'"{old}"', f'"{new}"').replace(f'"{ns}:blocks/{old.split(":")[1]}"', f'"{ns}:blocks/{new.split(":")[1]}"')
                    path = f"blocks/{new.split(':')[1]}.json"
            table = convert(json.loads(text), problems, f"{ns}:{path}")
            table = {"neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": ns}], **table}
            write_json(os.path.join(OUT, "data", ns, "loot_table", path), table)
            tables += 1
        for old_dir, new_dir in TAG_DIRS.items():
            for f in sorted(glob.glob(os.path.join(data, "*", "tags", old_dir, "**", "*.json"), recursive=True)):
                rel = os.path.relpath(f, data).split(os.sep)
                if rel[0] in SKIP_TAG_NAMESPACES:
                    continue
                tag = f"{rel[0]}/tags/{new_dir}/" + "/".join(rel[3:])
                for v in json.load(open(f, encoding="utf-8")).get("values", []):
                    tags.setdefault(tag, []).append(v)
        # casting time of molten carbon steel, written for the 1.20 forge: fluid tag
        for f in sorted(glob.glob(os.path.join(data, "forge", "fluid_casting_time", "tags", "*.json"))):
            write_json(os.path.join(OUT, "data", "c", "fluid_casting_time", "tags", os.path.basename(f)),
                       json.load(open(f, encoding="utf-8")))
    for block in DROP_SELF:
        ns, name = block.split(":")
        write_json(os.path.join(OUT, "data", ns, "loot_table", "blocks", f"{name}.json"), {
            "neoforge:conditions": [{"type": "neoforge:mod_loaded", "modid": ns}],
            "type": "minecraft:block",
            "pools": [{"bonus_rolls": 0.0, "conditions": [{"condition": "minecraft:survives_explosion"}],
                       "entries": [{"type": "minecraft:item", "name": block}], "rolls": 1.0}],
            "random_sequence": f"{ns}:blocks/{name}",
        })
        tables += 1
    tags.setdefault("minecraft/tags/block/mineable/pickaxe.json", []).extend(CBC_UNTAGGED_PICKAXE)
    tags["minecraft/tags/block/mineable/pickaxe.json"].extend(
        sorted(b for b in blocks if any(p.match(b) for p in UNTAGGED_PICKAXE_PATTERNS)))
    for tag, values in tags.items():
        # optional entries: a tag with a missing required entry fails to load as a whole
        entries = []
        for v in values:
            v = v if isinstance(v, dict) else {"id": v}
            entries.append({"id": v["id"], "required": False})
        unique = list({e["id"]: e for e in entries}.values())
        write_json(os.path.join(OUT, "data", *tag.split("/")), {"replace": False, "values": unique})
    for path, recipe in FIXED_RECIPES.items():
        write_json(os.path.join(OUT, "data", *path.split("/")), recipe)
    for p in problems:
        print("PROBLEM:", p)
    print(f"loot tables: {tables}, block tags: {len(tags)}, skipped tables of missing blocks: {skipped}")
    return 1 if problems else 0


if __name__ == "__main__":
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    sys.exit(main(sys.argv[1:]))
