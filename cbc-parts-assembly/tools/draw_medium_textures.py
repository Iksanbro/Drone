#!/usr/bin/env python3
"""Draws the textures of the medium cannon rounds (src/generated/medium_textures).

Each texture is drawn here, in the layout of CBC Neo Warfare's medium round textures (16x16: side of
the round 6x11 at 0,0 with the nose up, nose end 6x6 at 6,0, base 6x6 at 6,6), so the rounds use Neo
Warfare's own round model. Only the colours are taken from the big shell's textures (nose, body, band,
base), so a round looks like its big shell; Mk.I-V are marked with 1-5 light dots on the body.

Usage: python3 tools/draw_medium_textures.py <createbigcannons jar> <cbc_enhanced_shells jar>
Needs Pillow. The shell list is the one of generate_medium_shells.py.
"""
import io
import os
import sys
import zipfile

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import generate_medium_shells as gen  # noqa: E402

OUT = os.path.join(gen.ROOT, "src", "generated", "medium_textures", "assets", gen.MOD_ID, "textures", "item", "medium")

# the part of a big shell's 16x16 texture its block model shows (createbigcannons:block/projectile_block)
SIDE_BOX = (3, 0, 13, 16)
END_BOX = (3, 3, 13, 13)
# column shading of the side, a round seen as a cylinder
SHADE = [0.72, 1.12, 1.04, 0.98, 0.88, 0.7]
# where the Mk dots go on the side (body rows 5-6), the first n for Mk n
MK_DOTS = [(0, 5), (2, 5), (4, 5), (1, 6), (3, 6)]


class Textures:
    def __init__(self, jars: list[str]):
        self.zips = [zipfile.ZipFile(j) for j in jars]

    def get(self, rid: str) -> Image.Image:
        ns, path = rid.split(":")
        name = f"assets/{ns}/textures/{path}.png"
        for z in self.zips:
            if name in z.namelist():
                return Image.open(io.BytesIO(z.read(name))).convert("RGBA")
        raise FileNotFoundError(name)


def pixels(img: Image.Image, box) -> list:
    region = img.crop(box)
    return [p[:3] for p in region.get_flattened_data() if p[3] > 128]


def mean(px: list, default=(128, 128, 128)) -> tuple:
    if not px:
        return default
    n = len(px)
    return tuple(sum(p[i] for p in px) // n for i in range(3))


def dist(a, b) -> float:
    return sum((x - y) ** 2 for x, y in zip(a, b)) ** 0.5


def quantize(px: list, k: int = 4) -> list:
    """Main colours of the pixels, most common first (a few rounds of k-means)."""
    if not px:
        return [(128, 128, 128)]
    centers = [px[i * (len(px) - 1) // max(1, k - 1)] for i in range(k)]
    for _ in range(8):
        groups = [[] for _ in centers]
        for p in px:
            groups[min(range(len(centers)), key=lambda i: dist(p, centers[i]))].append(p)
        centers = [mean(g, c) for g, c in zip(groups, centers)]
    order = sorted(range(len(centers)), key=lambda i: -len(groups[i]))
    return [centers[i] for i in order if groups[i]]


def scale(c, f: float) -> tuple:
    return tuple(max(0, min(255, int(v * f))) for v in c) + (255,)


def palette(t: Textures, s: dict) -> dict:
    side_id, top_id, bottom_id = gen.textures(s)
    side = t.get(side_id)
    sbox = (3, 0, 13, 14) if s["name"].startswith("drop_mortar_shell") else SIDE_BOX
    body_px = pixels(side, (sbox[0], 5, sbox[2], sbox[3] - 2))
    colours = quantize(body_px)
    body = colours[0]
    band = next((c for c in colours[1:] if dist(c, body) > 45), scale(body, 0.6)[:3])
    nose = mean(pixels(side, (sbox[0], 0, sbox[2], 4)), body)
    tip = mean(pixels(t.get(top_id), (6, 6, 10, 10)), nose)
    base = mean(pixels(t.get(bottom_id), END_BOX), scale(body, 0.7)[:3])
    return {"body": body, "band": band, "nose": nose, "tip": tip, "base": base}


def draw(p: dict, mk: int | None, plain: bool) -> Image.Image:
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    put = img.putpixel
    # side, 6 x 11, nose up
    rows = []
    rows += [p["tip"], p["nose"], p["nose"]]           # 0-2 nose and fuze
    rows += [scale(p["nose"], 0.6)[:3]]                # 3 seam
    rows += [p["body"]] * 4                            # 4-7 body
    rows += [p["body"] if plain else p["band"]]        # 8 driving band
    rows += [p["body"], scale(p["base"], 0.85)[:3]]    # 9-10 base
    for y, c in enumerate(rows):
        for x in range(6):
            f = SHADE[x] * (1.12 if y == 0 else 1.0)
            put((x, y), scale(c, f))
    if mk:
        # Mk.I-V: 1-5 dots in a checker on the body, light on a dark round and dark on a light one
        r, g, b = p["body"]
        dot = (30, 30, 30, 255) if 0.3 * r + 0.59 * g + 0.11 * b > 150 else (235, 235, 225, 255)
        for x, y in MK_DOTS[:mk]:
            put((x, y), dot)
    # nose end, 6 x 6 at (6, 0): the fuze in the middle
    for y in range(6):
        for x in range(6):
            edge = x in (0, 5) or y in (0, 5)
            c = scale(p["nose"], 0.75) if edge else scale(p["tip"], 1.05)
            if 2 <= x <= 3 and 2 <= y <= 3:
                c = scale(p["tip"], 1.3)
            put((6 + x, y), c)
    # base, 6 x 6 at (6, 6): the primer in the middle
    for y in range(6):
        for x in range(6):
            edge = x in (0, 5) or y in (0, 5)
            c = scale(p["base"], 0.7 if edge else 0.95)
            if 2 <= x <= 3 and 2 <= y <= 3:
                c = scale(p["base"], 0.5)
            put((6 + x, 6 + y), c)
    return img


def main(jars: list[str]) -> int:
    t = Textures(jars)
    os.makedirs(OUT, exist_ok=True)
    shells = gen.shells()
    for s in shells:
        plain = s["kind"] in ("MORTAR_STONE", "GRAPESHOT")
        draw(palette(t, s), s["mk"], plain).save(os.path.join(OUT, f"{s['name']}.png"))
    print(f"textures: {len(shells)} in {OUT}")
    return 0


if __name__ == "__main__":
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(2)
    sys.exit(main(sys.argv[1:]))
