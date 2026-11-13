# CBC Interception

**Shoot down shells in flight.** Shells, autocannon rounds and rockets of Create Big Cannons and its addons can now **hit each other in the air** — build flak batteries, anti-rocket autocannons and point defence for your base and your airships.

---

## ✨ Features

### 💥 Projectiles collide
- Two projectiles that meet in flight **hit each other** — even very fast ones: their whole paths during the tick are checked, so they never slip past.
- A hit projectile **goes off right there**:
  - HE, shrapnel, smoke, fluid and cluster shells, HE rounds and armed rockets **explode in mid-air**;
  - rockets of CBC Enchanced Shells release and detonate their warhead;
  - solid shot, AP shells and rounds, bullets and unarmed rockets **break apart**.
- The projectile that hits is used up too — and explodes if it is explosive itself.

### 🛡️ Ways to stop a shell
- **Direct hit** — autocannon fire against incoming shells.
- **Fragments** — flak, shrapnel, grapeshot and Neo Warfare canister fragments shred incoming projectiles.
- **Explosions** — a blast next to a flying shell sets it off.
- **Proximity fuzes** — now go off when a projectile comes close, not only near mobs and contraptions.

### 🎯 Smart about friendly fire
- Shells of **one volley** (twin barrels, bursts of fire) never hit each other.
- A shell that just left the barrel is not hit by the one fired right before it.

### 🧩 Works with every CBC projectile
- All projectiles of **Create Big Cannons**, **CBC: Advanced Technologies**, **CBC Neo Warfare**, **CBC Enchanced Shells** and other addons built on CBC — found automatically, no list needed.
- Tags for modpack makers: `cbcintercept:interceptable` to add a projectile of another mod, `cbcintercept:not_interceptable` to leave one out.

---

## 🚀 Quick start
1. Build an autocannon or a flak gun.
2. Fire at an incoming shell or rocket — or put a **proximity fuze** on flak and fire it into the shell's path.
3. Watch it burst in the air.

Works great together with **Create: Radar Fire Control** for automatic shell and rocket interception.

---

## 📦 Requirements
- Minecraft **1.21.1**, NeoForge **21.1.228+**
- **Create Big Cannons** 5.11.4+

## 🔌 Optional
- **CBC: Advanced Technologies**, **CBC Neo Warfare**, **CBC Enchanced Shells** — their projectiles can be shot down too

Server-side: clients do not need the mod.

## ⚙️ Config
`config/cbcintercept-server.toml`:
- `projectilesCollide` — the whole mod
- `extraHitRadius` — how close two projectiles must pass to hit (default 0.5 blocks)
- `fragmentsHitProjectiles`, `explosionsHitProjectiles`, `proximityFuzesReactToProjectiles`

## ⚠️ Known issues
- A **mortar stone** breaks apart instead of exploding when shot down.
- Fragments fly at random: flak that bursts far from the target can miss.
- If an addon's projectile cannot explode (for example an addon built for another CBC version), it breaks apart instead.

---

## 📄 License
MIT. No other mods are bundled inside the jar.

---

*This mod was made 100% by AI.*
