## Language convention

All code in this project is written in English: class/interface/enum names, properties,
parameters, function names, and enum constants. This applies to every layer (domain, data,
ui, core) — see `docs/ARQUITECTURA_Y_DISENO.md` § "Convención de idioma" for the rationale
and the full before/after naming reference from the HE-04..HE-08 refactor.

Exceptions:
- **Comments and KDoc** may stay in Spanish (the team's working language). Do not translate
  existing Spanish comments as a side effect of unrelated changes.
- **User-facing strings** are the one place Spanish is the primary language — see the next
  section; they never appear as literals in Kotlin.

When adding new code: name it in English from the start. When editing an existing Spanish
identifier you encounter, rename it to English as part of that change.

## User-facing strings (Spanish + English, never hardcoded)

Every text the field technician can see — `Text()`, `placeholder`, `contentDescription`,
button labels, snackbars, notifications, dialog titles — comes from Android string
resources, in **both languages**:

- `app/src/main/res/values/strings.xml` — **Spanish, the default locale**
  (`res/resources.properties` declares `unqualifiedResLocale=es`).
- `app/src/main/res/values-en/strings.xml` — **English**.

Rules:
- **Never write a user-visible literal in a Composable, ViewModel or component.** Add a key
  to `values/strings.xml` *and* its translation to `values-en/strings.xml` in the same
  change. A key missing from either file is a bug (lint reports `MissingTranslation`).
- Key names are English snake_case prefixed by screen/component:
  `inspection_save`, `sync_offline_title`, `alert_status_closed`, `common_cancel`.
  Reuse `common_*` keys for shared words (Volver, Cancelar, Guardando…).
- In Compose resolve with `stringResource(R.string.x, args…)`; for counts use
  `<plurals>` + `pluralStringResource(R.plurals.x, count, count)`. Outside Compose use
  `context.getString(...)`. ViewModels expose `@StringRes Int` ids (or domain data), never
  already-resolved text, so the UI follows the device language.
- Use positional placeholders (`%1$s`, `%2$d`) so translations can reorder them; write a
  literal percent as `%%`.
- **Domain enums carry no display text.** Map them to `@StringRes` ids in
  `ui/components/Labels.kt` (`AlertStatus.labelRes()`, `EventCategory.labelRes()`, …) and
  add a `labelRes()` branch there when you add an enum constant.
- Proper nouns and symbols that never change by language (`AGATHA`, site names, `—`,
  zoom glyphs) still live in `values/strings.xml`, marked `translatable="false"`, and are
  not duplicated in `values-en`.
- Locale-dependent formats (dates) go through a translatable pattern resource and the
  Compose locale (`LocalConfiguration.current.locales[0]`), never a hardcoded `Locale`.
- **Sample data is translated too.** The mock content in `data/repository/` (alert
  descriptions, history entries, pending records) never holds a literal: free text is a
  `LocalizedText.Resource(R.string.sample_*, args)` and structured values are enums
  (`SensorType`, `AccelerationLevel`) or numbers (`minutesAgo`, `pointNumber`) that the UI
  formats. Add every `sample_*` key to both string files.
- Domain models never store display text. Free text the domain cannot avoid is a
  `LocalizedText` (`Plain` for text a person wrote, `Resource` for app text), resolved in
  Compose with `.resolve()`; relative times are minutes rendered with `relativeTimeAgo()` /
  `shortDuration()` (`ui/components/Texts.kt`). Text separators such as `·` go through
  `common_dot_separated`, not string templates.
- To check English on a device/emulator:
  `adb shell cmd locale set-app-locales com.hivend.agatha.debug --locales en-US`
  (empty `--locales ""` resets to the system language).

## Git workflow (commit per task, push per finished job)

- **After each task** (one coherent step of a larger request): verify it compiles and works,
  then **commit** it. Minimum check before every commit:
  `./gradlew assembleDebug lintDebug testDebugUnitTest`. If the change touches UI, also
  install it on an Android 13+ emulator/device and walk the affected flow — a green build
  proves it compiles, not that it renders or navigates correctly. Never commit a broken
  build.
- One commit per task, with a message that says what changed and why (English, imperative
  mood, e.g. "Move inspection form strings to resources").
- **When the whole larger job is finished** (every task of the user's request is committed
  and verified), **push** the branch: `git push -u origin <branch>`. Do not push half-done
  work in the middle of a job unless the user asks for it.
- Work on `develop` or a `feature/*` branch off it; `master` only receives merges via PR.
- Never commit `local.properties`, `google-services.json` or signing keys (`*.jks`).

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
