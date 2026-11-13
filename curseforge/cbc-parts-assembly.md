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

**CBC Parts Assembly** makes the smaller cannons of Create Big Cannons and its addons work in survival on NeoForge 1.21.1, and adds medium cannon rounds for the big shells that CBC Neo Warfare has no medium version of.

### Sequenced assembly recipes for cannon parts

Autocannon, twin and heavy autocannon, rocket pod, medium cannon and rotary cannon parts can normally only be cast in a cannon cast and bored with a cannon drill. This mod adds a Create **sequenced assembly** recipe for every one of them (over 90 parts):

- the recipe starts from an ingot of the part's material and costs **3× the metal of casting** (ingots plus molten metal from a spout);
- it uses the deployer, the mechanical press and the mechanical saw;
- parts with an extractor, spring or shaft get it in a second deployer step;
- casting and boring keep working: this is a second, more expensive way, not a replacement.

Covered: autocannons (Create Big Cannons), twin, vertical twin and heavy autocannons and rocket pods (CBC: Advanced Technologies), medium and rotary cannons (CBC Neo Warfare). Big cannons are not touched.

### Survival assembly

Parts placed in survival now connect to each other the way they do in creative, so the cannon can be put on a mount and assembled with redstone. Without the mod assembly fails with *"Cannon must have exactly one opening and exactly one closed end"*. Works for autocannons, twin and heavy autocannons, rocket pods, medium and rotary cannons, also for parts placed by deployers. Material properties are not changed.

### Wrench pickup

**Sneak + right click with a wrench** (Create's wrench or any item tagged `c:tools/wrench`) takes a cannon part back into the inventory whole, instead of breaking it into scrap.

### Fixes for the addons on 1.21.1

- Blocks of CBC: Advanced Technologies and CBC Neo Warfare drop again when broken: their loot tables and tags were in the 1.20 folders, which 1.21 does not read.
- Mould recipes of CBC: Advanced Technologies that 1.21.1 cannot read are added in a working form.
- Rotary cannons of CBC Neo Warfare get casting, boring, moulds and a breech extractor recipe.

### Rockets in unloaded chunks

Rockets of **CBC Enchanced Shells** keep flying when they leave the area around players: the chunks on their path are loaded for a moment as they pass, instead of the rocket freezing in mid-air or vanishing with its chunk.

### Medium cannon rounds (with CBC Neo Warfare)

60 new rounds and cartridges for the medium cannon, made after the big shells that have no medium version:

- **Create Big Cannons:** shrapnel shell, fluid shell, solid shot, drop mortar shell, mortar stone;
- **CBC Enchanced Shells, Mk.I–V each:** HE, AP, AP shot, solid shot, shrapnel, smoke, drop mortar, bag of grapeshot, mortar stone, cluster, bunker buster.

Each round is **half as strong** as its big shell: damage, explosion, penetration, toughness, mass, knockback, fragment count, smoke, fluid amount, cluster sub-rounds, drilling depth. They fly like Neo Warfare's own medium rounds.

- One big shell in a crafting table gives **2 rounds**; put a round on a filled medium cartridge (crafting table or deployer) to get a cartridge.
- Fuzes are added with Neo Warfare's recipes.
- The fluid round is filled with a spout (up to 1000 mB).
- The cluster round splits into medium HE rounds at the top of its flight; the bunker buster drills through blocks, then explodes.
- All rounds are in their own creative tab. Values can be changed by datapack (`data/cbcparts/munition_properties/projectiles/`).

The nuclear, hydrogen, gravitational, singularity, plasma, disintegration and vortex shells and the shell holder are left out on purpose.

### Config

`config/cbcparts-server.toml`:

| Option | Default | What it does |
|---|---|---|
| `connectPartsInSurvival` | `true` | connect parts placed in survival |
| `connectPartsPlacedByMachines` | `true` | also connect parts placed by deployers and fake players |
| `wrenchPickupParts` | `true` | wrench pickup of cannon parts |
| `rocketsLoadChunks` | `true` | Enchanced Shells rockets fly through unloaded chunks |

### Tested with

NeoForge 21.1.228, Create 6.0.10, Create Big Cannons 5.11.7, CBC: Advanced Technologies 0.1.4c, CBC Neo Warfare 0.0.6v, CBCMW recipe fix 0.0.3a, CBC Enchanced Shells 1.5.1 (rockets also 1.2.5).

The medium cannons of Neo Warfare get their own casting recipes from **CBCMW recipe fix**; install it to make them.
