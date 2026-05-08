# 🃏 Custom Card Game (Java + Maven)

## 📌 Project Overview

This project is a **from-scratch implementation of a custom card game** using Java.
The goal is not only to build a working game but to **deeply understand system design, object-oriented programming, and scalable architecture**.

The game started as a simple card-dealing program and is being gradually expanded into a **rule-based turn game** with advanced mechanics.

---

## 🏗️ Project Structure

```
src/
 ├── main/
 │   ├── java/
 │   │   ├── game/
 │   │   │   ├── Card.java
 │   │   │   ├── Deck.java
 │   │   │   ├── Player.java
 │   │   │   ├── Game.java
 │   │   │   ├── Suit.java
 │   │   │   └── Rank.java
 │   │   │
 │   │   └── myapp/
 │   │       └── Main.java
```

---

## 🧠 Core Design Philosophy

### 1. Separation of Responsibilities

Each class has a clear role:

| Class           | Responsibility                            |
| --------------- | ----------------------------------------- |
| `Card`          | Represents a single card                  |
| `Deck`          | Manages all cards (create, shuffle, deal) |
| `Player`        | Stores player state and hand              |
| `Game`          | Controls game flow and rules              |
| `Suit` / `Rank` | Define valid card values                  |

---

### 2. Use of `enum` (Important Design Decision)

Instead of using `String`, the project uses:

* `Suit` (HEARTS, SPADES, etc.)
* `Rank` (A, TWO, THREE, ..., K)

#### Why?

* Prevent invalid values
* Improve readability
* Enable easier rule implementation
* Avoid bugs caused by typos

---

## 🧩 Current Game Logic (Implemented)

### 🎯 Game Setup

* Multiple players can join
* Each player receives **6 cards**
* First player receives **7 cards**
* First player starts the game by dropping one card

---

### 🔄 Turn-Based System

* Game runs in a loop
* Each player takes a turn
* A player can:

    * Play a valid card
    * Or draw a card if no valid move

---

### 📜 Valid Move Rule

A card can be played if:

> It matches the **suit OR rank** of the current top card

---

### 🔁 Player Rotation Logic

The game supports:

* Forward direction
* Reverse direction
* Circular movement (no index errors)

---

### 🟡 Special Rules (Implemented)

#### 🔸 Card: 5

* Next player is **skipped**

#### 🔸 Card: 7

* Game direction is **reversed**

---

## ⚙️ Game Flow

1. Deck is created and shuffled
2. Players are added
3. Cards are distributed
4. First player starts by playing a card
5. Game enters loop:

    * Check valid move
    * Apply rules
    * Move to next player
6. Game ends when a player has **0 cards**

---

## 🧪 Current Features

✅ Deck creation using enums
✅ Card shuffling
✅ Player hand management
✅ Turn-based gameplay
✅ Valid move checking
✅ Skip rule (5)
✅ Reverse rule (7)
✅ Circular player movement

---

## 🚧 Features Not Yet Implemented

* Multi-card drop for 7
* Ace of Spades penalty logic
* Jack (J) and 8 control rules
* “Crazy” penalty (last card rule)
* User input (currently auto-play logic)
* GUI (JavaFX)

---

## 🛠️ Technical Notes

### Why `Game` Class is Important

Instead of putting logic in multiple places:

* All rules are centralized in `Game`
* This makes the system:

    * Easier to debug
    * Easier to extend
    * Cleaner to maintain

---

### Turn Movement Logic

The game uses modular arithmetic to ensure:

* No index out-of-bounds errors
* Smooth circular player rotation

---

## 🚀 Next Steps

Planned improvements:

* [ ] Implement Ace of Spades penalty system
* [ ] Add advanced rule chaining (stack effects)
* [ ] Add user interaction (input-based play)
* [ ] Implement full special card logic
* [ ] Introduce JavaFX UI
* [ ] Add multiplayer/network support

---

## 💡 Learning Focus

This project emphasizes:

* Deep understanding over fast implementation
* Clean and scalable design
* Real-world software architecture principles

---

## ✍️ Author Notes

This project is being built step-by-step with continuous improvements.


---
