# Unbound Weapons

Unbound Weapons is a lightweight Fabric combat mod designed to make Minecraft combat faster, more exciting, and more progression-based while keeping the core feel of vanilla Minecraft intact.

Instead of adding tons of new weapons or completely replacing vanilla combat, Unbound Weapons enhances existing tools like swords, axes, and tridents with new abilities, upgrades, movement options, and combat rewards.

The goal is simple:

**Make combat more fun without turning Minecraft into a totally different game.**

---

## Features

### Weapon Leveling

Weapons can be upgraded using **Unbound Tokens**, a custom progression item earned through combat.

Upgraded weapons store their own level, which can be seen in the item tooltip. Higher levels unlock or improve special abilities.

Current upgradeable weapons include:

- Swords
- Tridents

More weapon types may be expanded in future updates.

---

### Unbound Tokens

Unbound Tokens are the core progression currency of the mod.

They are:

- Custom items
- Separate from vanilla gold nuggets
- Glowing with an enchantment-style glint
- Earned from hostile mobs
- Used to upgrade weapons
- Displayed with custom name and lore

Tokens are no longer tied to gold, meaning players must actually fight mobs to progress instead of mining or farming gold.

---

### Hostile Mob Token Drops

Only hostile mobs can drop Unbound Tokens.

Passive mobs like sheep, cows, chickens, and villagers do not drop tokens.

Token drops currently use a chance-based system so progression feels earned instead of automatic.

---

### Scaling Upgrade Costs

Weapon upgrade costs increase as weapons become stronger.

Instead of every level costing the same amount, higher-level weapons require more Unbound Tokens, making late-game progression more meaningful.

There is currently no max weapon level set

---

### Dash Ability

Players can dash while sprinting with a sword equipped.

The dash is bound to a custom keybind and can be changed in Minecraft’s Controls menu.

Default key:

| Action | Key |
|---|---|
| Dash | R |

The dash adds fast movement to combat without replacing Minecraft’s normal movement system.

---

### Sword Combo System

Swords build combo progress when landing hits.

After 5 consecutive Sword hits, the **Black Flash** ability is charged and can be armed with a custom keybind.

Default Key:

| Action | Key |
|---|---|
| Black Flash Arm | V |

Once armed, the next sword hit triggers the ability

---

### Axe Abilities

Axes are designed as heavy precision weapons focused on disruption and high-impact attacks.

#### Armor Paralysis

At the required upgrade level, sneak-attacking with an axe triggers **Armor Paralysis**.

Armor Paralysis temporarily slows/paralyzes the target, giving axes a tactical crowd-control role in combat.

#### Guard Break

Higher-level axes unlock **Guard Break**, a charged heavy-strike ability.

To charge Guard Break, the player must land **3 critical axe hits in a row**. If the player lands a normal non-critical hit before completing the chain, the Guard Break chain resets.

Once charged, right click with the axe to arm Guard Break. The next axe hit within the time limit releases a powerful impact that:

- Deals bonus damage
- Applies Weakness
- Applies Slowness
- Creates heavy impact particles and sound effects

---

### Charged Tridents

Level 10 tridents unlock a powerful charged throw ability.

When holding right click with a high-level trident, players can charge up a high-speed throw.

The longer the trident is charged, the faster it launches.

Current charged trident features:

- Level 10 unlock requirement
- Live charge progress display
- Damage preview above the hotbar
- High-speed projectile launch
- Sonic Boom effects
- Velocity-based bonus damage

This turns high-level tridents into a powerful long-range combat option.

---

## Design Philosophy

Unbound Weapons is built around minimal but meaningful combat changes.

The mod aims to:

- Preserve vanilla Minecraft’s identity
- Improve existing weapons instead of replacing them
- Reward active combat
- Add progression without excessive complexity
- Keep abilities simple, readable, and satisfying
- Make each weapon type feel more unique

Unbound Weapons is not meant to be a full RPG overhaul. It is meant to feel like Minecraft combat with more energy, progression, and impact.

---

## Requirements

- Minecraft 1.21.x
- Fabric Loader
- Fabric API

---

## Installation

1. Install Fabric Loader.
2. Install Fabric API.
3. Download the Unbound Weapons `.jar` file.
4. Place the `.jar` file into your Minecraft `mods` folder.
5. Launch Minecraft using the Fabric profile.

---

## Development Setup

Clone the repository:

```bash
git clone <repo-url>
