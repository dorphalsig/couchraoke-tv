---
name: orchestration
description: Coordinate implementation slices from tasks.md. Use for phase/slice implementation, test+impl task pairing, direct vs subagent execution, gate validation, audit, task tracking, and marking slices done.
---

# Orchestration

Use for implementation work defined in `tasks.md`.

## Read first

Read if not already loaded:

- `CLAUDE.md`
- `.specify/memory/constitution.md`
- `plan.md` — the slice list and the gates each slice declares
- `tasks.md`

`tasks.md` is the source of truth for tasks. `plan.md` is the source of truth for gates.

## Core rules

- Mirror the requested slice from `tasks.md` into the task tracker before coding.
- Keep the task tracker synchronized with `tasks.md`.
- Work in `tasks.md` order.
- Pair each test task with its matching implementation task.
- One test + implementation pair is one unit of work.
- Without subagents: work on one pair at a time.
- Parallel work is allowed only through subagents.
- A `parallelizable` marker means eligible for subagent parallelism, nothing more.
- The mandatory workflow is the only allowed execution path.
- Do not mark a pair done until its gates and spot-checks pass.
- **Do not mark a slice done until its L or D gate passes.** Passing every pair is not passing the
  slice.

## Two tiers of proof

Pairs and slices are proved differently, and conflating them is what let three implementations ship
broken.

A **pair** is proved by U and S: the unit tests for the changed FQCNs, and the screenshot gate if it
touched UI. These run fast and catch logic and layout regressions.

A **slice** is proved by L or D: the real `mockphone` over loopback, or the real device. Only these
can observe a native decoder, a real socket, or a real clock. Every green pair in a slice can still
add up to an app that plays no video.

Re-running the pair-level gates is not independent verification. It is the same blind gate run
twice.

## Mandatory workflow

Follow this sequence for the requested slice. Do not skip, reorder, or parallelize steps except
where this skill explicitly allows subagents.

1. Find the slice in `tasks.md` and its declared gates in `plan.md`.
2. If the slice declares no L or D gate, stop and say so. The slice is not implementable as written.
3. Mirror its tasks into the task tracker.
4. Pair matching test and implementation tasks.
5. For each pair, in `tasks.md` order:
   - mark the pair `in_progress`
   - implement it using the `implementation` skill
   - load `gradle-validation`
   - independently run the pair's U gate using the FQCNs returned by implementation, and the S gate
     if it touched UI
   - run 2–5 spot-checks against the spec
   - if implementation returned green, your gates pass, and the spot-checks pass, mark the pair done
     in the tracker and `tasks.md`
   - if any fails, fix using the `implementation` failure protocol and repeat
6. When every pair is done, run the slice's L or D gate.
7. Mark the slice done only if that gate passes. If it fails, the slice is not done regardless of
   how many pairs are green — reopen the pairs the failure implicates.

**Do not start the next pair until the current pair is done or blocked. Do not start the next slice
until the current slice's L or D gate has passed.**

## Subagents

Use subagents only when the user explicitly allows delegated or parallel work.

When using subagents:

- Assign exactly one test + implementation pair per subagent.
- Use one branch and one worktree per pair.
- Put worktrees under `.claude/worktrees`.
- Do not reuse worktrees.
- Do not check out the same branch in multiple worktrees.
- Never delegate the slice-level L or D gate. Run it yourself, on the integrated slice.

Each subagent prompt must say:

- read `CLAUDE.md`
- load `navigation`, `implementation`, and `gradle-validation`
- implement only the assigned pair
- follow the `implementation` workflow
- return changed files, gate results, and spec notes

## Failures

If anything fails or is unclear:

- do not delegate the fix
- keep the pair incomplete
- follow the `implementation` failure protocol
- rerun the gates
- repeat the spec spot-check

A gate that could not run is `blocked`, not passed. Report it and stop.

## Task order that cannot compile

`tasks.md` is ordered by user story, so a class is often *constructed* by an earlier task than the
one that *creates* its collaborators. Taken literally the unit does not compile.

Before delegating a unit, read the constructor signature of every class it builds and confirm each
collaborator already exists. When one does not:

- pull the missing class **declarations** forward into the current unit, with their exact
  `contracts/` signatures and minimal bodies
- leave the **behaviour** to the task that owns it, and name that task in a comment
- do not renumber or reorder `tasks.md`, and do not implement the later task early

Implementing the behaviour early is the tempting error. It creates a merge hazard with the story
that owns the file and leaves that story's tests with nothing left to prove.

Record the ordering defect as an out-of-scope observation and report it. It is a real flaw in the
plan, and silently absorbing it hides that from whoever writes the next `tasks.md`.

Watch the coverage gate when you do this. A class in `--src` whose methods are deliberately stubs may
fall under the JaCoCo threshold through no fault of the implementation. Keep each stub to a single
expression and test the behaviour the phase genuinely implements — under a LINE+BRANCH gate that is
usually enough, because a one-line `TODO()` costs one line and no branches.

If it still fails, read the per-counter numbers before reacting. A class can fail on one counter and
pass comfortably on another, and the counter that failed tells you whether the gap is untested logic
or merely unwritten code. Only then decide whether to defer the class from `--src` until the owning
story fills it in. That decision is yours, not a subagent's, and it belongs in `plan.md` with its
reason.

## Delegating

Each subagent starts with no memory of what the last one hit. Carry the lessons forward yourself:
brief every subagent on the traps the previous units in this slice actually ran into, not just the
task text. In this repository that has consistently meant vacuous fixture loops, contract-versus-
prompt conflicts, and scoped test runs that skip detekt.

Subagents work in worktrees where `.github/skills/` does not exist. Pass main-checkout absolute
paths for the skills, or the subagent silently proceeds without them.

## Audit

Before marking a slice done, confirm all of:

- implementation returned `green` for every pair
- navigation checkpoint exists
- changed files match scope, and nothing unexpected changed
- the pair-level U gate passes independently
- the S gate verifies against a committed baseline, and any baseline change was inspected and
  justified
- **the slice's L or D gate passes**
- no gate was satisfied by a skipped test
- UI matches the spec at `w960dp-h540dp-land-television-xhdpi-notouch`
- behaviour matches the spec
- `@NoCoverageGenerated` usage is valid
- the tracker matches `tasks.md`
