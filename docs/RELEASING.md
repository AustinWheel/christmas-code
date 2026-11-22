# Releasing Christmas Code

## Build and verify

1. Run `./gradlew test sceneCheck buildPlugin` with JDK 21+ against the pinned baseline.
2. Run the **Compatibility** GitHub Actions workflow. Inspect every product's report, including warnings.
3. Complete the visual checks in `COMPATIBILITY.md` using the ZIP installed from disk in clean IDE profiles.
4. Update the version in `build.gradle.kts`, `CHANGELOG.md`, and the descriptor change notes.
5. Verify the repository license, artwork, description, source/support links, and screenshots.

## Signing

The build supports JetBrains' `signPlugin` and `verifyPluginSignature` tasks. Supply `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, and (when needed) `PRIVATE_KEY_PASSWORD` through environment variables. Never commit a private key or token. `PUBLISH_TOKEN` is used only for later Marketplace publishing.

```sh
./gradlew signPlugin verifyPluginSignature
```

On the maintainer machine, `./scripts/sign-local.sh` loads signing material from `~/.config/christmas-code/signing/` (override with `CHRISTMAS_SIGNING_DIR`) and runs both tasks without printing secrets. Back up that directory securely.

Keep the signing key outside the repository with restricted file permissions and a backup. A self-signed author certificate does not by itself remove all IDE trust prompts; Marketplace adds its own signature. See https://plugins.jetbrains.com/docs/intellij/plugin-signing.html.

## GitHub

Publish the tested ZIP, changelog, and checksums as a GitHub Release. Clearly label a pre-release if visual testing or Marketplace approval remains pending. Source publication alone does not publish to Marketplace.

## First Marketplace submission

The first release must be uploaded manually at https://plugins.jetbrains.com. Sign in, create/select the vendor profile, and accept the Developer Agreement yourself. Prepare the upload using `MARKETPLACE.md`, select the repository license, attach accurate screenshots, and upload the signed distribution from `build/distributions/`. Use a beta channel or hidden listing if appropriate. JetBrains reviews new plugins and updates before public availability.

Subsequent versions can use `./gradlew publishPlugin` with `PUBLISH_TOKEN`. The build does not auto-publish on pushes or tags.

References:
- https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html
- https://plugins.jetbrains.com/docs/marketplace/uploading-a-new-plugin.html
- https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html
