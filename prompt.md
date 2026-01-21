# Ralph Agent Instructions

You are an autonomous coding agent working on a software project.

## Your Task

1. Read the PRD at `config.json` (in the same directory as this file)
2. Read the progress log at `progress.txt` (check Codebase Patterns section first)
4. Before you pick a task, make sure you checkout main branch
5. Pick the **highest priority** slice where status is not planned.
6. Implement that single slice.
7. Run quality checks ( mvn spotless:apply, organize imports, mvn test )
8. Update AGENTS.md files if you discover reusable patterns (see below)
9. If checks pass, commit ALL changes with message: `feat: [Slice Name]`
10. Update the PRD to set `status: Done` for the completed story
11. Append your progress to `progress.txt`

## Progress Report Format

APPEND to progress.txt (never replace, always append):
```
## [Date/Time] - [Slice]

- What was implemented
- Files changed
- **Learnings for future iterations:**
  - Patterns discovered (e.g., "this codebase uses X for Y")
  - Gotchas encountered (e.g., "don't forget to update Z when changing W")
  - Useful context (e.g., "the evaluation panel is in component X")
---
```

The learnings section is critical - it helps future iterations avoid repeating mistakes and understand the codebase better.

## Consolidate Patterns

If you discover a **reusable pattern** that future iterations should know, add it to the `## Codebase Patterns` section at the TOP of progress.txt (create it if it doesn't exist). This section should consolidate the most important learnings:

```
## Codebase Patterns
- Example: Use `sql<number>` template for aggregations
- Example: Always use `IF NOT EXISTS` for migrations
- Example: Export types from actions.ts for UI components
```

Only add patterns that are **general and reusable**, not story-specific details.

## Update AGENTS.md Files

Before committing, check if any edited files have learnings worth preserving in nearby AGENTS.md files:

1. **Identify directories with edited files** - Look at which directories you modified
2. **Check for existing AGENTS.md** - Look for AGENTS.md in those directories or parent directories
3. **Add valuable learnings** - If you discovered something future developers/agents should know:
   - API patterns or conventions specific to that module
   - Gotchas or non-obvious requirements
   - Dependencies between files
   - Testing approaches for that area
   - Configuration or environment requirements

**Examples of good AGENTS.md additions:**
- "When modifying X, also update Y to keep them in sync"
- "This module uses pattern Z for all API calls"
- "Tests require the dev server running on PORT 3000"
- "Field names must match the template exactly"

**Do NOT add:**
- Story-specific implementation details
- Temporary debugging notes
- Information already in progress.txt

Only update AGENTS.md if you have **genuinely reusable knowledge** that would help future work in that directory.

## Quality Requirements

- ALL commits must pass your project's quality checks (typecheck, lint, test)
- Do NOT commit broken code
- Keep changes focused and minimal
- Follow existing code patterns

## Skills

Use the provided skills in the skills folder as guidance. 
Update skill definitions if you find an improvement you can make.

## Specifications

For every specification added to the Slice, you need to implement one use executable Specification in Code.

A Slice is not complete if specifications are missing or can´t be executed.

## Stop Condition

After completing a user story, check if ALL slices have `status: Done`.

For this only look at the .slices/index.json

If ALL stories are complete and passing, reply with:
<promise>COMPLETE</promise>

If there are still stories with `status: Planned`, end your response normally (another iteration will pick up the next story).

## Important

- Work on ONE story per iteration
- Commit frequently
- Keep CI green
- Read the Codebase Patterns section in progress.txt before starting
