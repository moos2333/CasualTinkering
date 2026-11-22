# Casual Tinkering

A Tinkers' Construct 3 addon for **Minecraft 1.20.1 (Forge)** that adds spinning saw blades, homing magic swords, thrown weapons, thermal ray guns and ancient gunblades — all built from Tinkers' materials, modifiers and tool definitions rather than standalone gear.

Both source projects — the 1.12.2 *Casual Tinkering* addon and *Tinkers Gadgets* — are rebuilt and merged here on the Tinkers' Construct 3 modifier hook API and its data-driven tool system.

## Content

| Tool | Description |
| --- | --- |
| **Circular Saw** | Broad melee/mining hybrid. Hold right-click to saw everything in front of you at high frequency; comes with Padded II, and Sweeping Edge widens the sawing cone. |
| **Heat Ray Gun** | Fluid-fuelled beam weapon. Charge, then fire a long-range ray whose damage scales with the fuel's temperature; overheats after sustained fire. |
| **Boomerang** | Thrown weapon that returns to your hand, inheriting your tool's traits on the way out and back. |
| **Magic Sword** | Four modifiers (base / control / auto / coop) that summon homing blades around your target. |
| **Throwing Knife** | Data-driven shuriken with a built-in triple shot when sneaking. |
| **Gunblade** | Ancient greatsword fused with a gun barrel. Hits a little harder than a greatsword and fires high-velocity rounds that carry your tool's on-hit modifiers; the firing cooldown shortens with attack speed. Found in Ancient Cities. |

Every tool uses the usual Tinkers' material system, so stats, traits and visuals follow the parts you build them from. Full documentation lives in the in-game **Encyclopedia**.

### Modifiers

- `circular_saw_attack` — sawing (Circular Saw)
- `magic_sword`, `magic_sword_control`, `magic_sword_auto`, `magic_sword_coop` — magic swords
- `thermal_ray` — heat ray beam (Heat Ray Gun)
- `boomerang` — returning throw (Boomerang)
- `triple_shot` — slotless triple shot (Throwing Knife)
- `gunblade` — gunshot (Gunblade)

## Requirements

| Dependency | Version |
| --- | --- |
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Tinkers' Construct | 3.12.1 or newer |
| Mantle | 1.11.113 or newer |
| Json Things | 0.9.9 or newer |
| Better Combat | optional |

**Better Combat** support is pure data: the addon ships `weapon_attributes` files for its tools and does not depend on Better Combat in any way. Without it, those files are simply never read.

## Building

Requires JDK 17:

```bash
./gradlew build
```

The output jar is written to `build/libs/`.

Dependencies are resolved from CurseMaven (Tinkers' Construct, Mantle, Json Things); JEI is a runtime-only dependency for development.

## License

MIT
