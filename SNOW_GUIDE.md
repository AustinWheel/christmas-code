# Optional exercise: falling snow in Java

The snowfall is now fully implemented in `SnowField.java`. This original exercise remains as a learning reference; no TODOs remain to complete.

Work in `src/main/java/dev/austin/christmas/scene/SnowField.java`.

You don't need IntelliJ APIs. This class receives a drawing surface and elapsed time. It already runs inside the standalone preview and plugin.

## The contract

```java
public void update(double dt, int width, int height)
public void paint(Graphics2D g, int width, int height)
```

- `dt` is elapsed **seconds**, usually about `0.033`. It is capped at `0.05` upstream.
- `width` and `height` are the visible editor area, in pixels—not the entire document.
- `(0, 0)` is the top-left. Positive x goes right; positive y goes **down**.
- Store coordinates as `double`; round only when drawing.
- Both methods run on the UI thread. No extra threads, timers, or `Thread.sleep` needed.
- `paint` may run more than once between updates. Keep all movement/random respawning in `update`.
- The scene already sets global opacity. Use ordinary colors first; don't reset the graphics composite.

## Milestone 1 — draw one flake

Start only in `paint`. Set a pale blue/white color and draw a circle at a fixed position.

Useful Java2D calls:

```java
g.setColor(new Color(210, 225, 245));
g.fillOval(x, y, diameter, diameter);
```

These are API examples: supply integer coordinates and diameter. You will need `import java.awt.Color;`.

Run `./dev.sh preview`. Success: one visible flake on the left side of the window, away from the tree. Close and rerun the preview after editing.

## Milestone 2 — move it

Add fields for its position and speed. Try a downward speed of **45 pixels per second**.

The movement equation is:

```text
new y = old y + speed × dt
```

Put movement in `update`, and draw using that stored position in `paint`. When the flake passes `height`, put it just above the top, at `-diameter`.

Success: it takes roughly 16 seconds to fall through a 720px window, and it reappears at the top. If it crosses in a fraction of a second, check your units.

## Milestone 3 — make a field of flakes

Create a small mutable nested `Flake` class with fields such as:

| Field | Type | Meaning |
|---|---|---|
| `x`, `y` | `double` | Current position |
| `speed` | `double` | Downward pixels per second |
| `diameter` | `int` | Circle size, around 1–4 pixels |
| `phase` | `double` | Optional unique offset for drifting |

Store about **100** flakes in an `ArrayList<Flake>`. Create a single `Random` as a field. Initialize the list the first time `update` sees valid dimensions; don't initialize another 100 every frame.

Suggested random ranges:

- x: anywhere from 0 to width.
- y: anywhere from 0 to height initially, so the scene starts full.
- speed: 18–65 pixels per second.
- diameter: 1–4 pixels.

Useful formula: `minimum + random.nextDouble() * (maximum - minimum)`.

Loop over the list to update positions, then loop over it again to paint. Reuse flakes when they leave the screen rather than adding new objects forever. Choose a new x when recycling.

Success: flakes start scattered, fall at different speeds, and their count stays constant after several minutes.

## Milestone 4 — gentle drift and resize

Add a `time` field and advance it once per `update` by `dt`. One possible horizontal velocity is:

```text
horizontal velocity = sin(time × 0.6 + flake phase) × 8
new x = old x + horizontal velocity × dt
```

Use a different phase per flake, from 0 to `2π`, so they don't all move in lockstep. Wrap flakes crossing the left/right edge. If the viewport shrinks, recycle or wrap particles into the new bounds. Avoid dividing or taking a remainder by zero; the starter already guards zero-sized updates.

Success: subtle independent drifting, no permanent disappearance after resizing, and no jumps after switching away and returning.

## Optional polish after it works

- Bigger flakes fall faster, suggesting that they are closer.
- Draw `*`, `.`, or `+` with `g.drawString(...)` for code-like snow. Text y is its baseline.
- Give distant flakes dimmer colors.
- Tune density based on window area, with a sensible upper limit.

Start with milestone 1, then bring back your `SnowField.java` for a review. Keep the initial version simple; the tree, timer, and IDE integration are already handled.
