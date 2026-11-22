# Changelog

## 1.5.2

- Heat Ray Gun: Diamond, Netherite, Sharpness, Reinforced, Overforced, Unbreakable and Experienced overlays.
- Gunblade: Emerald and Experienced overlays.
- Circular Saw and Boomerang: Experienced overlay.

## 1.5.1

- Gunblade chamber is now opened with sneak-use via the stock Tinkers GUI, reusing the quiver inventory screen.
- The chamber now accepts arrows in addition to Tinkers throwables (#minecraft:arrows and #tconstruct:throwable); arrows are fired at full bow charge.
- Removed the old sneak-use load/eject hand swap.

## 1.5.0

- Gunblade: added a 1-slot chamber. Hold any throwable in the offhand and sneak-use the gunblade to load it; loaded projectiles are thrown directly on fire, with a slightly longer cooldown. With an empty chamber the gunblade falls back to its own bullets (3 charges, reloaded by melee hits).
- Throwables are detected through `#tconstruct:throwable`, so throwable items from other mods work in the chamber.
- Gunblade upgrade slots 2 -> 3, durability multiplier 50% -> 55%.
- Fixed the gunblade not firing on right-click.
- Circular Saw: 11 modifier overlays (Diamond, Netherite, Sharpness, Emerald, Haste, Luck, Fiery, Necrotic, Reinforced, Overforced, Unbreakable).
- Heat Ray Gun: 4 modifier overlays (Emerald, Fiery, Luck, Necrotic).
- Throwing Knife now shows its material name.

## 1.4.7

New: the Gunblade and the Boomerang now show drawn modifier overlays on the tool model instead of a bare icon in the slot.

**Added**
- 22 hand-drawn modifier overlays, mapped per tool through `modifier_maps` and `tinkering/modifiers/sprites/<tool>.json`.
- Gunblade (10): Diamond, Netherite, Sharpness, Luck, Fiery, Necrotic, Reinforced, Overforced, Unbreakable, Severing.
- Boomerang (12): Diamond, Netherite, Sharpness, Luck, Fiery, Necrotic, Reinforced, Overforced, Unbreakable, Emerald, Cooling, Bane of Sssss.

**Notes**
- Overlays share each tool's UV layout, so they follow the material colours of the parts you build.
- Fiery emits light (luminosity 10), matching Tinkers' own tool sprites.
- Modifiers without an overlay still apply normally, they just draw no sprite.
