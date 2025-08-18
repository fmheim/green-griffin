# A Modern Word Building Game for Android

A modern implementation of a classic word building game for Android, built with Kotlin and Jetpack Compose. The game currently supports German language play with real-time word validation using AI.

## Features

### Current Features
- 🎮 Classic word placement gameplay mechanics
- 🎯 Drag and drop interface for tile placement 
- 🃏 Joker Selector 
- ✅ Fast word validation using local db (sv and de)
- 🎲 Point calculation based on letter values and board multipliers
- 🔄 Automatic tile refill system
- 🏗️ Built with modern Android architecture
  - Jetpack Compose for UI
  - ViewModel for state management
  - Clean Architecture principles

### Technical Implementation
- Built with Kotlin and Jetpack Compose
- Uses MVVM architecture pattern
- Implements real-time word validation using Gemini 1.5 Flash
- Follows Android's recommended app architecture
- Reactive state management using StateFlow

## Upcoming Features

### Game Mechanics
- [ ] New game mode where you need to reach a certain location by starting at a start location and moving to en end location
  - [ ] blocking walls/tiles
  - [ ] other special tiles: boost, joker, new letters, teleport
  - [ ] coop vs competitive mode
### Game Modes
- [ ] Meta screen implementation
  - [ ] Start new game
  - [ ] Continue game
  - [ ] Game mode selection
- [ ] Multiplayer mode
- [ ] Single player mode with AI opponent

### Language Support
- [ ] Multiple language support
- [ ] Language learning features
  - [ ] Word definitions
  - [ ] Usage examples
  - [ ] Pronunciation guides

### Technical Improvements
- [ ] Persistent storage for game state
- [ ] Architecture improvements
- [ ] Enhanced word validation
  - [ ] Validate tiles when moving back to rack
- [ ] Performance optimizations