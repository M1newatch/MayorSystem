# AGENTS.md — MayorSystem (Hafercraft fork)

House rules for any agent or human working in this repository. English is the
base language of everything.

## Language policy (non-negotiable)

| Context | Language |
|---|---|
| Conversation with the user, commits, branches, code, code comments, docs, config keys and config comments | English |
| Player-facing strings: perk `display_name` / `lore` values, `public.*` messages, election broadcast lines, any GUI text candidates/voters read | German |
| Admin-facing strings: `admin.*` messages, admin menu text, everything under `/mayor admin` | English |

Rules:
- Never translate config *keys*, code identifiers, MiniMessage tags, or
  `%placeholder%`s — only the human-readable values.
- German player text: short, plain, friendly; match the tone of the existing
  perk catalog. No `Du/Sie` mixing — use `du`.
- Shipped default files (`src/main/resources/*`) may contain English player
  text from upstream. That is migration debt: translate opportunistically when
  you touch a string anyway; never rewrite unrelated strings in a commit that
  is doing something else.

## Branch layout

- `main` = mirror of upstream `Louguerrier22/MayorSystem`. Do not carry
  Hafercraft-only changes here; keep it pullable from `upstream`.
- `hafercraft/26.3` = all fork work (ported cloud-paper fix, Hafercraft
  features, version bumps). Live-server jars build from this branch.
- Remote `origin` = `git@github.com:M1newatch/MayorSystem.git` (deploy key
  `~/.ssh/id_ed25519_GitHub`, pinned via `~/.ssh/config`).
  Add `upstream` → `https://github.com/Louguerrier22/MayorSystem` to rebase on
  upstream releases.

## Build

The host has no JDK and Gradle 8.14 does not run on the installed JRE (25).
Build in docker (JDK 17+ host for Gradle would also work if one appears):

```bash
docker run --rm \
  -v "$PWD":/src -w /src \
  -e GRADLE_USER_HOME=/src/.gradle \
  eclipse-temurin:21-jdk \
  ./gradlew --no-daemon build
```

- `build` runs tests + `verifySpigotUploadJar` and produces the deployable
  slim jar `build/libs/MayorSystem-<version>.jar` (runtime libraries are
  fetched via `plugin.yml` `libraries:`, not shaded). The `-all` shadow jar is
  optional/local only.
- The foojay toolchain resolver auto-provisions JDK 21 for compilation.
- Host JDK 25 (`/home/gregor/minecraft/jdk25`) is unusable for Gradle 8.14.

## Version scheme

`<upstream-base>-hafercraft.<n>`, e.g. `1.1.10-hafercraft.1`.
Bump for every deploy, never roll back. `plugin.yml` picks it up via
`${version}` expansion in `processResources`.

## Deploy & server ops (out of this repo, but relevant)

- `/home/gregor/minecraft/docs/plugin-manifest.md` is the source of truth for
  the live plugin: version, sha256, source URL. Update it with every deploy.
- Retire old jars in `/home/gregor/minecraft/plugins-retired/` (suffix naming
  like `MayorSystem-1.1.7.jar.26.3-broken`).
- Restart the live server only via
  `sudo bash /home/gregor/minecraft/scripts/apply-restart.sh` (announces,
  flushes, restarts systemd unit, verifies boot).
- Live plugin config lives in `/home/gregor/minecraft/server/plugins/MayorSystem/`
  (config.yml, gui.yml, messages.yml). It is tracked by the parent repo
  (`/home/gregor/minecraft`). Perk catalog edits belong there, mirrored into
  `docs/plugin-config-guide.md` when the docs are wrong after a change.

## Known landmines (from the 1.1.9 rebuild)

- **cloud-paper `2.0.0-beta.15` crashes Paper 26.3** (`NoSuchMethodException`
  on removed NMS accessors). Keep `2.0.1` or newer (currently: exactly 2.0.1
  in both `build.gradle.kts` and `plugin.yml` `libraries:`).
- Perk commands execute as **console**; dangerous roots (op, stop, lp, …) are
  hard-blocked by `BLOCKED_DANGEROUS_COMMAND_ROOTS` unless listed in
  `perks.command_execution.allow_roots`.
- `effect give @a …` commands are intercepted and become native infinite
  effects (journeable across joins/restarts). Targeted variants with a simple
  player-name selector (`effect give @a[name=X] …`) are intercepted the same
  way and tracked for that one player; unsupported selectors fall through to
  console (runs once, no cleanup). Any other command runs once at
  term start/ end. Negative potion effects are fully supported — they are a
  supported mechanism for "curse" perks.
- Election/perk data (sqlite `elections.db`) stores chosen perk *ids*, so
  catalogs can be restructured as long as ids survive. At term start,
  `computeAppliedPerkIds` quietly drops ids that are no longer in the enabled
  catalog — data won't crash, but prefer id stability anyway.
