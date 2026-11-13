# Create: Smart Logistics — CurseForge page

## Project settings

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

## Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Create (6.0.10 or newer 6.0.x) | `create` | 328085 |
| Optional | Just Enough Items (JEI) | `jei` | 238222 |
| Optional | EMI | `emi` | 580555 |

## Description

**Create: Smart Logistics** is an addon for **Create 6.0.10** with better versions of Create's logistics blocks and fixes for problems of its package network.

### New blocks

All three are brass versions of Create's blocks. Recipe (shapeless): the Create block + a precision mechanism + an electron tube.

**Smart Frogport**
- up to **16 addresses** in an "Addresses" tab next to its menu (`*` works as in Create); chain conveyor routing knows all of them;
- modes: send and receive, send only, receive only;
- catches and sends **5 packages at a time** (configurable).

**Smart Packager**
- box address set in its own menu (sneak + right click with an empty hand), no sign needed;
- auto-packing: packs everything from the inventory next to it, as if it were always powered;
- makes and unpacks **5 boxes at a time**.

**Smart Stock Ticker**
- an "Addresses" tab in the order menu of the stock keeper;
- an address for mechanical crafting orders, and rows "recipe type → address" (crushing, milling, any recipe type) for recipes ordered from JEI or EMI.

### Big factory gauge grid

A new button in the factory gauge's recipe menu opens a grid editor for mechanical crafters: **3×3, 5×5, 10×10, 25×25, 50×50, 100×100**. Paint with the gauge's inputs, fill from a crafting or mechanical crafting recipe, scroll large grids. Each cell is the mechanical crafter in the same place, seen from the front.

### Fixes

- **Chain conveyor jams:** two full chain conveyors sending packages to each other no longer wait for each other forever and stop the whole network.
- **"?" on factory gauges:** a part of the network in an unloaded chunk no longer turns every gauge of the network to "?"; gauges count what is loaded and keep working.
- **Recipes with more than 9 kinds of ingredients** (large mechanical crafting recipes) can be ordered from the stock keeper, JEI and EMI; Create cut them to 9 and they could not be ordered.
- **Crafting through mechanical crafters:** ordered crafts are put into the right crafters, completely, also for recipes larger than 3×3. Create's re-packager dropped every ingredient past the 9th cell, put items into the wrong crafters for other grid sizes, and started crafting before the grid was full.

### Compatibility

JEI through Create's own support (recipes up to 9 ingredients), EMI through this mod's handler (with or without JEI). REI only through its JEI compatibility layer.

### Config

`config/smartlogistics-server.toml`:

| Option | Default | What it does |
|---|---|---|
| `fixes.chainConveyorJamFix` | `true` | chain conveyor jam fix |
| `fixes.chainConveyorOverflowFactor` | `3.0` | how many times its capacity a conveyor accepts from neighbouring conveyors |
| `fixes.gaugesIgnoreUnloadedLinks` | `true` | gauges do not wait for unloaded parts of the network |
| `fixes.mechanicalCrafterFix` | `true` | mechanical crafter fix |
| `smart_blocks.smartFrogportBatch` | `5` | packages at a time for the smart frogport |
| `smart_blocks.smartPackagerBatch` | `5` | boxes at a time for the smart packager |

### Tested with

NeoForge 21.1.228, Create 6.0.10, JEI 19.21.0.247, EMI 1.1.24.

No textures of Create are included: the brass look is made in the game from your installed Create's textures.
