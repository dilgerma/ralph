# RALPH Loop with Event Modeling – Run the Loop, Not the Hype

This repository contains the **minimal tooling** needed to run a **RALPH loop** on a well-defined set of Event Modeling Slices.

Nothing more. Nothing magical.

If you’re looking for an “AI that builds your product from a single prompt” — this is **not** that repo.

If you already understand that **the hard part is defining the work**, and you want an agent to execute it relentlessly, slice by slice — you’re in the right place.

[Find the Article here](https://www.linkedin.com/pulse/building-systems-auto-pilot-ralph-loop-production-way-dilger-gdhic/)

The Miro Toolkit used to Event Model this:

[Miro Toolkit](https://nebulit.de/en/eventmodeling-tooling)

Live Event Modeling Webinar (120 minutes)

[Recording](https://youtu.be/6DqaNKxjvko)


---

## What This Repository Is

This repo gives you:

- A **repeatable loop** for running an AI coding agent
- A **simple task contract** (PRD + slices)
- A **learning feedback mechanism** for the agent
- A **progress log** you can actually inspect

In short:  
👉 **Define the work properly, then let the loop run.**

---

## What This Repository Is *Not*

- ❌ Not a framework
- ❌ Not prompt engineering magic
- ❌ Not “just build it” automation
- ❌ Not useful without clear task definitions

The loop automates the **easy 25%** (execution).  
You are still responsible for the **hard 75%** (thinking).

---

## The Core Idea

The RALPH loop applies a very old idea to AI agents:

> Break work into small, well-defined chunks.  
> Execute.  
> Learn.  
> Iterate.  
> Repeat.

That’s it.

This is **real Agile**, applied to AI.

---

## How the Loop Works

At a high level:

1. You model the system (ideally using Event Modeling)
2. You export the model into machine-readable slices
3. The agent:
    - Picks the most important slice
    - Implements it completely
    - Documents learnings
    - Updates progress
    - Moves on
4. The loop repeats until stopped

### The Loop (Conceptually)

```bash
while true; do
  run-agent
  collect-learnings
  update-progress
done
```

Persistence beats intelligence.

---

## Repository Structure

```text
.
├── index.json        # PRD – list of slices & priorities
├── .slices/          # One folder per slice (serialized Event Model)
├── prompt.md         # The only prompt the agent needs
├── Agents.md         # Accumulated agent learnings
├── progress.txt      # Append-only execution log
└── ralph.sh          # The loop runner
```

---

## Task Definition (This Matters)

The loop only works if:

- Slices are **small**
- Boundaries are **clear**
- Acceptance criteria are **unambiguous**
- Overlap is **avoided**

Garbage slices → garbage code  
At **machine speed**

---

## Forward *and* Reverse Loops

This setup works in two directions:

### Forward
- Event Model → Code
- Slice by slice implementation
- Autonomous execution

### Reverse
- Legacy codebase → Extracted model
- Capture implicit business logic
- Layer in human domain knowledge
- Rebuild cleanly from the model

This is where things get interesting.

---

## What the Agent Learns

Each iteration updates `Agents.md` with things like:

- Architectural patterns
- Framework constraints
- Gotchas and edge cases
- Cross-slice consistency rules

The agent doesn’t just execute —  
it **accumulates context**.

---

## What You Should Expect

- ✅ Fast, relentless execution
- ✅ Shockingly good knowledge extraction
- ✅ Very little “AI magic”
- ❌ No shortcuts around thinking
- ❌ No rescue from vague requirements

This is **SDLC on autopilot** —  
*after* you did the real work.

---

## Who This Is For

- Engineers modernizing legacy systems
- Teams already doing Event Modeling
- People who know “just build it” never worked
- Anyone tired of hype and ready for discipline

---

## Final Note

AI agents are not magic.

They’re **really fast junior developers**.

Give them:
- Clear tasks
- Clean boundaries
- A tight loop

Then step back and let persistence do its thing.

---

**Author:** Martin Dilger  
Author of *[Understanding Event Sourcing](https://www.eventsourcingbook.com)*  
Introduce this to your Team? [Let´s talk](httos://www.nebulit.de)
Event Modeling · Event Sourcing · Vertical Slices

( based on the work in this Repo : https://github.com/snarktank/ralph)