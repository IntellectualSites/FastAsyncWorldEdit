# Draft PR: single lock order for chunk GET call/trim, and main-thread handling of pending block entities

> This file is a draft pull request description. It is not part of the change: delete it before merging.
>
> Branch: `thunder/practical-thompson-wel30e`. Supersedes #3543 and #3722. Fixes #3490.

## AI disclosure

The two commits on top of #3543 (`9614261`, `8dba60a`) and this document were written by an AI assistant (Claude Code)
at MattBDev's direction. The changes were applied to the nine adapters by script, not by hand.

What the AI did and did not verify:

| Check | Status |
|---|---|
| Lock-order reasoning | Done by reading the code only. No tool-assisted analysis, no thread dumps. |
| Compile, `9614261` | Only `adapter-26.3` was compiled (`compileJava`), successfully. The other eight were not. |
| Compile, `8dba60a` | **Not compiled for any adapter.** |
| Unit tests | None run. None added. |
| Running server | Nothing was run on a server, on any version, on Paper or Spigot. |
| Server internals | Read from the bytecode of the 26.3 mapped server jar only. Assumed, not checked, for the other versions. |

Treat every claim below as something to confirm, not as established. The section "Verification tasks for the reviewer"
lists what needs a human.

## Overview

`PaperweightGetBlocks` could deadlock between a chunk call (`internalCall`), cache trimming (`trim`) and readers, which
stalls the blocking executor and every queued edit behind it. #3543 and #3722 each fixed part of this and each left
the other part in place. This branch combines them and closes the gaps between them, in all nine Bukkit adapters
(1.21, 1.21.4, 1.21.5, 1.21.6, 1.21.9, 1.21.11, 26.1, 26.2, 26.3).

It also carries #3543's fix for editing freshly generated chunks, reworked so that the chunk's pending block entities
are only touched on the main thread.

## Contents of the branch

1. dordsor21's seven commits from #3543, unchanged (head `4e10f35`).
2. `9614261` fix: single lock order for chunk GET call and trim.
3. `8dba60a` fix: only touch pending block entities on the main thread.

#3722 is not merged in as a commit (it conflicts with #3543 in every adapter file). Its changes and its comments were
re-applied by hand on top of #3543 as part of `9614261`.

## The lock order

Outermost first:

    NMS chunk -> section lock (sectionLocks[i]) -> section DelegateSemaphore -> sectionLock -> this

Readers already take locks in this order (`CharBlocks` `EMPTY.get` -> `update` -> `getSections` -> `getChunk`). The
rules that follow from it, now documented on the `sectionLock` field:

- Nothing may take a section lock while holding `sectionLock` or `this`.
- Nothing may take `sectionLock` while holding `this`.
- `reset()` and `super.trim(...)` take section locks, so they run only after both are released.

## Changes

Line numbers are for `adapter-26.3/.../v26_3/PaperweightGetBlocks.java` on this branch. The same change is in all
nine adapters.

### From #3543 (unchanged)

- `AbstractBukkitGetBlocks.call` is no longer `synchronized`; `forceLoadSections` is `volatile`.
- `internalCall` takes `synchronized (this)` only around the parts that need it.
- `createCopy` is read once into a local at the start of `internalCall`.
- `trim(false)` reads the forced sections before `synchronized (this)` and collects the layers to trim.

### From #3722 (re-applied, comments kept)

- `internalCall` (line 554) and `updateGet` (line 837): `reset()` is deferred with a `resetAll` flag until
  `sectionLock` is released.
- `trim(true)` (line 1108): `super.trim(true)` runs after `sectionLock` is released.

### New in `9614261`

- `internalCall`: the deferred `reset()` also runs outside the `synchronized (this)` that #3543 added there.
- `internalCall` (line 591): `newChunkSection(this::load, ...)` is no longer wrapped in `synchronized (this)`.
  `load` can reach `getSections`, which takes `sectionLock`, and that must come before `this`.
- `trim` (lines 1082-1093): the "nothing cached" early return no longer calls `super.trim(true)` inside
  `synchronized (this)`.
- `trim(false)` (line 1149): `super.trim(false, ...)` runs outside `synchronized (this)` in all adapters. It was
  already outside in eight; 26.3 had it inside.
- `trim(false)` (line 1115): loop bound fixed from `i <= getSectionCount()` to `i < getSectionCount()`. The old bound
  threw `ArrayIndexOutOfBoundsException` on any non-aggressive trim with sections loaded.
- Lock order documented on the `sectionLock` field (line 115) and at each site.

### New in `8dba60a`

#3543 took the chunk's `pendingBlockEntities` out of the chunk at the start of the call, from the edit thread, and put
the ones the edit does not overwrite back near the end. That map is a plain `HashMap` that the server modifies on the
main thread, and it was left empty for the duration of the call.

- `PaperweightPlatformAdapter.clearPostProcessing` and `setPostProcessing` are replaced by
  `removePendingBlockEntities(chunk, predicate)` (line 659 of the 26.3 `PaperweightPlatformAdapter`). It must be
  called on the main thread. It removes only the entries the predicate matches and returns the positions left.
- `internalCall` calls it inside the existing main-thread task that removes the tiles the edit overwrites (line 396).
  Entries the edit does not overwrite stay in the chunk throughout.
- The remaining entries are still promoted in the call's sync tasks via `getBlockEntity` (line 760), as in #3543.
  The forced clear afterwards is gone, since `getBlockEntity` removes each entry itself.

## Behaviour changes to be aware of

- Dropping the block cache (`reset()`, `super.trim(true)`) is no longer atomic with the `sections`/`levelChunk`
  update. Another thread can read a cached layer in between. The cache is not more stale than it was before the lock
  was taken, but this is a change in timing.
- `newChunkSection(this::load, ...)` relies on the layer's section lock (held at that point) rather than `this`.
- Pending block entities that the edit overwrites are removed before the section work. If the call then throws, they
  are not restored. This matches what already happens to real block entities removed in the same task.

## Known gaps, not addressed

- The Spigot obfuscated names for `pendingBlockEntities` (`"j"` for 1.21 to 1.21.6, `"i"` for 1.21.9 and 1.21.11)
  come from #3543 and were not verified. A wrong name fails in the static initialiser of `PaperweightPlatformAdapter`.
- `createCopy` is read when `internalCall` runs, but `copyKey` is captured when `call` runs. On the deferred path
  (chunk not yet loaded) the two can disagree. This predates both PRs.
- `updateGet` writes `levelChunk` under `sectionLock` only, not `this`. Unchanged from #3543.
- No automated test covers any of the locking.

## Verification tasks for the reviewer

Each task says what to check and why it matters. They are ordered roughly by how much the AI's work depends on them.

### 1. Build every adapter

Run a full build, or at least `compileJava` for all nine adapters.

Why: `8dba60a` has never been compiled, and `9614261` was compiled for one adapter out of nine. The edits were made
by regex over nine files with small differences between them (parameter modifiers, argument lists), so a slip in one
file is plausible. Also confirm no unused imports were left behind in `PaperweightPlatformAdapter` and
`PaperweightGetBlocks` (the script removed `HashMap` and `CompoundTag` imports where it judged them unused).

### 2. Confirm the nine adapters carry the same change

Diff the branch against #3543's head (`git diff 4e10f35 HEAD`) and compare the hunks across adapters.

Why: the review effort only scales if one adapter can stand for all nine. The AI checked this for #3543 itself and
for #3722, but did not re-run the comparison after its own two commits.

### 3. Audit the lock order by hand

For one adapter, list every place a lock is taken and what is already held there. The sites are:

- `internalCall`: `synchronized (nmsChunk)`, `synchronized (sectionLocks[i])` (two places), `synchronized (lock)`,
  `sectionLock.writeLock()`, `synchronized (this)` (two places).
- `updateGet`: `sectionLock.writeLock()`.
- `getSections`: `sectionLock.writeLock()`, then `getChunk`.
- `getChunk`: `synchronized (this)`.
- `trim`: `synchronized (this)` (three places), `sectionLock.writeLock()`.
- Inherited from `CharBlocks`/`CharGetBlocks`: `reset()`, `reset(layer)`, `trim(boolean)`, `trim(boolean, int)`,
  `load`, `EMPTY.get`, all on section locks.

Then confirm, for each `synchronized (this)` block and each `sectionLock` region, that nothing called inside it can
reach a lock that should come earlier. Pay particular attention to calls that look harmless but take locks:
`load`, `update`, `hasSection`, `getSections`, `getChunk`, `reset`.

Why: this is the entire basis of the fix, and it was done by reading. An independent pass is the main safeguard.
Do not rely on the comments the AI wrote; check them against the code.

### 4. Confirm with dordsor21 why `newChunkSection` was synchronized on `this`

#3543 added `synchronized (this)` around `newChunkSection(this::load, ...)`. This branch removes it.

Why: the AI removed it because it inverts the lock order and because the section lock is already held. It does not
know what the original author meant it to protect. If it guarded something other than this layer's cached data, that
protection is now gone.

### 5. Review the windows opened by deferring `reset()` and `super.trim(true)`

In `internalCall`, `updateGet` and `trim(true)`, look at what another thread can observe between the unlock and the
deferred cache drop.

Why: #3722 was stress tested for a week with this deferral, but on top of `main`, where `call` was still
`synchronized`. On this branch `call` is not synchronized, so more can interleave. In `updateGet`, also confirm the
deferred `reset()` still runs before `this.blocks[layer] = arr`.

### 6. Check the `trim(false)` loop fix

Run the `PaperweightGetBlocksTrimTest` from MattBDev's review comment on #3543 (the thread on the loop bound) against
this branch. It should pass. Consider committing it.

Why: it is the only concrete regression test that exists for any of this, and it was written against #3543's
structure. Note the test was also AI-generated.

### 7. Verify the server-side assumptions about pending block entities, per version

For each supported server version, confirm in the server source:

- `ChunkAccess.pendingBlockEntities` is a `HashMap` (or at least not thread-safe).
- `LevelChunk.getBlockEntity(pos)` removes the entry from `pendingBlockEntities` and promotes it, and returns the
  existing block entity if one is already registered.
- `LevelChunk.postProcessGeneration` promotes every pending entry on the main thread and then clears the map.
- On Paper, `moonrise$isPostProcessingDone()` exists and is set at the end of `postProcessGeneration`.

Why: the AI confirmed these from bytecode for 26.3 only. `8dba60a` depends on `getBlockEntity` removing the pending
entry, because the forced clear from #3543 was dropped.

### 8. Confirm `removePendingBlockEntities` only ever runs on the main thread

Check that `TaskManager.taskManager().sync(...)` in `internalCall` always executes its task on the main thread,
including when `internalCall` is itself called from the main thread.

Why: the method is not thread-safe by design. If `sync` can run the task elsewhere, the race is back.

### 9. Reproduce #3490 and confirm it is still fixed

Edit an area that includes chunks that have never been generated and that will contain structures (chests, spawners).
Check that there are no errors in the log, that block entities the edit did not touch keep their data (chest loot
tables, spawner types), and that ones it overwrote are gone cleanly.

Why: `8dba60a` changes how #3543 fixed this issue. The AI never saw the original error and could not run a server,
so it cannot say the fix still works, only that the same entries end up removed and promoted.

### 10. Test on Spigot as well as Paper

Start a Spigot server on at least one version from each field-name group (`"j"` and `"i"`) and run an edit over
freshly generated chunks.

Why: on Spigot the "post-processing done" shortcut does not exist, so `removePendingBlockEntities` runs on every
chunk call. The reflective field lookup is also only exercised there.

### 11. Stress test for deadlocks

On a test server, run many concurrent edits over overlapping chunks, from several players or queues, under memory
pressure so that aggressive trims happen (a small heap is the simplest way). Take thread dumps (`jstack`) if anything
stalls and look for the lock cycles described in the Overview.

Ideally, also run this branch on the server where #3722 was stress tested, for a comparable period.

Why: lock-order bugs do not show up in a single edit. The week of testing behind #3722 does not transfer to this
branch, because the surrounding locking is different.

### 12. Check history and undo

With history enabled, make edits that touch blocks, tiles, entities and biomes, then undo and redo them.

Why: the `createCopy` path runs inside the reworked `internalCall`, including inside the main-thread task that
`8dba60a` extended.

### 13. Decide how the history should look

The branch contains dordsor21's commits followed by two AI-authored commits with a `Co-Authored-By` trailer. Decide
whether to keep, squash or re-author them, and remove this file before merging.

## Credits

- dordsor21: #3543, the call/trim locking rework and the fix for freshly generated chunks.
- MattBDev: #3722, the section lock-order fix and its comments, and the review that found the loop-bound bug.
