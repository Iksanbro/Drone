#!/usr/bin/env python3
"""Data of the medium cannon shells (rounds and cartridges for CBC Neo Warfare's medium cannon).

Each medium round is made after a big shell and is half as strong. Input: the munition properties of
the big shells in tools/medium_shells/big/<mod>/<name>.json (Create Big Cannons defaults, and for
CBC Enchanced Shells the server's tuned files where there are some, else the mod's defaults).

Output (src/generated/medium_shells, rewritten each run):
  data/cbcparts/munition_properties/projectiles/medium_<name>.json  half of the big shell
  data/cbcparts/recipe/medium_shells/...  big shell -> 2 rounds; deployer: round + filled cartridge
  data/cbcparts/tags/...  medium_shells (entity types), medium_rounds, medium_cartridges
  assets/cbcparts/models/item/medium_<name>_{round,cartridge}.json
and the medium shell keys of src/main/resources/assets/cbcparts/lang/{en_us,ru_ru}.json.

Must list the same shells as MediumShell.java (a game test checks every round has properties).
"""
import json
import math
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BIG = os.path.join(ROOT, "tools", "medium_shells", "big")
OUT = os.path.join(ROOT, "src", "generated", "medium_shells")
LANG = os.path.join(ROOT, "src", "main", "resources", "assets", "cbcparts", "lang")
MOD_ID = "cbcparts"
CBC = "createbigcannons"
ES = "cbc_enhanced_shells"
NW = "cbcmodernwarfare"

ROMAN = ["I", "II", "III", "IV", "V"]

# name, kind, (english name, russian name), texture base, has bottom texture
CBC_SHELLS = [
    ("shrapnel_shell", "SHRAPNEL", ("Shrapnel Shell", "Шрапнельный снаряд"), "shrapnel_shell", True),
    ("fluid_shell", "FLUID", ("Fluid Shell", "Жидкостный снаряд"), "fluid_shell", True),
    ("solid_shot", "SHOT", ("Solid Shot", "Тяжёлый снаряд"), "solid_shot", True),
    ("drop_mortar_shell", "HE", ("Drop Mortar Shell", "Миномётный снаряд"), "drop_mortar_shell", True),
    ("mortar_stone", "MORTAR_STONE", ("Mortar Stone", "Миномётный камень"), "mortar_stone", False),
]
ES_TYPES = [
    ("he_shell", "HE", ("HE Shell", "Фугасный снаряд"), "he_shell", True),
    ("ap_shell", "AP_SHELL", ("AP Shell", "Бронебойно-фугасный снаряд"), "ap_shell", True),
    ("ap_shot", "SHOT", ("AP Shot", "Бронебойный снаряд"), "ap_shot", True),
    ("solid_shot", "SHOT", ("Solid Shot", "Тяжёлый снаряд"), "solid_shot", True),
    ("shrapnel_shell", "SHRAPNEL", ("Shrapnel Shell", "Шрапнельный снаряд"), "shrapnel_shell", True),
    ("smoke_shell", "SMOKE", ("Smoke Shell", "Дымовой снаряд"), "smoke_shell", True),
    ("drop_mortar_shell", "HE", ("Drop Mortar Shell", "Миномётный снаряд"), "drop_mortar_shell", True),
    ("bag_of_grapeshot", "GRAPESHOT", ("Bag of Grapeshot", "Мешок с картечью"), "grapeshot", False),
    ("mortar_stone", "MORTAR_STONE", ("Mortar Stone", "Миномётный камень"), "mortar_stone", False),
    ("cluster_shell", "CLUSTER", ("Cluster Shell", "Кассетный снаряд"), "cluster_shell", True),
    ("bunker_buster_shell", "BUNKER_BUSTER", ("Bunker Buster", "Бетонобойный снаряд"), "bunker_buster_shell", True),
]

# Cluster and bunker buster shells of CBC Enchanced Shells keep these per Mk in code, not in data.
CLUSTER_COUNT = [6, 9, 12, 16, 20]
CLUSTER_SUB_MK = [1, 1, 2, 3, 4]          # Mk of the HE shell the sub-projectiles are
BUNKER_DRILL_DEPTH = [10, 18, 28, 40, 56]
BUNKER_FINAL_POWER = [18, 24, 32, 42, 54]

# Big shells' smoke is capped at this size by Create Big Cannons (and Neo Warfare's medium smoke)
MAX_SMOKE_SCALE = 20

BALLISTICS = ["gravity", "drag", "quadratic_drag", "durability_mass", "penetration", "toughness", "deflection"]
DAMAGE = ["entity_damage", "renders_invulnerable", "ignores_invulnerability", "ignores_entity_armor", "knockback"]
MEDIUM = ["added_recoil", "can_squib"]
# combat values halved; flight (gravity, drag), spreads and flags are kept
HALVED = {"durability_mass", "penetration", "toughness", "entity_damage", "knockback", "explosive_power",
          "entity_damaging_explosive_power", "block_damaging_explosive_power", "shrapnel_count",
          "grapeshot_count", "smoke_scale", "smoke_duration", "fluid_shell_capacity"}
INTS = {"shrapnel_count", "grapeshot_count", "smoke_duration", "fluid_shell_capacity", "cluster_count", "drill_depth",
        "millibuckets_per_fluid_blob", "millibuckets_per_area_of_effect_radius"}
# What Create Big Cannons uses when a key is missing from a big shell's file
DEFAULTS = {"gravity": -0.05, "drag": 0.0, "quadratic_drag": False, "durability_mass": 1.0, "penetration": 1.0,
            "toughness": 1.0, "deflection": 1.0, "entity_damage": 1.0, "renders_invulnerable": True,
            "ignores_invulnerability": False, "ignores_entity_armor": False, "knockback": 2.0,
            "added_recoil": 1.0, "can_squib": True}
# A bag of grapeshot has no flight of its own (it bursts at the muzzle); its round gets these
GRAPESHOT_FLIGHT = {"gravity": -0.05, "drag": 0.01, "quadratic_drag": False, "durability_mass": 1.0, "penetration": 0.0,
                    "toughness": 0.0, "deflection": 0.0, "entity_damage": 0.0, "renders_invulnerable": False,
                    "ignores_invulnerability": True, "ignores_entity_armor": False, "knockback": 0.0}


def half(key: str, value):
    if isinstance(value, bool) or key not in HALVED:
        return value
    if key in INTS:
        return max(1, math.ceil(value / 2))
    v = value / 2
    return int(v) if float(v).is_integer() else round(v, 4)


def number(key: str, value):
    if key in INTS:
        return int(value)
    return int(value) if isinstance(value, (int, float)) and not isinstance(value, bool) and float(value).is_integer() else value


def load_big(mod: str, name: str) -> dict:
    with open(os.path.join(BIG, mod, f"{name}.json"), encoding="utf-8") as f:
        return json.load(f)


def explosion_keys(big: dict) -> list:
    if "explosive_power" in big:
        return ["explosive_power"]
    return ["entity_damaging_explosive_power", "block_damaging_explosive_power"]


def medium_properties(kind: str, big: dict, mk: int | None) -> dict:
    """Munition properties of the medium round: half of the big shell's, in the keys its kind reads."""
    keys = BALLISTICS + DAMAGE + MEDIUM
    extra: dict = {}
    if kind in ("HE", "AP_SHELL", "SHRAPNEL", "MORTAR_STONE", "FLUID", "CLUSTER"):
        keys += explosion_keys(big)
    if kind == "SHRAPNEL":
        keys += ["shrapnel_spread", "shrapnel_count"]
    if kind == "GRAPESHOT":
        keys += ["grapeshot_spread", "grapeshot_count"]
        big = dict(GRAPESHOT_FLIGHT, **big)
    if kind == "SMOKE":
        keys += ["smoke_scale", "smoke_duration"]
        big = dict(big, smoke_scale=min(big["smoke_scale"], MAX_SMOKE_SCALE))
    if kind == "FLUID":
        keys += ["fluid_shell_capacity", "millibuckets_per_fluid_blob", "millibuckets_per_area_of_effect_radius",
                 "fluid_blob_spread"]
    if kind == "CLUSTER":
        i = mk - 1
        extra = {
            "cluster_count": max(1, math.ceil(CLUSTER_COUNT[i] / 2)),
            "cluster_spread": 20 + 4 * mk,  # cone half angle, degrees (as the big cluster shell)
            "cluster_projectile": f"{MOD_ID}:medium_he_shell_mk{CLUSTER_SUB_MK[i]}",
        }
    if kind == "BUNKER_BUSTER":
        i = mk - 1
        extra = {"explosive_power": half("explosive_power", BUNKER_FINAL_POWER[i]),
                 "drill_depth": max(1, math.ceil(BUNKER_DRILL_DEPTH[i] / 2))}
    out = {}
    for k in keys:
        v = big.get(k, DEFAULTS.get(k))
        if v is None:
            raise KeyError(f"{k} missing for {kind}")
        out[k] = number(k, half(k, v))
    out.update(extra)
    return out


def burst_properties(big: dict) -> dict:
    """Shrapnel and grapeshot fragments of medium rounds: half the damage and strength of the big ones'."""
    return {k: number(k, half(k, v)) for k, v in big.items()}


def shells() -> list[dict]:
    out = []
    for name, kind, names, tex, bottom in CBC_SHELLS:
        out.append(dict(name=name, kind=kind, mod=CBC, big=f"{CBC}:{name}", mk=None, names=names,
                        tex=f"{CBC}:block/projectile/{tex}", bottom=bottom, big_props=load_big(CBC, name)))
    for t, kind, (en, ru), tex, bottom in ES_TYPES:
        for mk in range(1, 6):
            name = f"{t}_mk{mk}"
            out.append(dict(name=name, kind=kind, mod=ES, big=f"{ES}:{name}", mk=mk,
                            names=(f"{en} Mk.{ROMAN[mk - 1]}", f"{ru} Mk.{ROMAN[mk - 1]}"),
                            tex=f"{ES}:block/projectile/{tex}_mk{mk}", bottom=bottom, big_props=load_big(ES, name)))
    return out


def textures(s: dict) -> tuple[str, str, str]:
    base = s["tex"]
    side = base + "_side" if s["name"].startswith("drop_mortar_shell") else base
    top = base + "_top"
    return side, top, (base + "_bottom" if s["bottom"] else top)


ROUND_DISPLAY = {
    "thirdperson_righthand": {"rotation": [88, 1, 2], "translation": [0, 0.75, 1], "scale": [0.4, 0.4, 0.4]},
    "thirdperson_lefthand": {"rotation": [88, 1, 2], "translation": [0, 0.75, 1], "scale": [0.4, 0.4, 0.4]},
    "gui": {"rotation": [50, 40, -30], "scale": [0.73, 0.73, 0.73]},
    "fixed": {"rotation": [90, 0, 0]},
}
CARTRIDGE_DISPLAY = {
    "thirdperson_righthand": {"translation": [0, -4, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_lefthand": {"translation": [0, -4, 0], "scale": [0.5, 0.5, 0.5]},
    "ground": {"rotation": [-90, 0, 0], "translation": [0, 0, 2.5], "scale": [0.4, 0.4, 0.4]},
    "gui": {"rotation": [-50, 0, 30], "translation": [2, -2, 0], "scale": [0.58, 0.58, 0.58]},
    "fixed": {"translation": [0, -5, 0], "scale": [0.6, 0.6, 0.6]},
}


def round_model(s: dict) -> dict:
    """Same shape as Neo Warfare's medium rounds, painted with the big shell's block textures."""
    side, top, bottom = textures(s)
    face = {"uv": [0, 0, 16, 16], "texture": "#side"}
    return {
        "parent": "minecraft:block/block",
        "textures": {"side": side, "top": top, "bottom": bottom, "particle": top},
        "elements": [{
            "from": [5, 5, 2.5], "to": [11, 11, 13.5],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#bottom"},
                "east": dict(face, rotation=90), "west": dict(face, rotation=270),
                "up": face, "down": dict(face, rotation=180),
            },
        }],
        "display": ROUND_DISPLAY,
    }


def cartridge_model(s: dict) -> dict:
    """Neo Warfare's cartridge (brass case) with the round on top."""
    side, top, bottom = textures(s)
    case = "#case"
    return {
        "parent": "minecraft:block/block",
        "texture_size": [32, 32],
        "textures": {"case": f"{NW}:item/cartridge_filled", "side": side, "top": top, "bottom": bottom, "particle": top},
        "elements": [
            {"from": [4, 0, 4], "to": [12, 16, 12], "faces": {
                **{d: {"uv": [0, 4, 4, 12], "texture": case} for d in ("north", "east", "south", "west")},
                "up": {"uv": [0, 0, 4, 4], "texture": case}, "down": {"uv": [0, 12, 4, 16], "texture": case}}},
            {"from": [5, 16, 5], "to": [11, 21, 11], "faces": {
                **{d: {"uv": [4, 0, 7, 2.5], "texture": case} for d in ("north", "east", "south", "west")},
                "up": {"uv": [4, 2.5, 7, 5.5], "texture": case}}},
            {"from": [5, 21, 5], "to": [11, 32, 11], "faces": {
                **{d: {"uv": [0, 0, 16, 16], "texture": "#side"} for d in ("north", "east", "south", "west")},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"}, "down": {"uv": [0, 0, 16, 16], "texture": "#bottom"}}},
        ],
        "display": CARTRIDGE_DISPLAY,
    }


def conditions(s: dict) -> list:
    mods = [NW] + ([ES] if s["mod"] == ES else [])
    return [{"type": "neoforge:mod_loaded", "modid": m} for m in mods]


def write_json(path: str, data) -> None:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2, ensure_ascii=False)
        f.write("\n")


def update_lang(file: str, entries: dict) -> None:
    path = os.path.join(LANG, file)
    with open(path, encoding="utf-8") as f:
        lang = json.load(f)
    lang = {k: v for k, v in lang.items() if not k.startswith(("item.cbcparts.medium_", "entity.cbcparts.medium_"))}
    lang.update(entries)
    write_json(path, dict(sorted(lang.items())))


def special(kind: str, p: dict) -> str:
    if kind == "SHRAPNEL":
        return f"шрапнель: {p['shrapnel_count']} шт., разлёт {p['shrapnel_spread']}"
    if kind == "GRAPESHOT":
        return f"картечь у ствола: {p['grapeshot_count']} шт., разлёт {p['grapeshot_spread']}"
    if kind == "SMOKE":
        return f"дым: размер {p['smoke_scale']}, {p['smoke_duration']} тиков"
    if kind == "FLUID":
        return f"жидкость до {p['fluid_shell_capacity']} мВ, капли по {p['millibuckets_per_fluid_blob']} мВ"
    if kind == "CLUSTER":
        sub = p["cluster_projectile"].split("medium_he_shell_mk")[1]
        return f"делится на {p['cluster_count']} фугасных Mk.{ROMAN[int(sub) - 1]} (средних), конус {p['cluster_spread']}°"
    if kind == "BUNKER_BUSTER":
        return f"сверлит {p['drill_depth']} блоков, потом взрыв"
    if kind == "MORTAR_STONE":
        return "взрыв при ударе о блок, без взрывателя"
    if kind == "SHOT":
        return "без взрыва, без взрывателя"
    if kind == "AP_SHELL":
        return "взрыв после пробития"
    return ""


def explosion_text(p: dict) -> str:
    if "explosive_power" in p:
        return str(p["explosive_power"])
    if "entity_damaging_explosive_power" in p:
        return f"{p['entity_damaging_explosive_power']} / {p['block_damaging_explosive_power']}"
    return "—"


def write_table(all_shells: list, props: dict) -> None:
    lines = [
        "# Снаряды средних пушек",
        "",
        "Сгенерировано `tools/generate_medium_shells.py` из параметров больших снарядов (`tools/medium_shells/big/`), руками не править.",
        "",
        "Взрыв: одно число, или «по существам / по блокам». Масса — `durability_mass`.",
        "",
        "| Снаряд | ID снаряда (патрон: `_cartridge`) | Урон | Взрыв | Пробитие | Прочность | Масса | Отдача | Особое |",
        "|---|---|---|---|---|---|---|---|---|",
    ]
    for s in all_shells:
        p = props[s["name"]]
        lines.append(f"| {s['names'][1]} | `cbcparts:medium_{s['name']}_round` | {p['entity_damage']} | {explosion_text(p)} | "
                     f"{p['penetration']} | {p['toughness']} | {p['durability_mass']} | {p['added_recoil']} | {special(s['kind'], p)} |")
    with open(os.path.join(ROOT, "MEDIUM_SHELLS.md"), "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(lines) + "\n")


def main() -> int:
    all_shells = shells()
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    data = os.path.join(OUT, "data", MOD_ID)
    models = os.path.join(OUT, "assets", MOD_ID, "models", "item")
    en: dict = {"itemGroup.cbcparts.medium_shells": "Medium Cannon Shells",
                "tooltip.cbcparts.fluid_round.empty": "Empty: fill with a spout",
                "tooltip.cbcparts.fluid_round.fluid": "%s, %s mB"}
    ru: dict = {"itemGroup.cbcparts.medium_shells": "Снаряды средних пушек",
                "tooltip.cbcparts.fluid_round.empty": "Пустой: залейте жидкость дозатором",
                "tooltip.cbcparts.fluid_round.fluid": "%s, %s мВ"}
    entity_ids = []
    props: dict = {}
    for s in all_shells:
        entity = f"medium_{s['name']}"
        round_id, cartridge_id = f"{MOD_ID}:{entity}_round", f"{MOD_ID}:{entity}_cartridge"
        entity_ids.append(f"{MOD_ID}:{entity}")
        props[s["name"]] = medium_properties(s["kind"], s["big_props"], s["mk"])
        write_json(os.path.join(data, "munition_properties", "projectiles", f"{entity}.json"), props[s["name"]])
        write_json(os.path.join(models, f"{entity}_round.json"), round_model(s))
        write_json(os.path.join(models, f"{entity}_cartridge.json"), cartridge_model(s))
        write_json(os.path.join(data, "recipe", "medium_shells", f"{entity}_round.json"), {
            "neoforge:conditions": conditions(s),
            "type": "minecraft:crafting_shapeless",
            "category": "misc",
            "ingredients": [{"item": s["big"]}],
            "result": {"id": round_id, "count": 2},
        })
        if s["kind"] != "FLUID":  # a deployer recipe has a fixed result: the fluid would be lost
            write_json(os.path.join(data, "recipe", "medium_shells", "deploying", f"{entity}_cartridge.json"), {
                "neoforge:conditions": conditions(s),
                "type": "create:deploying",
                "ingredients": [{"item": f"{NW}:filled_mediumcannon_cartridge"}, {"item": round_id}],
                "results": [{"id": cartridge_id, "count": 1, "components": {
                    f"{CBC}:projectile": [{"item": {"id": round_id, "count": 1}, "slot": 0}]}}],
            })
        en_name, ru_name = s["names"]
        en[f"item.{MOD_ID}.{entity}_round"] = f"{en_name} Mediumcannon Round"
        en[f"item.{MOD_ID}.{entity}_cartridge"] = f"{en_name} Mediumcannon Cartridge"
        en[f"entity.{MOD_ID}.{entity}"] = f"{en_name} Mediumcannon Round"
        ru[f"item.{MOD_ID}.{entity}_round"] = f"{ru_name} (средняя пушка)"
        ru[f"item.{MOD_ID}.{entity}_cartridge"] = f"Патрон: {ru_name[0].lower() + ru_name[1:]} (средняя пушка)"
        ru[f"entity.{MOD_ID}.{entity}"] = f"{ru_name} (средняя пушка)"

    for burst, big in (("medium_shrapnel_burst", "shrapnel_burst"), ("medium_grapeshot_burst", "grapeshot_burst")):
        write_json(os.path.join(data, "munition_properties", "projectiles", f"{burst}.json"),
                   burst_properties(load_big(CBC, big)))
    en["entity.cbcparts.medium_shrapnel_burst"] = "Medium Shrapnel"
    en["entity.cbcparts.medium_grapeshot_burst"] = "Medium Grapeshot"
    ru["entity.cbcparts.medium_shrapnel_burst"] = "Шрапнель среднего калибра"
    ru["entity.cbcparts.medium_grapeshot_burst"] = "Картечь среднего калибра"

    # referenced from Neo Warfare's tags and RPL's precise_motion in src/generated/addon_fixes
    tags = os.path.join(data, "tags")
    write_json(os.path.join(tags, "entity_type", "medium_shells.json"),
               {"values": [{"id": i, "required": False} for i in entity_ids]})
    write_json(os.path.join(tags, "item", "medium_rounds.json"),
               {"values": [{"id": i + "_round", "required": False} for i in entity_ids]})
    write_json(os.path.join(tags, "item", "medium_cartridges.json"),
               {"values": [{"id": i + "_cartridge", "required": False} for i in entity_ids]})
    write_table(all_shells, props)
    update_lang("en_us.json", en)
    update_lang("ru_ru.json", ru)
    print(f"medium shells: {len(all_shells)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
