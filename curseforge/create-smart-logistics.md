# Create: Smart Logistics

**Smarter logistics for Create 6.** Brass versions of the frogport, packager and stock ticker that do more at once, a **100×100 recipe grid** for mechanical crafters, and fixes for jams and stalls of Create's package network.

---

## ✨ Features

### 🐸 Smart Frogport
- Up to **16 addresses** in one frogport, in an "Addresses" tab; `*` patterns work as in Create, chain conveyor routing knows all of them.
- Modes: **send and receive**, **send only**, **receive only**.
- Catches and sends **5 packages at a time**.

### 📦 Smart Packager
- **Box address** right in its menu — no sign needed.
- **Auto-packing**: packs everything from the inventory next to it without a redstone signal.
- Makes and unpacks **5 boxes at a time**.

### 📋 Smart Stock Ticker
- An "Addresses" tab in the stock keeper's order menu.
- **Mechanical crafting** address: ingredients of crafting orders go straight to your crafters.
- Add rows like *Crushing → Crusher room*, *Milling → Mill* — ingredients of recipes ordered from **JEI** or **EMI** go to the right place.

### 🧩 Huge mechanical crafter recipes
- A grid editor in the **factory gauge**: **3×3, 5×5, 10×10, 25×25, 50×50, 100×100**.
- Paint the recipe with the gauge's inputs, fill it from a crafting recipe, scroll large grids.
- The gauge orders the whole recipe, and every ingredient lands in the crafter in the same place.

### 🩹 Fixes for Create's logistics
- **Chain conveyors no longer jam** when two full conveyors send packages to each other.
- Factory gauges no longer show **"?"** and stop when part of the network is in an unloaded chunk.
- Recipes with **more than 9 kinds of ingredients** can be ordered from the stock keeper, JEI and EMI.
- Mechanical crafters get **every ingredient in the right place**, also for recipes bigger than 3×3 — Create used to lose everything past the 9th cell.

---

## 🚀 Quick start
1. Craft a smart block: the normal Create block + a **precision mechanism** + an **electron tube** (shapeless).
2. **Smart Frogport:** place it on a chain conveyor like a normal one, open the "Addresses" tab and add addresses with **+**.
3. **Smart Packager:** sneak + right click with an empty hand to set the box address and auto-packing.
4. **Smart Stock Ticker:** in the stock keeper's order menu open the "Addresses" tab and set where crafting ingredients go.
5. **Big grid:** in a factory gauge's recipe menu press the grid button next to the trash can.

---

## 📦 Requirements
- Minecraft **1.21.1**, NeoForge **21.1.228+**
- **Create** 6.0.10+

## 🔌 Optional
- **JEI** or **EMI** — order recipes straight from the recipe viewer

## ⚙️ Config
`config/smartlogistics-server.toml`: each fix can be turned off; chain conveyor overflow; frogport and packager batch sizes.

## ⚠️ Known issues
- **REI** is supported only through its JEI compatibility layer.
- As in Create, several crafts in one order need a **re-packager** in front of the crafters' packager.

---

## 🌐 Languages
English, Russian.

## 📄 License
MIT. No textures of Create are bundled: the brass look is made in the game from your installed Create.

---

*This mod was made 100% by AI.*
