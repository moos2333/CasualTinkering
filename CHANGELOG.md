# Changelog

## 1.6.3

- Quartziron is now classified as a heavy ranged material, so its ranged traits and encyclopedia entry work as intended.

## 1.6.2

- Fixed thrown boomerangs losing their damage and knockback when the game is saved and reloaded.

## 1.6.1

- Quartziron now supports armour plating and chainmail, with the new Concealment trait: incoming damage is reduced by 0.5% per block of distance from the attacker, up to 5% per level.
- Fixed the Sniping trait dealing no distance bonus damage in melee while knocking targets back much harder than intended.
- The Molten Verdant Steel page in the encyclopedia now has its own description and lists its effects in the same wording as the other slime alloys.

## 1.6.0

- New tier 3 material: Quartziron. Cast molten quartz over iron parts, or molten iron over quartz. Its Sniping trait adds damage with hit distance.
- New tier 3 material: Verdant Steel. Alloy molten copper with earth slime and molten clay. Its Oversiphon trait turns picked up experience into overslime.
- Both materials come with their own ingots, and Molten Verdant Steel can be poured: it clings like slime and sets off small explosions that break no blocks and set nothing on fire.

## 1.5.9

- Heat Ray Gun fuel tiers: lava and blazing blood deal full damage, other hot fluids deal half damage and apply their fluid effects, fluids below 400K cannot fire.
- Circular Saw rebalanced as a faster, lighter heavy tool.
- Removing the Gunblade modifier now refunds its ability slots.

## 1.5.8

- Circular Saw no longer flickers between sawing and idle in third person.
- Circular Saw reach in Better Combat raised to 3.

## 1.5.7

- Throwing Knife projectile damage and velocity raised to 1.25.
- The Magic Sword Coop modifier no longer triggers from projectile hits.
- Circular Saw sweep ignores taps shorter than one sweep interval.
- Triple Shot can now be salvaged.
- Boomerang charge time now scales continuously with attack speed.

## 1.5.6

- Broken textures for the Circular Saw, Heat Ray Gun and Boomerang.
- Refined Throwing Knife material textures.

## 1.5.5

- The Gunblade barrel trait can now be upgraded by the Rebalanced modifier, matching other ancient tools.

## 1.5.4

- Added optional Epic Fight compatibility through datapack files; the Gunblade, Circular Saw, Heat Ray Gun and Boomerang use sword, axe, one-handed tool and dagger templates respectively, with the Boomerang keeping sword-length reach.
- Corrected and standardized the encyclopedia entries for all tools.

## 1.5.3

- Fixed an issue that prevented the Gunblade from generating in ancient city chests.

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
