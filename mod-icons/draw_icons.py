#!/usr/bin/env python3
"""Draws the icons of the four mods: a white-ringed blue badge with a grid on a dark navy square,
with the mod's subject rendered in 3D in the middle.

The subjects are signed distance field scenes, ray marched with numpy: lit by a key light with soft
shadows, ambient occlusion, a blue sky reflected by the metal parts and a rim light. Rendered at twice
the size and scaled down, so edges are smooth.

Usage: python3 mod-icons/draw_icons.py [mod ...]   (needs numpy and Pillow; about a minute per icon)
Writes mod-icons/<mod>.png (1024x1024, for the CurseForge project) and each mod's
src/main/resources/icon.png (512x512).
"""
import math
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
OUT = int(os.environ.get("ICON_SIZE", "1024"))   # icon size (ICON_SIZE=256 for a quick preview)
SS = 2              # supersampling of the 3D render and the badge
R = OUT * SS        # working resolution


# =============================================================================================
# colours

def lin(c):
    """sRGB 0-255 -> linear 0-1."""
    return (np.array(c, dtype=np.float64) / 255.0) ** 2.2


def srgb(x):
    return np.clip(x, 0, 1) ** (1 / 2.2)


# =============================================================================================
# badge (2D, numpy)

NAVY = np.array([10, 22, 42]) / 255
BLUE_C = np.array([78, 162, 245]) / 255
BLUE_E = np.array([12, 62, 150]) / 255


def badge() -> Image.Image:
    n = R
    yy, xx = np.mgrid[0:n, 0:n].astype(np.float64)
    c = n / 2
    x, y = (xx - c) / c, (yy - c) / c           # -1..1
    r = np.hypot(x, y)
    img = np.zeros((n, n, 3))
    # background: navy with a faint lift in the middle
    img[:] = NAVY[None, None, :] * (1 + 0.25 * np.clip(1 - r / 1.4, 0, 1))[..., None]

    r_out, r_ring_in = 0.905, 0.835
    # outer glow of the ring
    glow = np.exp(-((r - r_out) / 0.035) ** 2) * (r > r_out)
    img += glow[..., None] * np.array([0.25, 0.5, 1.0]) * 0.45

    # inner disc: radial gradient (light from the upper left), grid, vignette, sheen
    d = np.hypot(x + 0.22, y + 0.28) / 1.25
    t = np.clip(d, 0, 1) ** 1.3
    disc = BLUE_C[None, None, :] * (1 - t)[..., None] + BLUE_E[None, None, :] * t[..., None]
    step = 0.135
    gx = np.abs(((x + 1) / step) % 1 - 0.5) * step
    gy = np.abs(((y + 1) / step) % 1 - 0.5) * step
    g = np.minimum(gx, gy)
    px = 2.0 / n
    line = np.clip(1 - (g - 0.0035) / (1.5 * px), 0, 1)
    line_glow = np.exp(-(g / 0.012) ** 2) * 0.35
    disc = disc + (line * 0.32 + line_glow * 0.18)[..., None] * np.array([0.75, 0.9, 1.0])
    # finer sub grid
    step2 = step / 4
    g2 = np.minimum(np.abs(((x + 1) / step2) % 1 - 0.5), np.abs(((y + 1) / step2) % 1 - 0.5)) * step2
    disc = disc + (np.clip(1 - (g2 - 0.0012) / px, 0, 1) * 0.07)[..., None]
    # inner shadow under the ring, darker towards the edge
    vign = np.clip((r - (r_ring_in - 0.22)) / 0.22, 0, 1) ** 2
    disc = disc * (1 - 0.45 * vign)[..., None]
    # glossy sheen across the upper half
    sheen = np.clip(1 - np.hypot(x + 0.05, (y + 0.62) * 1.7) / 0.95, 0, 1) ** 2 * 0.16
    disc = disc + sheen[..., None]

    # ring: bevelled white
    ring_t = np.clip((r - r_ring_in) / (r_out - r_ring_in), 0, 1)
    bevel = 0.92 + 0.08 * np.cos((ring_t - 0.35) * math.pi)
    light = 0.9 + 0.1 * (-(x + y) / np.maximum(r, 1e-6)) * 0.7
    ring = np.array([0.93, 0.96, 1.0])[None, None, :] * (bevel * light)[..., None]
    ring = ring * (1 - 0.18 * np.exp(-((ring_t - 0.0) / 0.12) ** 2))[..., None]   # dark inner edge

    aa = 1.5 * px
    in_disc = np.clip((r_ring_in - r) / aa, 0, 1)
    in_ring = np.clip((r_out - r) / aa, 0, 1) - in_disc
    img = img * (1 - in_disc - in_ring)[..., None] + disc * in_disc[..., None] + ring * in_ring[..., None]
    return Image.fromarray((np.clip(img, 0, 1) * 255).astype(np.uint8), "RGB").convert("RGBA")


# =============================================================================================
# signed distance fields

def rotation(rx=0.0, ry=0.0, rz=0.0):
    """Local -> world rotation from angles in degrees (applied x, then y, then z)."""
    a, b, c = (math.radians(v) for v in (rx, ry, rz))
    mx = np.array([[1, 0, 0], [0, math.cos(a), -math.sin(a)], [0, math.sin(a), math.cos(a)]])
    my = np.array([[math.cos(b), 0, math.sin(b)], [0, 1, 0], [-math.sin(b), 0, math.cos(b)]])
    mz = np.array([[math.cos(c), -math.sin(c), 0], [math.sin(c), math.cos(c), 0], [0, 0, 1]])
    return mz @ my @ mx


class Frame:
    """A placed object: local = (world - centre) rotated back."""

    def __init__(self, centre, rot):
        self.c = np.array(centre, dtype=np.float64)
        self.m = rot

    def local(self, p):
        return (p - self.c) @ self.m

    def child(self, offset, rot=None):
        return Frame(self.c + self.m @ np.array(offset, dtype=np.float64), self.m @ (rot if rot is not None else np.eye(3)))


def length(v):
    return np.sqrt(np.sum(v * v, axis=-1))


def sd_box(q, b, rr=0.0):
    d = np.abs(q) - (np.array(b) - rr)
    return length(np.maximum(d, 0)) + np.minimum(np.max(d, axis=-1), 0) - rr


def sd_cyl(q, axis, h, r, rr=0.0):
    """Capped cylinder along local axis 0/1/2, half length h, radius r, edge rounding rr."""
    o = [i for i in range(3) if i != axis]
    radial = np.sqrt(q[:, o[0]] ** 2 + q[:, o[1]] ** 2) - (r - rr)
    along = np.abs(q[:, axis]) - (h - rr)
    return np.sqrt(np.maximum(radial, 0) ** 2 + np.maximum(along, 0) ** 2) + np.minimum(np.maximum(radial, along), 0) - rr


def sd_ellipsoid(q, rad):
    rad = np.array(rad)
    k0 = length(q / rad)
    k1 = length(q / (rad * rad))
    return k0 * (k0 - 1) / np.maximum(k1, 1e-9)


def sd_sphere(q, r):
    return length(q) - r


def smin(a, b, k):
    h = np.clip(0.5 + 0.5 * (b - a) / k, 0, 1)
    return b * (1 - h) + a * h - k * h * (1 - h)


class Scene:
    """Objects as (sdf function of world points, material name)."""

    def __init__(self):
        self.parts = []

    def add(self, fn, material):
        self.parts.append((fn, material))

    def eval(self, p):
        best = np.full(len(p), 1e9)
        mat = np.zeros(len(p), dtype=np.int32)
        for i, (fn, _) in enumerate(self.parts):
            d = fn(p)
            closer = d < best
            best = np.where(closer, d, best)
            mat = np.where(closer, i, mat)
        return best, mat

    def dist(self, p):
        best = np.full(len(p), 1e9)
        for fn, _ in self.parts:
            best = np.minimum(best, fn(p))
        return best


# =============================================================================================
# materials: base colour (sRGB), metalness, roughness (0 mirror .. 1 matte)

MATERIALS = {
    "steel": (lin((128, 136, 150)), 0.9, 0.3),
    "steel_dark": (lin((92, 98, 112)), 0.9, 0.32),
    "gold": (lin((255, 178, 40)), 1.0, 0.22),
    "brass": (lin((232, 176, 78)), 1.0, 0.28),
    "brass_dark": (lin((190, 130, 48)), 1.0, 0.35),
    "copper": (lin((214, 118, 62)), 1.0, 0.3),
    "chrome": (lin((205, 212, 224)), 1.0, 0.12),
    "card": (lin((196, 146, 92)), 0.0, 0.85),
    "paper": (lin((240, 236, 226)), 0.0, 0.7),
    "ink": (lin((70, 64, 60)), 0.0, 0.8),
    "cloud": (lin((226, 233, 242)), 0.0, 0.9),
    "cloud_dark": (lin((168, 178, 194)), 0.0, 0.9),
    "dirt": (lin((112, 84, 58)), 0.0, 0.9),
    "black": (lin((14, 15, 18)), 0.0, 0.9),
}

KEY = np.array([-0.5, 0.72, 0.55])
KEY = KEY / np.linalg.norm(KEY)


def env(r):
    """Light coming from direction r (the sky the metal reflects)."""
    up = np.clip(r[:, 1] * 0.5 + 0.5, 0, 1)
    sky = np.array([0.03, 0.035, 0.05])[None] * (1 - up[:, None]) + np.array([0.62, 0.68, 0.78])[None] * up[:, None]
    # a soft box light up and to the left, and a thin horizon band
    box = np.exp(-(((r[:, 0] + 0.45) / 0.35) ** 2 + ((r[:, 1] - 0.55) / 0.25) ** 2)) * 3.2
    band = np.exp(-((r[:, 1] - 0.05) / 0.06) ** 2) * 0.6
    return sky + (box + band)[:, None]


def render(scene: Scene, size: int, fov=0.25, cam_z=10.0):
    """Returns RGBA float image (linear colour, alpha coverage)."""
    ys, xs = np.mgrid[0:size, 0:size]
    u = ((xs + 0.5) / size * 2 - 1).ravel()
    v = (1 - (ys + 0.5) / size * 2).ravel()
    dirs = np.stack([u * fov, v * fov, -np.ones_like(u)], axis=-1)
    dirs /= length(dirs)[:, None]
    origin = np.array([0, 0, cam_z])
    n = size * size
    t = np.full(n, cam_z - 3.2)
    hit = np.zeros(n, dtype=bool)
    # rays that miss the scene's bounding sphere (radius 3) are not marched
    alive = np.nonzero(np.abs(u * fov * cam_z) ** 2 + np.abs(v * fov * cam_z) ** 2 < 3.2 ** 2)[0]
    for _ in range(220):
        if len(alive) == 0:
            break
        p = origin + dirs[alive] * t[alive, None]
        d = scene.dist(p)
        t[alive] += d * 0.85
        done = d < 4e-4 * t[alive]
        hit[alive[done]] = True
        gone = t[alive] > cam_z + 3.5
        alive = alive[~(done | gone)]

    out = np.zeros((n, 4))
    idx = np.nonzero(hit)[0]
    if len(idx) == 0:
        return out.reshape(size, size, 4)
    p = origin + dirs[idx] * t[idx, None]
    _, mat = scene.eval(p)
    e = 1.5e-3
    nrm = np.stack([scene.dist(p + np.array(o)) - scene.dist(p - np.array(o)) for o in
                    ([e, 0, 0], [0, e, 0], [0, 0, e])], axis=-1)
    nrm /= np.maximum(length(nrm), 1e-9)[:, None]
    view = -dirs[idx]

    # soft shadow towards the key light
    sh = np.ones(len(idx))
    st = np.full(len(idx), 0.02)
    for _ in range(64):
        q = p + nrm * 0.003 + KEY[None] * st[:, None]
        d = scene.dist(q)
        sh = np.minimum(sh, np.clip(10 * d / st, 0, 1))
        st += np.clip(d, 0.01, 0.25)
    sh = np.clip(sh, 0, 1) ** 1.5

    # ambient occlusion
    ao = np.zeros(len(idx))
    for k in range(1, 6):
        hk = 0.035 * k
        ao += (hk - scene.dist(p + nrm * hk)) / (2 ** k)
    ao = np.clip(1 - 3.2 * ao, 0.25, 1)

    base = np.zeros((len(idx), 3))
    metal = np.zeros(len(idx))
    rough = np.zeros(len(idx))
    for i, (_, m) in enumerate(scene.parts):
        sel = mat == i
        c, me, ro = MATERIALS[m]
        base[sel] = c
        metal[sel] = me
        rough[sel] = ro

    ndl = np.clip(np.sum(nrm * KEY[None], axis=-1), 0, 1)
    ndv = np.clip(np.sum(nrm * view, axis=-1), 0, 1)
    h = KEY[None] + view
    h /= length(h)[:, None]
    ndh = np.clip(np.sum(nrm * h, axis=-1), 0, 1)
    refl = -view + 2 * np.sum(nrm * view, axis=-1)[:, None] * nrm
    shin = 8 + (1 - rough) ** 2 * 400
    spec = ndh ** shin * (shin + 2) / 8 * 0.5 * sh

    # blur the reflection for rough metal by mixing with the normal's sky
    env_r = env(refl) * (1 - rough[:, None]) + env(nrm) * rough[:, None]
    fres = 0.04 + 0.96 * (1 - ndv) ** 5
    ambient = np.array([0.3, 0.31, 0.36])[None] * (0.55 + 0.45 * nrm[:, 1:2])

    diffuse = base * (ambient + 1.25 * (ndl * sh)[:, None]) * ao[:, None]
    metallic = base * env_r * (0.35 + 0.65 * ao)[:, None] * (0.55 + 0.45 * sh)[:, None] + base * (spec * 2.2)[:, None]
    col = diffuse * (1 - metal[:, None]) + metallic * metal[:, None]
    col += ((spec * (1 - metal))[:, None] * 0.6)
    col += (fres * (1 - rough) * 0.35)[:, None] * env_r * (1 - metal[:, None])
    rim = (1 - ndv) ** 3 * 0.55
    col += rim[:, None] * np.array([0.35, 0.6, 1.0])[None] * ao[:, None]

    # filmic tone curve
    x = col * 1.05
    col = (x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14)
    out[idx, :3] = col
    out[idx, 3] = 1
    return out.reshape(size, size, 4)


def to_image(rgba: np.ndarray) -> Image.Image:
    rgb = srgb(rgba[..., :3])
    a = rgba[..., 3:4]
    arr = np.concatenate([rgb, a], axis=-1)
    return Image.fromarray((arr * 255 + 0.5).astype(np.uint8), "RGBA")


# =============================================================================================
# parts of the scenes

def shell(scene, frame: Frame, length_, radius, body="steel_dark", studs=True, band="copper"):
    """A shell along local +x: flat base at -L/2, ogive nose ending at +L/2."""
    L, Rr = length_, radius
    body_end = 0.08 * L
    nose = 0.5 * L - body_end

    def body_fn(p):
        q = frame.local(p)
        cyl = sd_cyl(q - np.array([(-0.5 * L + body_end) / 2, 0, 0]), 0, (body_end + 0.5 * L) / 2, Rr, 0.02 * Rr)
        tip = sd_ellipsoid(q - np.array([body_end, 0, 0]), (nose, Rr, Rr))
        tip = np.maximum(tip, -(q[:, 0] - body_end))
        return smin(cyl, tip, 0.02 * Rr)

    scene.add(body_fn, body)
    # base rim and driving band
    scene.add(lambda p: sd_cyl(frame.local(p) - np.array([-0.47 * L, 0, 0]), 0, 0.03 * L, Rr * 1.05, 0.01 * L), "steel")
    scene.add(lambda p: sd_cyl(frame.local(p) - np.array([-0.3 * L, 0, 0]), 0, 0.035 * L, Rr * 1.04, 0.008 * L), band)
    # fuze tip
    scene.add(lambda p: sd_sphere(frame.local(p) - np.array([0.5 * L - 0.05 * Rr, 0, 0]), 0.18 * Rr), "steel")
    if studs:
        pts = []
        for xf, ang in [(-0.2, 30), (-0.2, 100), (0.0, 65), (0.0, 135), (-0.05, -5), (0.2, 40), (0.2, 105)]:
            a = math.radians(ang)
            pts.append(np.array([xf * L, math.sin(a) * Rr * 0.98, math.cos(a) * Rr * 0.98]))
        pts = np.array(pts)

        def studs_fn(p):
            q = frame.local(p)
            d = np.full(len(p), 1e9)
            for c in pts:
                d = np.minimum(d, sd_sphere(q - c, 0.14 * Rr))
            return d

        scene.add(studs_fn, "gold")


def wrench(scene, frame: Frame, length_, width):
    L, w = length_, width
    th = w * 0.32

    def fn(p):
        q = frame.local(p)
        shaft = sd_box(q, (L * 0.36, w * 0.5, th), th * 0.6)
        d = shaft
        for sx in (1, -1):
            hq = q - np.array([sx * L * 0.42, 0, 0])
            head = sd_cyl(hq, 2, th * 1.05, w * 1.15, th * 0.5)
            # open jaw: a slot from the end towards the shaft
            jaw = sd_box(hq - np.array([sx * w * 0.75, 0, 0]), (w * 0.75, w * 0.42, th * 3))
            head = np.maximum(head, -jaw)
            d = smin(d, head, w * 0.12)
        return d

    scene.add(fn, "chrome")


def barrel(scene, frame: Frame, length_, radius):
    """A cannon barrel along local +x: breech block, tube with brass bands, muzzle with its bore."""
    L, Rr = length_, radius

    def tube(p):
        q = frame.local(p)
        # tapering tube: radius shrinks towards the muzzle
        taper = Rr * (1.0 - 0.12 * np.clip((q[:, 0] + 0.3 * L) / (0.75 * L), 0, 1))
        radial = np.sqrt(q[:, 1] ** 2 + q[:, 2] ** 2) - taper
        along = np.abs(q[:, 0] - 0.05 * L) - 0.45 * L
        d = np.sqrt(np.maximum(radial, 0) ** 2 + np.maximum(along, 0) ** 2) + np.minimum(np.maximum(radial, along), 0)
        d = d * 0.95
        breech = sd_cyl(q - np.array([-0.42 * L, 0, 0]), 0, 0.1 * L, Rr * 1.28, Rr * 0.12)
        knob = sd_sphere(q - np.array([-0.53 * L, 0, 0]), Rr * 0.45)
        muzzle = sd_cyl(q - np.array([0.47 * L, 0, 0]), 0, 0.05 * L, Rr * 1.08, Rr * 0.05)
        d = smin(smin(d, breech, Rr * 0.08), muzzle, Rr * 0.05)
        d = smin(d, knob, Rr * 0.1)
        bore = sd_cyl(q - np.array([0.52 * L, 0, 0]), 0, 0.12 * L, Rr * 0.58)
        return np.maximum(d, -bore)

    scene.add(tube, "steel")

    def bands(p):
        q = frame.local(p)
        d = np.full(len(p), 1e9)
        for xf, rf in ((-0.27, 1.12), (-0.02, 1.08), (0.24, 1.04)):
            d = np.minimum(d, sd_cyl(q - np.array([xf * L, 0, 0]), 0, 0.035 * L, Rr * rf, 0.01 * L))
        return d

    scene.add(bands, "brass")

    def bolts(p):
        q = frame.local(p)
        d = np.full(len(p), 1e9)
        for k in range(8):
            a = 2 * math.pi * k / 8
            c = np.array([-0.42 * L, math.sin(a) * Rr * 1.3, math.cos(a) * Rr * 1.3])
            d = np.minimum(d, sd_sphere(q - c, Rr * 0.13))
        return d

    scene.add(bolts, "brass_dark")


def gear(scene, frame: Frame, radius, teeth, thickness, material="brass"):
    def fn(p):
        q = frame.local(p)
        rxy = np.sqrt(q[:, 0] ** 2 + q[:, 1] ** 2)
        a = np.arctan2(q[:, 1], q[:, 0])
        sector = 2 * math.pi / teeth
        a2 = (a + sector / 2) % sector - sector / 2
        lx, ly = rxy * np.cos(a2), rxy * np.sin(a2)
        tooth = np.sqrt(np.maximum(np.abs(lx - radius * 0.93) - radius * 0.12, 0) ** 2 +
                        np.maximum(np.abs(ly) - radius * 0.13, 0) ** 2) + \
            np.minimum(np.maximum(np.abs(lx - radius * 0.93) - radius * 0.12, np.abs(ly) - radius * 0.13), 0)
        disc = rxy - radius * 0.84
        d2 = np.minimum(disc, tooth)
        d2 = np.maximum(d2, -(rxy - radius * 0.3))                  # hole
        ring = np.abs(rxy - radius * 0.62) - radius * 0.05           # sunk ring on the face
        dz = np.abs(q[:, 2]) - thickness
        d = np.sqrt(np.maximum(d2 + 0.01, 0) ** 2 + np.maximum(dz + 0.01, 0) ** 2) + np.minimum(np.maximum(d2, dz), 0) - 0.01
        groove = np.maximum(ring, -(np.abs(q[:, 2]) - thickness * 0.7))   # sunk ring on both faces
        d = np.maximum(d, -groove)
        return d * 0.9

    scene.add(fn, material)

    def hub(p):
        q = frame.local(p)
        return np.maximum(sd_cyl(q, 2, thickness * 1.25, radius * 0.32, radius * 0.04), -sd_cyl(q, 2, thickness * 2, radius * 0.17))

    scene.add(hub, "brass_dark")


def package(scene, frame: Frame, size):
    s = size

    scene.add(lambda p: sd_box(frame.local(p), (s, s * 0.8, s), s * 0.05), "card")

    def straps(p):
        q = frame.local(p)
        a = sd_box(q, (s * 1.012, s * 0.812, s * 0.13), s * 0.02)
        b = sd_box(q, (s * 0.13, s * 0.812, s * 1.012), s * 0.02)
        return np.minimum(a, b)

    scene.add(straps, "brass")
    # address label on the front face (+z) with lines of "text"
    label = frame.child((-s * 0.52, -s * 0.2, s * 1.0))
    scene.add(lambda p: sd_box(label.local(p), (s * 0.34, s * 0.26, s * 0.012), s * 0.02), "paper")

    def ink(p):
        q = label.local(p)
        d = np.full(len(p), 1e9)
        for k, wf in enumerate((0.8, 0.55, 0.7)):
            d = np.minimum(d, sd_box(q - np.array([-s * 0.3 * (1 - wf), s * (0.12 - 0.11 * k), s * 0.014]),
                                     (s * 0.26 * wf, s * 0.022, s * 0.006), s * 0.004))
        return d

    scene.add(ink, "ink")


def tornado(scene, frame: Frame, height, width):
    """A funnel of swirling cloud rings, widest at the top, swaying."""
    H, W = height, width
    rings = 9

    def ring_fn(lo, hi):
        def fn(p):
            q = frame.local(p)
            d = np.full(len(p), 1e9)
            for k in range(lo, hi):
                t = k / (rings - 1)                       # 0 top .. 1 bottom
                y = H / 2 - t * H
                major = W * (1.0 - 0.86 * t ** 0.9)
                minor = W * (0.2 - 0.1 * t)
                sway = 0.16 * W * math.sin(t * 3.4 + 0.6)
                x, z = q[:, 0] - sway, q[:, 2]
                ang = np.arctan2(z, x)
                # each ring is a little lumpy, like cloud
                lump = minor * 0.18 * np.sin(5 * ang + k * 1.7)
                rr = np.sqrt(x * x + z * z)
                tor = np.sqrt((rr - major) ** 2 + ((q[:, 1] - y) * 1.15) ** 2) - (minor + lump)
                d = smin(d, tor * 0.8, minor * 0.6)
            return d
        return fn

    scene.add(ring_fn(0, 5), "cloud")
    scene.add(ring_fn(5, rings), "cloud_dark")

    def debris(p):
        q = frame.local(p)
        d = np.full(len(p), 1e9)
        for (a, yf, rf, sz) in [(0.3, -0.25, 0.85, 0.08), (2.4, -0.05, 1.05, 0.07), (4.0, 0.15, 1.15, 0.06),
                                (5.3, -0.38, 0.7, 0.06), (1.2, 0.3, 1.2, 0.05), (3.3, -0.42, 0.6, 0.05)]:
            c = np.array([math.cos(a) * W * rf, yf * H, math.sin(a) * W * rf])
            m = rotation(a * 50, a * 30, a * 70)
            d = np.minimum(d, sd_box((q - c) @ m, (W * sz, W * sz * 0.6, W * sz * 0.8), W * 0.01))
        return d

    scene.add(debris, "dirt")


# =============================================================================================
# 2D effects drawn behind or over the 3D subject

def burst_layer(cx, cy, r_out, r_in, points, rot0, colour, size):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    pts = []
    for k in range(points * 2):
        a = rot0 + math.pi * k / points
        rr = r_out * (1 if k % 2 == 0 else r_in / r_out) * (0.85 + 0.15 * math.sin(k * 2.7))
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    d.polygon(pts, fill=colour)
    return layer


def explosion(size, cx, cy, r):
    base = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    glow = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([cx - r * 1.3, cy - r * 1.3, cx + r * 1.3, cy + r * 1.3], fill=(255, 150, 40, 170))
    base = Image.alpha_composite(base, glow.filter(ImageFilter.GaussianBlur(r * 0.35)))
    base = Image.alpha_composite(base, burst_layer(cx, cy, r, r * 0.55, 13, 0.2, (255, 128, 30, 255), size)
                                 .filter(ImageFilter.GaussianBlur(r * 0.02)))
    base = Image.alpha_composite(base, burst_layer(cx, cy, r * 0.78, r * 0.42, 13, 0.45, (255, 196, 64, 255), size)
                                 .filter(ImageFilter.GaussianBlur(r * 0.02)))
    base = Image.alpha_composite(base, burst_layer(cx, cy, r * 0.5, r * 0.3, 11, 0.1, (255, 244, 190, 255), size)
                                 .filter(ImageFilter.GaussianBlur(r * 0.03)))
    core = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    ImageDraw.Draw(core).ellipse([cx - r * 0.28, cy - r * 0.28, cx + r * 0.28, cy + r * 0.28], fill=(255, 255, 245, 255))
    return Image.alpha_composite(base, core.filter(ImageFilter.GaussianBlur(r * 0.08)))


def crosshair(size, cx, cy, r):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    w = max(2, int(size * 0.006))
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=(255, 255, 255, 150), width=w)
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        d.line([(cx + math.cos(a) * r * 0.8, cy + math.sin(a) * r * 0.8),
                (cx + math.cos(a) * r * 1.2, cy + math.sin(a) * r * 1.2)], fill=(255, 255, 255, 190), width=int(w * 1.6))
    glow = layer.filter(ImageFilter.GaussianBlur(size * 0.006))
    return Image.alpha_composite(glow, layer)


def speed_lines(size, pts, colour=(200, 230, 255, 120)):
    layer = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for (x0, y0, x1, y1, w) in pts:
        d.line([(x0, y0), (x1, y1)], fill=colour, width=int(w))
    return layer.filter(ImageFilter.GaussianBlur(size * 0.003))


# =============================================================================================
# icons

def px(v):
    """Badge coordinates (-1..1, y down) -> working pixels."""
    return (v + 1) / 2 * R


def scene_parts():
    s = Scene()
    barrel(s, Frame((0.05, -0.05, 0.0), rotation(rx=20, ry=-25, rz=35)), 3.3, 0.42)
    wrench(s, Frame((0.05, 0.05, 0.75), rotation(rx=0, ry=15, rz=-42)), 2.6, 0.3)
    return s, [], []


def scene_interception():
    s = Scene()
    shell(s, Frame((-0.35, -0.35, 0.0), rotation(rx=35, ry=-20, rz=40)), 2.6, 0.42)
    shell(s, Frame((1.08, 1.02, -0.3), rotation(rx=20, ry=10, rz=-138)), 1.25, 0.19, body="brass", studs=False, band="steel")
    behind = [explosion(R, px(0.3), px(-0.27), R * 0.27)]
    over = [crosshair(R, px(0.0), px(0.0), R * 0.33)]
    return s, behind, over


def scene_logistics():
    s = Scene()
    gear(s, Frame((0.95, 0.95, -0.6), rotation(rx=-25, ry=-30, rz=10)), 0.72, 12, 0.13)
    package(s, Frame((-0.12, -0.12, 0.0), rotation(rx=24, ry=-38, rz=0)), 0.95)
    gear(s, Frame((-1.25, -1.15, 0.9), rotation(rx=-15, ry=30, rz=5)), 0.46, 9, 0.1, "brass")
    return s, [], []


def scene_weather():
    s = Scene()
    tornado(s, Frame((-0.1, 0.18, -0.4), rotation(rx=14, ry=0, rz=-5)), 2.9, 1.05)
    wrench(s, Frame((0.35, -0.75, 0.9), rotation(rx=10, ry=-20, rz=28)), 2.3, 0.27)
    lines = speed_lines(R, [(px(-0.75), px(-0.35), px(-0.45), px(-0.42), R * 0.006),
                            (px(0.48), px(-0.1), px(0.78), px(-0.18), R * 0.006),
                            (px(-0.7), px(0.2), px(-0.48), px(0.16), R * 0.005)])
    return s, [lines], []


SCENES = {
    "cbc-parts-assembly": scene_parts,
    "cbc-interception": scene_interception,
    "create-smart-logistics": scene_logistics,
    "weather-physics-sable-fix": scene_weather,
}


def drop_shadow(subject: Image.Image) -> Image.Image:
    a = subject.getchannel("A")
    sh = Image.new("RGBA", subject.size, (0, 8, 30, 0))
    sh.putalpha(a.point(lambda v: v * 150 // 255))
    sh = sh.filter(ImageFilter.GaussianBlur(R * 0.018))
    out = Image.new("RGBA", subject.size, (0, 0, 0, 0))
    out.paste(sh, (int(R * 0.022), int(R * 0.03)))
    return out


def clip_to_disc(layer: Image.Image) -> Image.Image:
    m = Image.new("L", layer.size, 0)
    rr = 0.835 * R / 2
    c = R / 2
    ImageDraw.Draw(m).ellipse([c - rr, c - rr, c + rr, c + rr], fill=255)
    a = Image.composite(layer.getchannel("A"), Image.new("L", layer.size, 0), m)
    layer = layer.copy()
    layer.putalpha(a)
    return layer


def draw(mod: str) -> Image.Image:
    scene, behind, over = SCENES[mod]()
    img = badge()
    for layer in behind:
        img = Image.alpha_composite(img, clip_to_disc(layer))
    subject = to_image(render(scene, R))
    img = Image.alpha_composite(img, drop_shadow(subject))
    img = Image.alpha_composite(img, subject)
    for layer in over:
        img = Image.alpha_composite(img, clip_to_disc(layer))
    return img.resize((OUT, OUT), Image.LANCZOS)


def main(mods) -> None:
    for mod in mods or SCENES:
        img = draw(mod)
        img.save(os.path.join(HERE, f"{mod}.png"))
        res = os.path.join(ROOT, mod, "src", "main", "resources")
        os.makedirs(res, exist_ok=True)
        img.resize((512, 512), Image.LANCZOS).save(os.path.join(res, "icon.png"))
        print("drawn", mod, flush=True)


if __name__ == "__main__":
    main(sys.argv[1:])
