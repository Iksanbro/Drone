# Weather Physics Sable Fix — CurseForge page

## Project settings

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

## Dependencies

| Relation | Project | Slug | Project ID |
|---|---|---|---|
| Required | Weather Physics (Wind, Sails & Tornadoes) | `tornado-physics` | 1541520 |
| Required | Sable | `sable` | 1312371 |

Weather Physics brings in Weather, Storms & Tornadoes (237746) and CoroUtil itself. In the mod file both are optional, so the game still starts without them; the fix then simply does nothing.

## Description

**Fixes the server crash "Body has been removed" when you play Weather Physics together with Sable.**

When a Sable structure broke apart, merged or was destroyed while a tornado was pulling at it, the whole server could crash. With this mod the tornado simply skips that structure, and the game goes on. Nothing else about tornadoes, wind or Sable changes.

### Good to know
- Only the **server** needs it; in singleplayer just put it in your mods folder.
- Needs **Weather Physics** and **Sable**; without them it does nothing.
- Safe to add to an existing world and safe to remove later.

---

*This mod was made 100% by AI.*
