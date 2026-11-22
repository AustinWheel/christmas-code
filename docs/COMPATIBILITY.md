# Compatibility and testing

The plugin declares IntelliJ Platform **253 through 263.*** (2025.3–2026.3) and depends only on `com.intellij.modules.platform`. The release build is compiled against WebStorm 2025.3.2 with Java 21 bytecode.

## Binary verification

| Product | Version/build | Result |
| --- | --- | --- |
| WebStorm | 2025.3.2 / 253.30387.83 | Passed |
| GoLand | 2026.3 / 263.7307.211 | Passed |
| IntelliJ IDEA | 2026.3 / 263.7307.200 | Passed |
| WebStorm | 2026.3 / 263.7307.202 | Passed |
| PyCharm | 2026.3 / 263.7307.206 | Passed |
| PhpStorm | 2026.3 / 263.7307.215 | Passed |
| CLion, Rider, RubyMine, RustRover | 2026.3 | CI workflow configured; not yet verified |

Checks on representative products do not establish compatibility with every patch release or editor variant. The manual GitHub Actions **Compatibility** workflow checks nine products. Reports are retained as workflow artifacts.

The SDK marks `CustomHighlighterOrder` and its renderer ordering method as experimental. They are used to draw after the base editor fill but before text and selection backgrounds. Current verifier reports flag those usages; they do not report binary incompatibilities for the products marked passed. Do not extend the upper build bound without checking the next release.

## Automated tests

- Four settings tests cover defaults, loading preferences, defensive copying, opacity clamping, and preserving the startup preference.
- `sceneCheck` covers animation, transparency, repeatable rendering, snow occlusion at several rotation angles, ground collision, resizing, and particle recycling.

## Visual smoke checks

The user confirmed the editor background and shared splits in IntelliJ IDEA 2026.3 during development. These checks still need to be completed on the final ZIP in each primary target IDE; binary verification is not a visual test:

- Install from disk in a clean profile; open an ordinary code file and toggle on/off.
- Create horizontal and vertical splits. Confirm one continuous scene, then disable sharing and confirm separate trees.
- Change opacity to 0, 25, and 100; verify Apply and Cancel, then restart and check persistence.
- Enable automatic activation, close/reopen the project, and confirm startup. Toggle off for the session and ensure it stays off until the next open.
- Scroll, edit/delete all text, switch tabs, resize, and detach a window.
- Confirm text selection, caret, clicks, tooltips, and keyboard navigation remain usable.
- Confirm animation pauses in inactive windows, resumes smoothly, and releases resources on disable/project close.
- Observe CPU/memory at high DPI and with several splits.

Remote development/Split Mode, consoles, diff viewers, and special notebook editors are outside the tested scope. The normal IDE UI (gutters, dividers, tabs) interrupts the visual between panes.
