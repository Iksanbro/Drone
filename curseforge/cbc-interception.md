# CBC Interception — CurseForge page

## Project settings

| Field | Value |
|---|---|
| Name | CBC Interception |
| Summary | Shoot down Create Big Cannons shells, rounds and rockets in flight. |
| License | MIT License |
| Game version / loader | Minecraft 1.21.1, NeoForge |
| Environment | Server (clients do not need it; in singleplayer install it in the game) |
| File | `cbc-interception-1.21.1-1.0-release.jar` |
| File display name | CBC Interception 1.0 Release |
| Release type | Release |

## Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Create Big Cannons | `create-big-cannons` | 646668 |
| Optional | Create Big Cannons: Advanced Technologies | `create-big-cannons-advanced-technologies` | 1180196 |
| Optional | CBC Neo Warfare | `cbc-neo-warfare` | 1552960 |
| Optional | CBC Enchanced Shells [Create Big Cannons] | `cbc-enchanced-shells-create-big-cannons` | 1489732 |

Create (328085) and Ritchie's Projectile Library (1279407) come in with Create Big Cannons. The optional mods are supported, not needed.

## Description

**CBC Interception** lets cannon projectiles hit each other. Shells, autocannon rounds and rockets that meet in flight collide, and the one that is hit goes off just as if it had hit its target:

- HE, shrapnel, smoke, fluid and cluster shells, HE rounds and rockets with a warhead **explode in the air** where they were hit;
- rockets of CBC Enchanced Shells release their warhead and set it off;
- solid shot, AP shells and rounds, bullets and unarmed rockets **break apart**.

The projectile that hits is used up too, and explodes if it is explosive itself.

### What can shoot down a projectile

| By | How |
|---|---|
| A direct hit | both paths during the tick are checked in full, so fast projectiles never pass through each other |
| Fragments | flak, shrapnel, grapeshot and Neo Warfare canister fragments hit projectiles like shells do |
| An explosion nearby | projectiles inside an explosion go off |
| Proximity fuzes | an armed proximity fuze goes off when a projectile comes within its distance (in Create Big Cannons it only reacts to living things and contraptions) |

Projectiles of one volley (twin barrels, bursts of fire) do not hit each other, and a projectile that just left the barrel takes no part for 2 ticks.

### Which projectiles

Every projectile of Create Big Cannons and of every addon built on its projectile class: CBC: Advanced Technologies, CBC Neo Warfare, CBC Enchanced Shells and others. The mod finds how each one goes off by itself, no list needed. Entity type tags:

- `cbcintercept:interceptable` — add an entity of another mod (a projectile or rocket);
- `cbcintercept:not_interceptable` — leave a projectile or fragment type out.

### Config

`config/cbcintercept-server.toml`:

| Option | Default | What it does |
|---|---|---|
| `projectilesCollide` | `true` | the whole mod |
| `extraHitRadius` | `0.5` | blocks added to the sizes of two projectiles when checking a hit (0 = touching only) |
| `fragmentsHitProjectiles` | `true` | fragments shoot down projectiles |
| `explosionsHitProjectiles` | `true` | explosions set off projectiles |
| `proximityFuzesReactToProjectiles` | `true` | proximity fuzes go off near projectiles |

### Tested with

NeoForge 21.1.228, Create Big Cannons 5.11.7, CBC: Advanced Technologies 0.1.4c, CBC Neo Warfare 0.0.6v, CBC Enchanced Shells 1.2.5 and 1.5.1.

Known limits: a mortar stone breaks apart instead of exploding when hit; fragments fly at random, so flak that bursts far from the target can miss.
