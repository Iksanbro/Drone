# Project settings and dependencies

Not part of the descriptions; for filling in the CurseForge project and file forms.

## CBC Parts Assembly
### Project settings

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

### Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Create | `create` | 328085 |
| Required | Create Big Cannons | `create-big-cannons` | 646668 |
| Optional | Create Big Cannons: Advanced Technologies | `create-big-cannons-advanced-technologies` | 1180196 |
| Optional | CBC Neo Warfare | `cbc-neo-warfare` | 1552960 |
| Optional | CBCMW recipe fix | `cbcmw-recipe-fix` | 1598498 |
| Optional | CBC Enchanced Shells [Create Big Cannons] | `cbc-enchanced-shells-create-big-cannons` | 1489732 |

Ritchie's Projectile Library (`ritchies-projectile-library`, 1279407) is needed by Create Big Cannons itself; it does not have to be listed here.

## CBC Interception
### Project settings

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

### Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Create Big Cannons | `create-big-cannons` | 646668 |
| Optional | Create Big Cannons: Advanced Technologies | `create-big-cannons-advanced-technologies` | 1180196 |
| Optional | CBC Neo Warfare | `cbc-neo-warfare` | 1552960 |
| Optional | CBC Enchanced Shells [Create Big Cannons] | `cbc-enchanced-shells-create-big-cannons` | 1489732 |

Create (328085) and Ritchie's Projectile Library (1279407) come in with Create Big Cannons. The optional mods are supported, not needed.

## Create: Smart Logistics
### Project settings

| Field | Value |
|---|---|
| Name | Create: Smart Logistics |
| Summary | Smarter frogports, packagers and stock tickers, big factory gauge grids and fixes for Create's logistics. |
| License | MIT License |
| Game version / loader | Minecraft 1.21.1, NeoForge |
| Environment | Client and server (install on both) |
| File | `create-smart-logistics-1.21.1-1.0-release.jar` |
| File display name | Create: Smart Logistics 1.0 Release |
| Release type | Release |

### Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Create (6.0.10 or newer 6.0.x) | `create` | 328085 |
| Optional | Just Enough Items (JEI) | `jei` | 238222 |
| Optional | EMI | `emi` | 580555 |

## Weather Physics Sable Fix
### Project settings

| Field | Value |
|---|---|
| Name | Weather Physics Sable Fix |
| Summary | Fixes the "Body has been removed" server crash of Weather Physics with Sable. |
| License | MIT License |
| Game version / loader | Minecraft 1.21.1, NeoForge |
| Environment | Server (in singleplayer install it in the game) |
| File | `weather-physics-sable-fix-1.21.1-1.0-release.jar` |
| File display name | Weather Physics Sable Fix 1.0 Release |
| Release type | Release |

### Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Weather Physics (Wind, Sails & Tornadoes) | `tornado-physics` | 1541520 |
| Required | Sable | `sable` | 1312371 |

Weather Physics brings in Weather, Storms & Tornadoes (237746) and CoroUtil itself. In the mod file both are optional, so the game still starts without them; the fix then simply does nothing.

