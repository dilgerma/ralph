# Ralph Agent Instructions

## Overview

Ralph is an autonomous AI agent loop that runs Amp repeatedly until all PRD items are complete. Each iteration is a fresh Amp instance with clean context.

## Commands

```bash

# Run Ralph (from your project that has prd.json)
./ralph.sh [max_iterations]
```

## Key Files

- `ralph.sh` - The bash loop that spawns fresh Amp instances
- `prompt.md` - Instructions given to each Amp instance
- `config.json.example` - Example PRD format

## Patterns

- Each iteration spawns a fresh instance with clean context
- Memory persists via git history, `progress.txt`, and `config.json`
- Only  work on one slice at a time
- Always update AGENTS.md with discovered patterns for future iterations
