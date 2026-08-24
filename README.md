# Unbound Weapons

Unbound Weapons is a lightweight Fabric combat mod designed to make Minecraft combat faster, more exciting, and more progression-based while keeping the core feel of vanilla Minecraft intact.

Instead of adding tons of new weapons or completely replacing vanilla combat, Unbound Weapons enhances existing tools like swords, axes, and tridents and more with new abilities, upgrades, movement options, and combat rewards.

The goal is simple:

**Make combat more fun without totally changing Minecraft.**

---

## Features

### Weapon Leveling

Weapons can be upgraded using **Unbound Tokens**, a custom progression item earned through combat.

Upgraded weapons store their own level, which can be seen in the item tooltip. Higher levels unlock or improve special abilities.

Current upgradeable weapons include:

- Swords
- Axe
- Tridents
- Spears
- Maces
- Bows

More weapon types may be expanded in future updates.

---

### Unbound Tokens

Unbound Tokens are the core progression currency of the mod. They are dropped by hostile mobs and players upon death (at a specific drop rate). The tokens are used to upgrade weapons.

---

### Hostile Mob Token Drops

Only hostile mobs can drop Unbound Tokens.

Passive mobs like sheep, cows, chickens, and villagers do not drop tokens.

Token drops currently use a chance-based system so progression feels earned instead of automatic.

### Token Economy Modes

The world stores one persistent token mode:

- `SMP`: valid player kills transfer one Token Charge and award one spendable Unbound Token.
- `SINGLEPLAYER`: hostile mobs can drop spendable Unbound Tokens; PvP transfers are disabled.
- `HYBRID`: both mob drops and PvP transfers are enabled.

Every player starts with 3 Token Charges in every mode and can hold up to 10. In PvP modes, a valid kill transfers one charge from the victim to the killer. A player with no charges cannot reward another charge, and the same pair of players cannot transfer again for 30 minutes. Players below the starting balance lose 3% attack damage per missing charge, while one charge recovers every 30 minutes until the balance returns to 3.

Operators can change modes with:

```text
/unbound mode smp
/unbound mode singleplayer
/unbound mode hybrid
```

The shorter `/smp`, `/singleplayer`, and `/hybrid` aliases are also available. `/unbound mode` displays the current mode, and `/unbound tokens` displays the player's charge balance.

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

### Skewer Dash

At Mega Level 10, pressing the remappable Dash key with an upgraded spear available in the hotbar activates **Skewer Dash**. The ability automatically selects the spear, instantly begins its fully charged right-click attack, and launches the player forward with maximum vanilla Lunge force even when the spear does not have the Lunge enchantment. All vanilla spear materials are supported.

The first enemy struck takes the spear's normal attack damage and is carried in front of the player until the dash ends. Colliding with a wall while carrying an enemy deals an additional 1–10 damage. Early wall impacts deal more bonus damage, while impacts later in the dash deal less. The exact bonus is reported in chat when it lands.

Skewer Dash has a 5-second cooldown and uses the same keybind as Sword Dash.

### Momentum

At spear Mega Level 5, press the remappable Momentum key (default `Z`) while sprinting to begin charging. Every 7 uninterrupted seconds of sprinting temporarily adds one Lunge level on top of the spear's existing enchantment, including levels beyond Lunge III. Momentum ends after three separate left clicks while the charged spear is held, even if those swings miss. It also ends immediately if sprinting stops, restoring the spear's original Lunge level without changing its other enchantments.

### Meteor Drop

At mace Mega Level 5, press the remappable Dash key while airborne with the mace in your main hand to plunge straight downward. Landing creates a shockwave that damages and knocks back nearby enemies, then bounces the player back into the air. Its damage scales from 4 to 16, its radius scales from 2.5 to 5 blocks, and its rebound scales modestly with drop distance. Meteor Drop prevents fall damage during the plunge and has a 5-second cooldown.

At Mega Level 10, Meteor Drop becomes a two-stage ability. The first press launches the player along their exact three-dimensional look vector, supporting every horizontal, vertical, and diagonal direction. A downward first-stage launch accumulates normal fall distance, so vanilla mace smash attacks can hit or miss during it. The second press launches the player straight down and triggers the normal Meteor Drop impact on landing.

### Gravity Well

At mace Mega Level 10, hold Sneak and press the remappable Dash key to create a Gravity Well centered above you. For 1.5 seconds, enemies within a 10-block radius are pulled inward and lifted into a tight group for a mace smash or Meteor Drop. Teammates are excluded, and Gravity Well has its own 12-second cooldown.

### Recoil Shot

At bow Mega Level 5, press the remappable Dash key while holding a bow to instantly fire a critical arrow without consuming ammunition. The player launches in the exact opposite direction, allowing upward, downward, horizontal, and diagonal movement based on aim. Recoil Shot has a 5-second cooldown.

### Volley

At bow Mega Level 10, hold the remappable Volley key (default `X`) while firing normal bow arrows. Every arrow fired while the key remains held activates the ability, allowing repeated shots without releasing X. With open space above the aimed location, 24 additional arrows form a 5-by-5 square with its center omitted. They spawn at randomized heights between 7 and 15 blocks, launch straight upward at the same movement speed used by the Netherite-spear dash, then fall naturally. When the original shot is aimed nearly straight upward, its center arrow is removed, leaving only the 24-arrow outer grid. Every summoned projectile copies the ammunition type and bow: normal, spectral, and tipped arrows retain their original behavior, potion data, glowing effect, and compatible bow enchantments. A Volley costs 24 arrows total, reduced to 7 when the bow has Infinity. If a block is detected anywhere in the 5-by-5 area up to 15 blocks above the target, that shot becomes a Power Shot and the original arrow travels at four times its normal speed without paying the Volley cost. Releasing the Volley key returns the bow to normal firing.

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
