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

A small fix for a server crash of **Weather Physics** together with **Sable**:

```
java.lang.RuntimeException: Body has been removed
    at dev.ryanhcode.sable...RapierPhysicsPipeline.assertBodyValid
    at dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle.getLinearVelocity
    at net.killey.tornadophysics.event.TornadoEvent.processPhysics
```

### Cause

`TornadoEvent.processPhysics` asks a Sable sub-level's physics body for its velocity without checking that the body still exists. When the sub-level is removed in the same tick (it broke apart, merged or was destroyed), Sable throws and the server crashes. Weather Physics already does this check in its `WindPhysics` class, but not here.

### Fix

A mixin adds the same check (`RigidBodyHandle.isValid()`) at the start of `processPhysics`: a removed body is skipped for that tick. Nothing else changes.

### Tested with

NeoForge 21.1.228, Weather Physics 1.0.3, Sable 2.0.6, Weather, Storms & Tornadoes. A game test creates and removes a Sable sub-level under a real Weather2 storm: without the fix it crashes with the same error, with it it does not.
