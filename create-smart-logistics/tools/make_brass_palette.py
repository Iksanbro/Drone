#!/usr/bin/env python3
"""Builds the brass palettes of the smart blocks (textures/palette/).

The smart blocks look like Create's packager, frogport and stock ticker in brass. Create's textures
are not shipped with this mod (Create's assets are All Rights Reserved): the game recolours Create's
own textures when it loads them, with a vanilla "paletted_permutations" sprite source
(assets/minecraft/atlases/blocks.json). This script makes the palettes, two per texture (Create
uses the same colour for parts that turn into different brass colours): <name>_key.png has every colour
of the Create texture that turns to brass, <name>_brass.png its brass colour, one pixel each.

Usage: python3 tools/make_brass_palette.py <create textures/block dir> <brass textures dir>
  the brass textures dir holds the recoloured textures to reproduce, named like Create's
  (packager_details.png, port.png, ...). Needs Pillow.
"""
import os
import sys

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "smartlogistics", "textures", "palette")
TEXTURES = ["packager_details", "packager_particle", "port", "port2", "stock_ticker"]


def main(create_dir: str, brass_dir: str) -> int:
    os.makedirs(OUT, exist_ok=True)
    for name in TEXTURES:
        original = Image.open(os.path.join(create_dir, name + ".png")).convert("RGBA")
        brass = Image.open(os.path.join(brass_dir, name + ".png")).convert("RGBA")
        if original.size != brass.size:
            raise SystemExit(f"{name}: sizes differ")
        pairs = list(zip(original.getdata(), brass.getdata()))
        mapping: dict = {}
        for a, b in pairs:
            if a != b and mapping.setdefault(a, b) != b:
                raise SystemExit(f"{name}: colour {a} turns into two colours")
        for a, b in pairs:
            if a == b and a in mapping:
                raise SystemExit(f"{name}: colour {a} is kept in one place and changed in another")
        keys = sorted(mapping)
        key_img = Image.new("RGBA", (len(keys), 1))
        pal_img = Image.new("RGBA", (len(keys), 1))
        for i, k in enumerate(keys):
            key_img.putpixel((i, 0), k)
            pal_img.putpixel((i, 0), mapping[k])
        key_img.save(os.path.join(OUT, f"{name}_key.png"))
        pal_img.save(os.path.join(OUT, f"{name}_brass.png"))
        print(f"{name}: {len(keys)} colours")
    return 0


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1], sys.argv[2]))
