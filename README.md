# Fallen Angel Origin (Fabric addon for Origins — MC 1.21.1)

Adds a new race, **Fallen Angel**, to the [Origins](https://modrinth.com/mod/origins) mod.

## What's implemented, and how

Almost the entire race is implemented as **data-driven Origins power JSON**
(`src/main/resources/data/origins/`), because that is the stable, documented
part of the Origins/Apoli API. Only one thing genuinely needs Java code —
picking a random, item-compatible, correctly-leveled enchantment (and the
Apple → Enchanted Golden Apple special case) — since nothing like that exists
as a built-in datapack action. That lives in
`src/main/java/com/fallenangel/action/DivineEnchantAction.java`.

| Requirement | How it's done |
|---|---|
| Shows up on the Origins selection screen | `data/origins/origins/fallen_angel.json` + `data/origins/origin_layers/origin.json` |
| Your icon as the Origin's icon | Your artwork is packaged as a small custom item (`fallenangel:fallen_angel_icon`, no other function) and referenced by the origin's `icon` field, so it renders exactly like a real Minecraft item icon |
| +2 hearts | `powers/extra_hearts.json` (`origins:conditioned_attribute`) |
| Extra attack damage | `powers/divine_strength.json` |
| Bonus damage vs. undead | `powers/undead_slayer.json` (`origins:modify_damage_dealt` + undead entity-group check) |
| Everything disabled in the Nether | every power above has a `condition` that checks `origins:dimension != minecraft:the_nether` |
| Totem-like resurrection, 1 min cooldown, purple/white particles | `powers/resurrection.json` (`origins:prevent_death`) + `powers/resurrection_cooldown.json` (`origins:cooldown`) |
| Active random-enchant ability, 1 min cooldown | `powers/divine_enchant.json` (`origins:active_self`) calling the custom `fallenangel:divine_enchant` action |
| Apple → Enchanted Golden Apple | handled inside `DivineEnchantAction.java` |
| Walk on water, dive by sneaking | `powers/walk_on_water.json` (`origins:walk_on_fluid`) |

## Chosen balance values (you can freely retune these)

- **+2 hearts** (`add_value 4.0` to max health)
- **+2 attack damage** flat on top of whatever weapon you're holding
- **+3 bonus damage** against undead mobs specifically
- **Resurrection**: 1/minute, restores to half a heart, then Regeneration II (45s) + Absorption II (5s)
- **Divine Enchantment**: 1/minute, one random valid enchantment at a random valid level for the held weapon/tool

These are meant to feel strong but not run-breaking — similar in weight to
the strongest *built-in* Origins races (e.g. Merman, Elytrian), not
strictly better than all of them combined.

## ⚠️ Before you compile: 3 things to check

Origins/Apoli update fairly often and I can't run a live Gradle build in this
environment to verify against the exact jar you'll use, so please sanity
check these three spots:

1. **`gradle.properties`** — `origins_version`, `apoli_version`, `calio_version`
   are placeholders. Go to https://modrinth.com/mod/origins/versions and
   https://modrinth.com/mod/apoli/versions, find the newest build tagged for
   **1.21.1 Fabric**, and copy its exact version string into
   `gradle.properties`.
2. **`DivineEnchantAction.java`** — the top of the file has a big comment
   block about this. The *enchant-picking logic* uses plain vanilla/Fabric
   APIs and should be fine as-is. The one call that registers the custom
   action with Apoli (`ApoliRegistries.ENTITY_ACTION` / `ActionFactory`) is
   the part most likely to need a one-line adjustment if Apoli renamed
   something internally between versions — if it doesn't compile, open any
   of Apoli's own built-in action classes on GitHub
   (apace100/apoli-fabric, `src/main/java/.../power/factory/action/`) for
   whatever version you pinned, and match the exact signature.
3. **Gradle Wrapper** — I couldn't download the actual `gradle-wrapper.jar`
   binary in this sandbox (no network access). Either open the project in
   IntelliJ/IDEA with the Gradle + Minecraft Development plugins (it will
   generate the wrapper for you), or run `gradle wrapper --gradle-version 8.8`
   once you have a local Gradle install.

## Building the jar

```bash
./gradlew build
```

The compiled mod will be at `build/libs/fallen-angel-origin-1.0.0.jar`.
Drop that (plus Origins, Apoli, Calio, and Fabric API) into your `mods`
folder.

## Project layout

```
src/main/java/com/fallenangel/
  FallenAngelMod.java          - entrypoint, registers the icon item + custom action
  action/DivineEnchantAction.java - random-enchant / apple->golden-apple logic

src/main/resources/
  fabric.mod.json
  assets/fallenangel/...       - icon texture, item model, lang file
  data/origins/origins/fallen_angel.json
  data/origins/origin_layers/origin.json
  data/origins/powers/*.json   - all the race's stats and abilities
```
