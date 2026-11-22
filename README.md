# Christmas Code

An animated ASCII Christmas tree and falling snow behind your code, for IntelliJ-based IDEs.

![Christmas Code scene preview](docs/images/scene-preview.png)

*Standalone renderer preview at 95% opacity. The editor background defaults to a subtler 25%.*

## Features

- A rotating character-art tree, gold and amber garlands, glowing red/orange ornaments, and presents.
- Falling snow that stays behind the tree and settles at the ground.
- A single continuous scene across split editors, or an independent scene in each pane.
- Adjustable opacity and optional automatic activation when projects open.
- Animation pauses when the IDE window is inactive or the editor is hidden.
- Runs locally: no accounts, analytics, network calls, or document-content collection.

## Install and use

Download the plugin ZIP from [Releases](https://github.com/AustinWheel/christmas-code/releases), then use **Settings → Plugins → gear menu → Install Plugin from Disk**. Select the ZIP without extracting it and restart if prompted.

Open a code file, then choose **Tools → Toggle Christmas Background**. Configure the effect under **Settings → Tools → Christmas Code**:

| Setting | Default | Behavior |
| --- | --- | --- |
| Share one scene across split editors | On | Each pane shows its part of one scene; detached windows get separate scenes. |
| Background opacity | 25% | 0–100%; changes apply without restarting. |
| Enable automatically when opening a project | Off | Applies the next time a project opens. The Tools toggle overrides the current session only. |

Preferences are saved globally across projects. Gutters, split dividers, and tool windows retain their usual appearance. Scrolling code does not move the scene. The background is intended for the standard desktop code editor; consoles, diff viewers, remote development, and special notebook editors are not supported/tested.

## Compatibility

The descriptor targets platform builds **253–263.*** (2025.3 through 2026.3). It depends only on the shared IntelliJ Platform, rather than Java-, JavaScript-, or Go-specific plugins. See [compatibility results and limitations](docs/COMPATIBILITY.md) for the tested IDEs. Binary verification is separate from visual testing.

## Development

Use JDK 21 or newer. The default build downloads a pinned WebStorm 2025.3.2 SDK; no installed IDE is required.

```sh
./gradlew test sceneCheck buildPlugin
./gradlew runIde
./gradlew preview
```

To use an installed IDE instead:

```sh
./gradlew -PideaPath="/path/to/IDE.app" runIde
```

On macOS, `./dev.sh` uses an installed IntelliJ IDEA and its bundled Java runtime when available. Override the runtime with `CHRISTMAS_JAVA_HOME`. A 2026.3 IDE requires Java 25; the plugin's own bytecode targets Java 21.

Useful tasks:

- `test`: settings defaults, persistence loading, and invalid-value handling.
- `sceneCheck`: animation, transparency, deterministic painting, snow occlusion, collision, and resize/recycling checks.
- `sceneSnapshot`: generate `build/scene-preview.png`.
- `sceneTurntable`: export a full tree rotation and report rendering times.
- `verifyPlugin -PverifyProduct=GO -PverifyVersion=2026.3`: check a downloaded IDE with Plugin Verifier.
- `verifyPlugin -PverifyIdePath="/path/to/IDE.app"`: check an installed IDE.

The distribution ZIP is written to `build/distributions/`. See [release instructions](docs/RELEASING.md).

## Architecture

`ChristmasBackgroundService` renders each scene once per timer tick into a reusable image. An editor-local custom highlighter paints the cached image before code and selection backgrounds. Split panes crop the same image using window coordinates. Editors, timers, images, and highlighters are cleaned up when disabled or disposed.

`AsciiTree` rasterizes rotating geometry into a fixed character grid with depth testing. `SnowField` manages particles; `SnowGround` supplies the shared landing surface. `ChristmasSettings` stores preferences, and `ChristmasStartupActivity` handles opt-in project startup.

The painting-order API is experimental in the SDK. Compatibility is deliberately bounded and checked before extending support to new platform branches.

## Contributing

Bug reports and small pull requests are welcome. Include the IDE/build, OS, split layout, settings, and reproduction steps. Avoid attaching source code or project screenshots that you do not want to share publicly.

## License

MIT © 2026 Austin Wheeler. See [LICENSE](LICENSE).
