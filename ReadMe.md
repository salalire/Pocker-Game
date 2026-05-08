# 🃏 Poker Game (Java + Maven)

## 📌 Project Goal

This project is a **from-scratch implementation of a Poker game** using Java.
The main objective is not just to build a working game, but to **understand every component deeply** and design it in a way that is **clean, extendable, and maintainable**.

This project will evolve step-by-step:

* Start with core game logic (cards, players, deck)
* Add game rules (Poker logic)
* Introduce UI using JavaFX
* Improve architecture for scalability (multiplayer, networking, etc.)

---

## 🏗️ Project Structure

```
src/
 ├── main/
 │   ├── java/
 │   │   ├── game/
 │   │   │   ├── Card.java
 │   │   │   ├── Deck.java
 │   │   │   └── Player.java
 │   │   │
 │   │   └── myapp/
 │   │       └── Main.java
```

---

## 📦 Package Design (WHY this structure?)

### 🔹 `game` package

This package contains the **core domain logic** of the Poker game.

Think of it like the *real-world objects* in a card game:

* `Card` → represents a single playing card
* `Deck` → represents a full collection of cards
* `Player` → represents a player in the game

👉 These classes are **independent of UI or input/output**
👉 This makes them reusable (console, GUI, network, etc.)

---

### 🔹 `myapp` package

This package controls the **application entry point**.

* `Main.java` → starts the program

👉 Keeps execution logic separate from game logic
👉 Makes the system cleaner and easier to scale

---

## 🧠 Core Classes (Detailed Explanation)

### 🂡 Card.java

Represents a single playing card.

#### Responsibility:

* Store **rank** (Ace, King, etc.)
* Store **suit** (Hearts, Spades, etc.)

#### Why it exists:

A Poker game is built on cards — this is the **smallest unit** of the system.

#### Example:

```java
Card card = new Card("Ace", "Spades");
```

---

### 🃏 Deck.java

Represents a full deck of cards.

#### Responsibility:

* Create all 52 cards
* Shuffle cards
* Deal cards to players

#### Why it exists:

Instead of creating cards manually, the deck manages:

* card generation
* randomness
* distribution

#### Real-life analogy:

Like a dealer holding and distributing cards.

---

### 👤 Player.java

Represents a player in the game.

#### Responsibility:

* Store player name
* Store player's cards (hand)
* Track player state (later: chips, bets, etc.)

#### Why it exists:

Poker is player-driven — each player must maintain their own state.

---

### 🚀 Main.java

Entry point of the application.

#### Responsibility:

* Start the program
* Create deck and players
* Control game flow (temporary for now)

#### Why it exists:

Separates **execution logic** from **game logic**

---

## ⚙️ Why Maven?

This project uses Maven to:

* Manage dependencies (JavaFX later)
* Standardize project structure
* Make builds consistent

#### Future use:

* Add JavaFX libraries
* Add testing frameworks (JUnit)
* Package the application

---

## 🔄 Current Status

✅ Maven project created
✅ Basic package structure created
✅ Core classes initialized:

* Card
* Deck
* Player

🚧 Next Steps:

* Implement Card logic (rank & suit properly)
* Build Deck (generate + shuffle)
* Add Player hand system
* Start simple game flow (deal cards)

---

## 🧩 Design Philosophy

This project follows:

### 1. Separation of Concerns

Each class has **one responsibility only**

### 2. Scalability

Code is written so we can later add:

* GUI (JavaFX)
* Multiplayer (Sockets)
* AI players

### 3. Real-World Modeling

Classes mirror real-life objects:

* Card → real card
* Deck → real deck
* Player → real player

---

## 🛠️ Future Roadmap

* [ ] Implement card suits and ranks using enums
* [ ] Implement deck shuffle logic
* [ ] Deal cards to players
* [ ] Add Poker rules (hand ranking)
* [ ] Add betting system
* [ ] Build JavaFX UI
* [ ] Add multiplayer support

---



## ✍️ Author Notes

This README will be updated as the project evolves.
Each feature will be added with explanation to ensure **deep understanding**, not just implementation.

---
