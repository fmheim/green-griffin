# A Modern Scrabble Game for Android

A modern implementation of the classic Scrabble word game for Android, built with Kotlin and Jetpack Compose. The game currently supports German language play with real-time word validation using AI.

## Features

### Current Features
- 🎮 Classic Scrabble gameplay mechanics
- 🎯 Drag and drop interface for stone placement
- ✅ Real-time word validation using Gemini AI
- 🔤 German language support
- 🎲 Point calculation based on official Scrabble rules
- 🔄 Automatic stone refill system
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
- [ ] Support for single letter word validation
- [ ] Implementation of joker stones
- [ ] Special board fields (double word points, double letter points)
- [ ] Stone shuffling in hand
- [ ] Quick actions for stone management
  - [ ] Move all unlocked stones from board to hand
  - [ ] Only show stone refill button after word submission

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
  - [ ] Validate stones when moving back to hand
- [ ] Performance optimizations