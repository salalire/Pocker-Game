# 🃏 Custom Card Game (Java + Maven)

## 📌 Project Overview

This project is a **from-scratch implementation of a custom card game** using Java.
The goal is not only to build a working game but to **deeply understand system design, object-oriented programming, and scalable architecture**.

The project has evolved from a simple console-based card system into a **fully interactive rule-based game with a Swing UI**.

---

## 🏗️ Project Structure
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

---

## 🧠 Core Design Philosophy

### 1. Separation of Responsibilities

Each class has a clear role:

| Class      | Responsibility                            |
| ---------- | ----------------------------------------- |
| `Card`     | Represents a single card                  |
| `Deck`     | Manages cards (create, shuffle, deal)     |
| `Player`   | Stores player state and hand              |
| `Game`     | Controls game logic and rules             |
| `MainUi`   | Handles UI and user interaction           |
| `Suit` / `Order` | Define valid card values           |

---

### 2. Logic vs UI Separation (Important)

- All rules are inside the `Game` class
- UI only calls methods and displays results
- No game logic is written in UI

This ensures:

* Easier debugging
* Cleaner architecture
* Better scalability

---

### 3. Use of `enum`

Instead of using `String`, the project uses:

* `Suit` (HEARTS, SPADES, CLUBS, DIAMONDS)
* `Order` (ONE, TWO, THREE, ..., KING)

#### Why?

* Prevent invalid values
* Improve readability
* Simplify rule implementation

---

## 🧩 Current Game Logic (Implemented)

### 🎯 Game Setup

* Multiple players supported
* Each player receives **6 cards**
* First player receives **7 cards**
* First player starts the game

---

### 🔄 Turn-Based System

Each player can:

* Play a valid card
* Draw ONE card per turn
* Pass the turn strategically

---

### 📜 Valid Move Rule

A card can be played if:

> It matches the **suit OR rank** of the current top card  
> OR is a special card (J or 8)

---

### 🔁 Player Rotation Logic

The game supports:

* Forward direction
* Reverse direction
* Circular movement (no index errors)

---

## 🟡 Special Rules (Implemented)

#### 🔸 Card: 5

* Next player is **skipped**

#### 🔸 Card: 7

* Game direction is **reversed** — **only when played alone**
* Player may drop **up to 4 additional cards** alongside the 7 (max 5 cards total)
* All extra cards must share the **same suit** as the 7
* When bundled with extras, the 7 does **NOT** reverse direction — it acts purely as a carrier card

#### 🔸 Ace of Spades

* Triggers penalty
* Next player must:
    * Play **2 of Spades** OR
    * Draw penalty cards

#### 🔸 Card: 2 (Stacking Penalty)

* Adds +2 cards penalty
* Players can stack penalties:
    * 2 → 4 → 6 → ...

#### 🔸 Card: J or 8

* Can be played anytime
* Player chooses the next suit

---

## ♻️ Deck Management

Problem:

* Game crashed when deck became empty

Solution:

* Discard pile is reshuffled back into deck
* Top card is preserved

---

## 🖥️ UI Features (JavaFX)

* Green felt table background with gradient
* Real card rendering with suit symbols (♥ ♦ ♠ ♣), rank labels, proper red/black colouring
* Cards in hand lift on hover and are clickable
* **Seven bundle mode**: click a 7 to enter bundle mode, then click same-suit cards to select extras, then click the 7 again to confirm the play
* Draw button → draw one card per turn
* Pass button → end your turn
* Direction indicator (Clockwise / Counter-Clockwise)
* Winner screen with trophy

---

## 🏆 Win Condition

* Game ends when a player has **0 cards**
* UI displays the winner

---

## ⚙️ Game Flow

1. Deck is created and shuffled
2. Players are added
3. Cards are distributed
4. First player starts
5. Game continues via UI interaction:
    * Play / Draw / Pass
    * Apply rules
    * Move to next player
6. Game ends when a player wins

---

## 🧪 Current Features

✅ Deck creation using enums  
✅ Card shuffling  
✅ Player hand management  
✅ Turn-based gameplay  
✅ Valid move checking  
✅ Skip rule (5)  
✅ Reverse rule (7)  
✅ Ace penalty system  
✅ Two stacking penalty  
✅ J/8 suit control  
✅ Deck reshuffling  
✅ Swing UI interaction  
✅ Win detection

---

## 🚧 Future Improvements

* Add card images instead of text
* Improve UI layout
* Add AI players
* Multiplayer support (network)
* Sound effects and animations
* Score tracking
* Advanced rules (crazy mode)

---

## 🛠️ Technical Notes

### Why `Game` Class is Important

* All rules are centralized in `Game`
* Makes system:
    * Easier to debug
    * Easier to extend
    * Cleaner to maintain

---

### Turn Movement Logic

* Uses modular arithmetic
* Prevents index out-of-bounds
* Ensures smooth circular gameplay

---

## 💡 Learning Focus

This project emphasizes:

* Deep understanding over quick coding
* Clean architecture design
* Separation of concerns
* Real-world software engineering practices

---

## ✍️ Author Notes

This project is built step-by-step with continuous improvements and design refinement.



---