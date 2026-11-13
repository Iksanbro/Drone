# CBC Parts Assembly — CurseForge page

## Project settings

| Field | Value |
|---|---|
| Name | CBC Parts Assembly |
| Summary | Assemble cannon parts in survival, craft them by sequenced assembly, and fire 60 new medium cannon rounds. |
| License | MIT License |
| Game version / loader | Minecraft 1.21.1, NeoForge |
| Environment | Client and server (install on both) |
| File | `cbc-parts-assembly-1.21.1-1.0-release.jar` |
| File display name | CBC Parts Assembly 1.0 Release |
| Release type | Release |

## Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Create | `create` | 328085 |
| Required | Create Big Cannons | `create-big-cannons` | 646668 |
| Optional | Create Big Cannons: Advanced Technologies | `create-big-cannons-advanced-technologies` | 1180196 |
| Optional | CBC Neo Warfare | `cbc-neo-warfare` | 1552960 |
| Optional | CBCMW recipe fix | `cbcmw-recipe-fix` | 1598498 |
| Optional | CBC Enchanced Shells [Create Big Cannons] | `cbc-enchanced-shells-create-big-cannons` | 1489732 |

Ritchie's Projectile Library (`ritchies-projectile-library`, 1279407) is needed by Create Big Cannons itself; it does not have to be listed here.

## Description

**Build the smaller cannons of Create Big Cannons in survival — and give the medium cannon a full arsenal.**

### Craft cannon parts with Create
Autocannon, twin and heavy autocannon, rocket pod, medium cannon and rotary cannon parts can now be made on a **sequenced assembly** line with deployers, presses, saws and spouts, not only by casting and boring. It costs more metal than casting, so casting stays the cheap way.

### Assemble cannons in survival
Parts you place in survival now connect to each other, just like in creative. Put a cannon mount down, place the breech, the recoil part and the barrels in a line, give the mount a redstone signal — and the cannon is ready. Parts placed by deployers connect too.

### Pick parts up with a wrench
**Sneak + right click** a cannon part with a wrench to take it back whole, instead of breaking it into scrap.

### Addons fixed
- Blocks of CBC: Advanced Technologies and CBC Neo Warfare drop again when you break them.
- Missing mould recipes of CBC: Advanced Technologies work.
- Rotary cannons of CBC Neo Warfare can be cast and bored.

### Rockets keep flying
Rockets of CBC Enchanced Shells no longer freeze in the air or vanish when they fly away from players.

### 60 new medium cannon rounds
With **CBC Neo Warfare**, the medium cannon gets rounds for every big shell it was missing:

- from Create Big Cannons: **shrapnel, fluid, solid shot, drop mortar shell, mortar stone**;
- from CBC Enchanced Shells, **Mk.I to Mk.V**: HE, AP, AP shot, solid shot, shrapnel, smoke, drop mortar, grapeshot, mortar stone, **cluster** and **bunker buster**.

Each round is **half as strong** as its big shell and flies like the other medium rounds.

- Craft **1 big shell → 2 rounds**, then put a round on a filled medium cartridge (crafting table or deployer).
- Add fuzes the same way as for Neo Warfare's rounds.
- Fill fluid rounds with a **spout**.
- Cluster rounds split into small HE rounds at the top of their flight; bunker busters drill into blocks before they explode.
- All rounds are in the **Medium Cannon Shells** creative tab.

### Good to know
- Install on the server **and** on every client.
- Survival assembly, wrench pickup and the rocket fix can be turned off in `config/cbcparts-server.toml`.
- Neo Warfare's medium cannons themselves are made with **CBCMW recipe fix**.

---

*This mod was made 100% by AI.*
