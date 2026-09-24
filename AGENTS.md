# AGENTS.md

Guidance for AI coding agents (OpenCode, Claude Code, etc.) working in this repository.

## Project Overview

WebShell is an Android WebView wrapper kit: a minimal Kotlin app that hosts a plain HTML/CSS/JS
web app in a full-screen WebView with a JavaScript-to-native bridge.

- Language/stack: Kotlin + AndroidX AppCompat/Core-KTX; web layer is plain HTML/CSS/JS in `assets/www/`
- Package/module: `com.yourapp` (intentional placeholder — forks stamp their own id here)
- Toolchain: minSdk 26, compile/target SDK 35, JVM 11, AGP 8.7.3 / Kotlin 2.0.21 via `gradle/libs.versions.toml`
- Author/maintainer: yungsamd17 (https://github.com/yungsamd17)

## Build & Verify

```bash
./gradlew assembleDebug   # build debug APK (the only CI gate)
./gradlew lintDebug       # Android lint (available via AGP, not run by CI)
```

- There is no unit or instrumented test suite yet — verification is `assembleDebug` plus manual
  testing of the bridge methods on a device/emulator.
- CI (`.github/workflows/ci.yml`) runs on every push to `main`, PRs, and manual dispatch:
  validates the Gradle wrapper, sets up JDK 17 (temurin) + the Android SDK, runs
  `./gradlew assembleDebug --stacktrace`, and uploads `app-debug.apk` as the `app-debug-apk` artifact.
- Local sandboxes often lack the Android SDK/JDK — if builds can't run locally, rely on careful
  code review and let CI verify.

## Architecture

```
app/src/main/
  kotlin/com/yourapp/
    MainActivity.kt       # single Activity hosting a full-screen, edge-to-edge WebView
    NativeBridge.kt       # @JavascriptInterface methods callable from JS as window.Native.*
  assets/www/
    index.html            # sample entry page
    bridge.js             # JS wrapper around the native bridge
    app.js / style.css    # sample web app code
  res/                    # themes.xml, strings.xml, adaptive launcher icons
  AndroidManifest.xml     # permissions + activity config (see gotchas)
docs/                     # getting-started, bridge-api, java-reference, project-structure, gotchas-and-tips
gradle/libs.versions.toml # version catalog (AGP/Kotlin/androidx versions)
```

Key patterns:

- Bridge contract is two-sided: every new `@JavascriptInterface` method in `NativeBridge.kt`
  must get a matching wrapper in `assets/www/bridge.js`. Document it in `docs/bridge-api.md`.
- `@JavascriptInterface` methods run on a background thread — any UI work must be wrapped in
  `activity.runOnUiThread { ... }`; pushing data back to JS goes through
  `activity.webView.post { webView.evaluateJavascript(...) }`.
- The web app lives entirely in `app/src/main/assets/www/` and loads via
  `file:///android_asset/www/index.html`; no server, no build step for the web side.
- Version bumps happen in `app/build.gradle.kts` (`versionCode`/`versionName`) plus a
  Keep-a-Changelog style entry in `CHANGELOG.md`.

## Commit Messages

Format: `type(scope): short imperative summary` — lowercase after type, no trailing period.
Keep commits atomic — one logical change per commit.

| Type | Use for |
|---|---|
| `feat` | new user-facing feature |
| `fix` | bug fix |
| `refactor` | code change that neither fixes nor adds behavior |
| `style` | formatting/UI polish without logic change |
| `test` | adding or fixing tests |
| `docs` | documentation only |
| `chore` | build, deps, CI, tooling |
| `release` | version bump / release tagging |

Scope is a short area name for this project (e.g. `app`, `bridge`, `www`, `docs`, `ci`).
Use plain `type:` only when a change genuinely spans everything (rare).

Examples:

```
feat(bridge): add clipboard read method
fix(app): keep keyboard from covering input fields
chore(ci): cache gradle wrapper in workflow
docs(readme): document commit message types
```

- Never add a `Co-authored-by` / `Signed-off-by` trailer for the same identity
  that authors the commit — a self co-author is a redundant duplicate. Only
  credit a genuinely different human co-author, and only when asked. No AI
  co-author trailers in commits either; AI attribution stays only in the PR body.
- Keep the body free of trailers entirely unless explicitly asked for one.
  When squash-merging via `gh pr merge --squash`, pass an explicit
  `--subject` and an empty `--body ""` so GitHub doesn't re-inject branch
  trailers or auto-credit the branch author as a co-author.

## Agent Guardrails

- Never commit or push directly to `main`; all changes land through pull requests.
- Never open a PR unless the developer explicitly asks for it.
- One concern per change. If the description says "also", split it into another branch/PR.
- Do not commit secrets, keystores, local-only files (`local.properties`, `.gradle/`,
  `build/`) or APK/AAB outputs (`*.apk`, `*.aab` are gitignored).
- When watching CI/bot feedback on your PRs: poll checks and comments newer than the last push,
  verify each bot finding against the source before "fixing" it, dismiss false positives with a
  written reason, and stop when checks are green on the latest commit.

## Pull Requests

All changes land on `main` through pull requests.

1. Create a branch off `main`: `<type>/<short-description>` (e.g. `feat/clipboard-bridge-method`).
2. Commit there using the format from **Commit Messages**; keep commits atomic.
3. Push the branch and open a PR against `main`.

PR rules:

- One feature/fix per PR — small and focused beats large and thorough.
- Title follows the commit message format: `type(scope): short imperative summary` —
  it becomes the squash-merge commit message.
- Body stays concise, following the PR template: what changed and why, bullet list of touched
  areas, evidence if applicable, testing checklist (tick before merge).
- End the body with an AI attribution line stating exactly which model and agent made the changes,
  in this exact format:

  ```
  Built with {model} in the {agent} harness.
  ```

  Example: `Built with ox-alpha in the OpenCode harness.`

- Do **not** put AI attribution in GitHub Release notes — releases stay clean.
- CI must pass before merging.

## Gotchas

- `android:hardwareAccelerated="true"` must stay set — WebView rendering breaks without it.
- `allowFileAccess = false` in `MainActivity.kt` is intentional hardening; asset loading uses
  `file:///android_asset` which is unaffected. Do not flip it back on.
- `configChanges` on the Activity prevents recreation on rotation/keyboard so the web app never
  reloads; removing it changes core behavior. `windowSoftInputMode="adjustResize"` keeps inputs
  visible above the keyboard.
- Edge-to-edge is enabled (`WindowCompat.setDecorFitsSystemWindows(window, false)`) — content
  should pad with `env(safe-area-inset-*)` in CSS.
- `com.yourapp` namespace/applicationId is a deliberate template placeholder; don't "fix" it.
- Keep the Gradle wrapper executable (`gradlew`); CI validates it on every build.
