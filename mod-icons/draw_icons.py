#!/usr/bin/env python3
"""Draws the icons of the four mods (512x512 PNG): a white-ringed blue badge with a grid on a dark
navy square, and the mod's subject in the middle.

Usage: python3 mod-icons/draw_icons.py   (needs Pillow)
Writes mod-icons/<mod>.png and each mod's src/main/resources/icon.png.
"""
import math
import os
import shutil

from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SS = 4                      # supersampling
SIZE = 512
W = SIZE * SS
C = W / 2

NAVY = (11, 24, 44)
RING = (236, 244, 255)
BLUE_IN = (66, 150, 236)
BLUE_OUT = (14, 72, 160)
GRID = (150, 205, 255)

STEEL_DARK = (38, 42, 50)
STEEL = (70, 77, 90)
STEEL_LIGHT = (132, 142, 160)
GOLD = (246, 176, 46)
GOLD_DARK = (176, 104, 18)
BRASS = (222, 170, 72)
BRASS_DARK = (150, 100, 34)
BRASS_LIGHT = (250, 214, 130)


def s(v: float) -> float:
    return v * SS


# ------------------------------------------------------------------ badge

def badge() -> Image.Image:
    img = Image.new("RGBA", (W, W), NAVY + (255,))
    r_out, r_in = s(232), s(214)
    # inner radial gradient
    grad = Image.new("RGBA", (W, W))
    gp = grad.load()
    for y in range(0, W, 2):
        for x in range(0, W, 2):
            d = min(1.0, math.hypot(x - C + s(40), y - C + s(50)) / r_in)
            col = tuple(int(BLUE_IN[i] + (BLUE_OUT[i] - BLUE_IN[i]) * d ** 1.4) for i in range(3)) + (255,)
            gp[x, y] = col
            gp[x + 1, y] = col
            gp[x, y + 1] = col
            gp[x + 1, y + 1] = col
    # grid lines
    gd = ImageDraw.Draw(grad)
    step = s(34)
    k = -10
    while k * step < W + step:
        pos = C + k * step
        gd.line([(pos, 0), (pos, W)], fill=GRID + (90,), width=int(s(1.6)))
        gd.line([(0, pos), (W, pos)], fill=GRID + (90,), width=int(s(1.6)))
        k += 1
    # darker rim inside the ring
    rim = Image.new("L", (W, W), 0)
    ImageDraw.Draw(rim).ellipse([C - r_in, C - r_in, C + r_in, C + r_in], fill=255)
    inner = Image.new("L", (W, W), 0)
    ImageDraw.Draw(inner).ellipse([C - r_in + s(18), C - r_in + s(18), C + r_in - s(18), C + r_in - s(18)], fill=255)
    inner = inner.filter(ImageFilter.GaussianBlur(s(14)))
    shade = Image.new("RGBA", (W, W), (5, 25, 70, 0))
    shade.putalpha(Image.eval(inner, lambda v: int((255 - v) * 0.55)))
    grad = Image.alpha_composite(grad, shade)

    d = ImageDraw.Draw(img)
    # soft glow behind the ring
    glow = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([C - r_out - s(6), C - r_out - s(6), C + r_out + s(6), C + r_out + s(6)], fill=(90, 160, 255, 120))
    img = Image.alpha_composite(img, glow.filter(ImageFilter.GaussianBlur(s(8))))
    d = ImageDraw.Draw(img)
    d.ellipse([C - r_out, C - r_out, C + r_out, C + r_out], fill=RING + (255,))
    img.paste(grad, (0, 0), rim)
    return img


def circle_mask() -> Image.Image:
    m = Image.new("L", (W, W), 0)
    r = s(214)
    ImageDraw.Draw(m).ellipse([C - r, C - r, C + r, C + r], fill=255)
    return m


# ------------------------------------------------------------------ geometry helpers

def rot(points, cx, cy, ang):
    ca, sa = math.cos(ang), math.sin(ang)
    return [(cx + x * ca - y * sa, cy + x * sa + y * ca) for x, y in points]


def shadow_of(layer: Image.Image, dx=10, dy=14, blur=12, alpha=120) -> Image.Image:
    a = layer.getchannel("A").point(lambda v: v * alpha // 255)
    sh = Image.new("RGBA", layer.size, (0, 10, 30, 0))
    sh.putalpha(a)
    sh = sh.filter(ImageFilter.GaussianBlur(s(blur)))
    out = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    out.paste(sh, (int(s(dx)), int(s(dy))))
    return out


def shaded_band(d, pts_fn, colors, n=24):
    """Draws a cylinder-like body as n strips across its width, darkest at the edges."""
    for i in range(n):
        t0, t1 = i / n, (i + 1) / n
        tm = (t0 + t1) / 2
        # light from the upper left: bright strip at 30%, dark edges
        light = max(0.0, 1 - abs(tm - 0.3) / 0.7)
        col = mix(colors[0], colors[1], light ** 0.8)
        if 0.2 < tm < 0.32:
            col = mix(col, colors[2], 0.6)
        d.polygon(pts_fn(t0, t1), fill=col + (255,))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# ------------------------------------------------------------------ subjects

def shell(layer, cx, cy, ang, length, radius, body=(STEEL_DARK, STEEL_LIGHT, (190, 200, 215)), studs=True,
          band=None):
    """A shell lying along ang: flat base at -length/2, ogive nose at +length/2."""
    d = ImageDraw.Draw(layer)
    body_len = length * 0.62
    nose_len = length - body_len
    x0 = -length / 2

    def strip(t0, t1):
        y0, y1 = -radius + 2 * radius * t0, -radius + 2 * radius * t1
        pts = []
        # body rectangle part
        pts += [(x0, y0), (x0 + body_len, y0)]
        # nose: ogive, half width shrinks as sqrt
        steps = 24
        for k in range(1, steps + 1):
            u = k / steps
            w = math.sqrt(max(0.0, 1 - u ** 2))
            pts.append((x0 + body_len + nose_len * u, y0 * w))
        for k in range(steps, 0, -1):
            u = k / steps
            w = math.sqrt(max(0.0, 1 - u ** 2))
            pts.append((x0 + body_len + nose_len * u, y1 * w))
        pts += [(x0 + body_len, y1), (x0, y1)]
        return rot(pts, cx, cy, ang)

    shaded_band(d, strip, body)
    # base rim
    rim = [(x0 - radius * 0.06, -radius * 1.04), (x0 + radius * 0.22, -radius * 1.04),
           (x0 + radius * 0.22, radius * 1.04), (x0 - radius * 0.06, radius * 1.04)]
    d.polygon(rot(rim, cx, cy, ang), fill=STEEL_DARK + (255,))
    if band:
        bx = x0 + body_len * 0.22
        d.polygon(rot([(bx, -radius * 1.02), (bx + radius * 0.35, -radius * 1.02),
                       (bx + radius * 0.35, radius * 1.02), (bx, radius * 1.02)], cx, cy, ang), fill=band + (255,))
    if studs:
        for px, py in [(0.12, -0.55), (0.12, 0.25), (0.42, -0.2), (0.42, 0.6), (0.7, -0.5), (0.7, 0.3)]:
            x = x0 + length * px
            y = radius * py
            (sx, sy), = rot([(x, y)], cx, cy, ang)
            rr = radius * 0.17
            d.ellipse([sx - rr * 1.25, sy - rr * 1.25, sx + rr * 1.25, sy + rr * 1.25], fill=(25, 22, 18, 255))
            d.ellipse([sx - rr, sy - rr, sx + rr, sy + rr], fill=GOLD + (255,))
            d.ellipse([sx - rr * 0.8, sy - rr * 0.8, sx + rr * 0.2, sy + rr * 0.2], fill=(255, 220, 120, 255))


def wrench(layer, cx, cy, ang, length, width):
    d = ImageDraw.Draw(layer)
    half = length / 2

    def strip(t0, t1):
        y0, y1 = -width / 2 + width * t0, -width / 2 + width * t1
        return rot([(-half * 0.8, y0), (half * 0.8, y0), (half * 0.8, y1), (-half * 0.8, y1)], cx, cy, ang)

    shaded_band(d, strip, ((90, 98, 112), (214, 222, 235), (245, 248, 255)))
    for end, flip in ((half * 0.8, 1), (-half * 0.8, -1)):
        (hx, hy), = rot([(end, 0)], cx, cy, ang)
        r = width * 1.25
        d.ellipse([hx - r, hy - r, hx + r, hy + r], fill=(170, 180, 196, 255), outline=(70, 76, 90, 255), width=int(s(3)))
        # jaw opening
        jaw = [(end + flip * r * 0.2, -r * 0.45), (end + flip * r * 1.4, -r * 0.6),
               (end + flip * r * 1.4, r * 0.6), (end + flip * r * 0.2, r * 0.45)]
        d.polygon(rot(jaw, cx, cy, ang), fill=(0, 0, 0, 0))
    return d


def cut_jaws(layer, cx, cy, ang, length, width):
    """Clears the wrench jaws (drawn as filled circles) to leave the U shape."""
    mask = Image.new("L", layer.size, 0)
    md = ImageDraw.Draw(mask)
    half = length / 2
    for end, flip in ((half * 0.8, 1), (-half * 0.8, -1)):
        r = width * 1.25
        jaw = [(end + flip * r * 0.15, -r * 0.42), (end + flip * r * 1.5, -r * 0.55),
               (end + flip * r * 1.5, r * 0.55), (end + flip * r * 0.15, r * 0.42)]
        md.polygon(rot(jaw, cx, cy, ang), fill=255)
    a = layer.getchannel("A")
    a = Image.composite(Image.new("L", layer.size, 0), a, mask)
    layer.putalpha(a)


def barrel(layer, cx, cy, ang, length, radius):
    """A cannon barrel with reinforcing rings and a muzzle, along ang."""
    d = ImageDraw.Draw(layer)
    half = length / 2

    def strip_for(xa, xb, ra, rb):
        def strip(t0, t1):
            ya0, ya1 = -ra + 2 * ra * t0, -ra + 2 * ra * t1
            yb0, yb1 = -rb + 2 * rb * t0, -rb + 2 * rb * t1
            return rot([(xa, ya0), (xb, yb0), (xb, yb1), (xa, ya1)], cx, cy, ang)
        return strip

    body = (STEEL_DARK, STEEL_LIGHT, (190, 200, 215))
    # breech (wider), tube (tapering), muzzle (wider)
    shaded_band(d, strip_for(-half, -half * 0.45, radius * 1.25, radius * 1.25), body)
    shaded_band(d, strip_for(-half * 0.45, half * 0.85, radius, radius * 0.82), body)
    shaded_band(d, strip_for(half * 0.85, half, radius * 1.02, radius * 1.02), body)
    # brass reinforcing rings
    for x in (-half * 0.45, -half * 0.05, half * 0.4):
        rr = radius * (1.08 - 0.12 * (x + half) / length)
        shaded_band(d, strip_for(x, x + radius * 0.32, rr, rr), (BRASS_DARK, BRASS, BRASS_LIGHT), n=16)
    # bore
    (mx, my), = rot([(half, 0)], cx, cy, ang)
    br = radius * 0.55
    pts = []
    for k in range(32):
        a = 2 * math.pi * k / 32
        pts.append((half + math.cos(a) * br * 0.35, math.sin(a) * br))
    d.polygon(rot(pts, cx, cy, ang), fill=(12, 14, 18, 255))
    # breech knob
    (kx, ky), = rot([(-half - radius * 0.35, 0)], cx, cy, ang)
    kr = radius * 0.5
    d.ellipse([kx - kr, ky - kr, kx + kr, ky + kr], fill=STEEL + (255,), outline=STEEL_DARK + (255,), width=int(s(4)))


def gear(layer, cx, cy, r, teeth, col, col_dark, hole=0.35):
    d = ImageDraw.Draw(layer)
    pts = []
    for k in range(teeth * 4):
        a = 2 * math.pi * k / (teeth * 4)
        rr = r if (k % 4) in (1, 2) else r * 0.8
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=col_dark + (255,))
    pts2 = [(cx + (x - cx) * 0.9, cy + (y - cy) * 0.9) for x, y in pts]
    d.polygon(pts2, fill=col + (255,))
    hr = r * hole
    d.ellipse([cx - hr, cy - hr, cx + hr, cy + hr], fill=col_dark + (255,))
    hr2 = hr * 0.55
    d.ellipse([cx - hr2, cy - hr2, cx + hr2, cy + hr2], fill=(30, 26, 20, 255))


def starburst(layer, cx, cy, r_out, r_in, points, col, rot0=0.0):
    d = ImageDraw.Draw(layer)
    pts = []
    for k in range(points * 2):
        a = rot0 + math.pi * k / points
        rr = r_out if k % 2 == 0 else r_in
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=col + (255,))


def package(layer, cx, cy, size):
    """An isometric brass-trimmed cardboard package."""
    d = ImageDraw.Draw(layer)
    a = size
    h = size * 0.95
    top = [(cx, cy - a * 0.55 - h * 0.5), (cx + a, cy - h * 0.5), (cx, cy + a * 0.55 - h * 0.5), (cx - a, cy - h * 0.5)]
    left = [(cx - a, cy - h * 0.5), (cx, cy + a * 0.55 - h * 0.5), (cx, cy + a * 0.55 + h * 0.5), (cx - a, cy + h * 0.5)]
    right = [(cx + a, cy - h * 0.5), (cx, cy + a * 0.55 - h * 0.5), (cx, cy + a * 0.55 + h * 0.5), (cx + a, cy + h * 0.5)]
    card, card_l, card_d = (200, 150, 96), (226, 184, 128), (150, 104, 60)
    d.polygon(left, fill=card + (255,))
    d.polygon(right, fill=card_d + (255,))
    d.polygon(top, fill=card_l + (255,))
    # brass straps
    def lerp(p, q, t):
        return (p[0] + (q[0] - p[0]) * t, p[1] + (q[1] - p[1]) * t)
    strap_w = 0.16
    # strap across the top (front-left to back-right) and down both faces
    tl, tr, tb, tlft = top[0], top[1], top[2], top[3]
    s0, s1 = 0.5 - strap_w / 2, 0.5 + strap_w / 2
    strap_top = [lerp(tlft, tl, s0), lerp(tlft, tl, s1), lerp(tb, tr, s1), lerp(tb, tr, s0)]
    d.polygon(strap_top, fill=BRASS_LIGHT + (255,))
    rl0, rl1 = lerp(tb, tr, s0), lerp(tb, tr, s1)
    strap_right = [rl0, rl1, (rl1[0], rl1[1] + h), (rl0[0], rl0[1] + h)]
    d.polygon(strap_right, fill=BRASS_DARK + (255,))
    ll0, ll1 = lerp(tlft, tb, s0), lerp(tlft, tb, s1)
    strap_top2 = [ll0, ll1, lerp(tl, tr, s1), lerp(tl, tr, s0)]
    d.polygon(strap_top2, fill=BRASS + (255,))
    strap_left = [ll0, ll1, (ll1[0], ll1[1] + h), (ll0[0], ll0[1] + h)]
    d.polygon(strap_left, fill=BRASS + (255,))
    # edges
    for poly in (top, left, right):
        d.line(poly + [poly[0]], fill=(90, 60, 30, 255), width=int(s(3)))
    # address label on the left face
    lab = [lerp(left[0], left[1], 0.12), lerp(left[0], left[1], 0.36),
           (lerp(left[0], left[1], 0.36)[0], lerp(left[0], left[1], 0.36)[1] + h * 0.45),
           (lerp(left[0], left[1], 0.12)[0], lerp(left[0], left[1], 0.12)[1] + h * 0.45)]
    d.polygon([(x, y + h * 0.18) for x, y in lab], fill=(245, 240, 228, 255))
    for i in range(3):
        y0 = lab[0][1] + h * (0.27 + i * 0.1)
        d.line([(lab[0][0] + s(8), y0 + (lab[1][1] - lab[0][1]) * 0.15), (lab[1][0] - s(8), y0 + (lab[1][1] - lab[0][1]) * 0.85)],
               fill=(120, 110, 100, 255), width=int(s(3)))


def tornado(layer, cx, cy, height, width):
    d = ImageDraw.Draw(layer)
    rings = 9
    for i in range(rings):
        t = i / (rings - 1)
        y = cy - height / 2 + height * t
        w = width * (1 - t * 0.82)
        hh = w * 0.28
        off = math.sin(t * 3.2) * width * 0.12
        col = mix((235, 242, 252), (140, 152, 170), t * 0.7)
        d.ellipse([cx + off - w, y - hh, cx + off + w, y + hh], fill=mix(col, (90, 100, 118), 0.35) + (255,))
        d.ellipse([cx + off - w * 0.92, y - hh * 0.95, cx + off + w * 0.92, y + hh * 0.55], fill=col + (255,))
    # debris
    for (px, py, r) in [(-0.55, 0.1, 0.05), (0.6, -0.15, 0.04), (0.45, 0.35, 0.035), (-0.4, 0.42, 0.03)]:
        x, y = cx + px * width, cy + py * height
        rr = r * width
        d.polygon([(x - rr, y), (x, y - rr), (x + rr, y + rr * 0.3)], fill=(110, 84, 58, 255))


# ------------------------------------------------------------------ icons

def compose(draw_subject) -> Image.Image:
    base = badge()
    subject = Image.new("RGBA", (W, W), (0, 0, 0, 0))
    draw_subject(subject)
    base = Image.alpha_composite(base, shadow_of(subject))
    base = Image.alpha_composite(base, subject)
    return base.resize((SIZE, SIZE), Image.LANCZOS)


def icon_parts(layer):
    barrel(layer, C - s(6), C + s(6), math.radians(-38), s(330), s(46))
    w = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    wrench(w, C + s(10), C + s(18), math.radians(40), s(250), s(28))
    cut_jaws(w, C + s(10), C + s(18), math.radians(40), s(250), s(28))
    layer.alpha_composite(shadow_of(w, 6, 8, 6, 140))
    layer.alpha_composite(w)


def icon_interception(layer):
    # a shell coming up from the lower left, hit in flight: burst at the meeting point
    starburst(layer, C + s(48), C - s(50), s(118), s(62), 12, (255, 196, 64), 0.1)
    starburst(layer, C + s(48), C - s(50), s(80), s(42), 12, (255, 236, 150), 0.35)
    shell(layer, C - s(42), C + s(40), math.radians(-40), s(270), s(46))
    # a small round coming down from the upper right
    shell(layer, C + s(112), C - s(112), math.radians(135), s(96), s(17), studs=False,
          body=((60, 52, 30), (210, 170, 80), (250, 225, 150)))
    d = ImageDraw.Draw(layer)
    # crosshair
    r = s(150)
    d.ellipse([C - r, C - r, C + r, C + r], outline=(255, 255, 255, 170), width=int(s(5)))
    for a in range(4):
        ang = a * math.pi / 2 + math.pi / 4
        d.line([(C + math.cos(ang) * r * 0.82, C + math.sin(ang) * r * 0.82),
                (C + math.cos(ang) * r * 1.18, C + math.sin(ang) * r * 1.18)], fill=(255, 255, 255, 190), width=int(s(6)))


def icon_logistics(layer):
    gear(layer, C + s(96), C - s(96), s(70), 10, BRASS, BRASS_DARK)
    package(layer, C - s(10), C + s(20), s(130))
    gear(layer, C - s(118), C + s(120), s(46), 8, BRASS_LIGHT, BRASS_DARK)


def icon_weather(layer):
    tornado(layer, C - s(8), C + s(6), s(290), s(118))
    w = Image.new("RGBA", layer.size, (0, 0, 0, 0))
    wrench(w, C + s(40), C + s(70), math.radians(-30), s(230), s(26))
    cut_jaws(w, C + s(40), C + s(70), math.radians(-30), s(230), s(26))
    layer.alpha_composite(shadow_of(w, 6, 8, 6, 140))
    layer.alpha_composite(w)


ICONS = {
    "cbc-parts-assembly": icon_parts,
    "cbc-interception": icon_interception,
    "create-smart-logistics": icon_logistics,
    "weather-physics-sable-fix": icon_weather,
}


def main() -> None:
    for mod, fn in ICONS.items():
        img = compose(fn)
        out = os.path.join(HERE, f"{mod}.png")
        img.save(out)
        res = os.path.join(ROOT, mod, "src", "main", "resources")
        os.makedirs(res, exist_ok=True)
        shutil.copy(out, os.path.join(res, "icon.png"))
        print("drawn", mod)


if __name__ == "__main__":
    main()
