# Hafercraft build & deploy notes (continuation for the next machine)

This file travels with the repo. Read `AGENTS.md` first (language policy,
branch layout, landmines). This note is the operational continuation of the
2026-11-09/10 session.

## State (as of 2026-11-10, after this push)

Done:
- Fork ported to `hafercraft/26.3`: cloud-paper 2.0.1 port commit, feature
  commit (mandatory section picks `pick_min_limit`, loot curse
  `drop_modifier`, `%mayor_name%`/`%mayor_uuid%` placeholders, catalog
  restructure), compile-error fix commit. Clean compile NOT yet verified —
  the two errors found so far are fixed; expect maybe 1-2 more.
- Catalog (8 boons + 8 curses enabled, minima 1/1) is written into BOTH:
  - shipped defaults: `src/main/resources/config.yml`
  - live server: `/home/gregor/minecraft/server/plugins/MayorSystem/config.yml`
  (Both validated YAML; ConfigDefaultsSync cannot resurrect the retired
  `effects` section because the defaults no longer contain it.)
- Messages/gui keys for the new validation flow are already in the bundled
  defaults (`public.perk_section_minimum` German, admin counterpart English,
  GUI `section_rules` German). They auto-merge into the live files on first
  boot of the new jar.

NOT done (the remaining checklist):
1. Build somewhere with enough CPU: see command below. Host JDK 25/jdk25 dir
   does NOT work with Gradle 8.14 — use the container recipe.
2. `sha256sum` the produced jar; update the MayorSystem row in
   `/home/gregor/minecraft/docs/plugin-manifest.md` (version 1.1.10-hafercraft.1,
   source = this fork, branch hafercraft/26.3, sha) — the manifest's fork
   section text is already written, only the table row + hash are missing.
3. Retire old jar: move `/home/gregor/minecraft/server/plugins/MayorSystem-1.1.9-beta.jar`
   → `/home/gregor/minecraft/plugins-retired/` (parent repo tracks jars there).
4. Copy `build/libs/MayorSystem-1.1.10-hafercraft.1.jar` into `/home/gregor/minecraft/server/plugins/`.
5. Restart via `sudo bash /home/gregor/minecraft/scripts/apply-restart.sh`
   (announces + flushes + restarts + prints verification tail).
6. Verify: `[MayorSystem] Ready.` in log, NO resurrected `effects:` section in
   `server/plugins/MayorSystem/config.yml`, new sync keys added under messages/gui,
   `/mayor admin perks` shows Segnungen + Flüche with the 8+8.
7. Smoke test as a candidate: apply with 3 boons → blocked by
   `perk_section_minimum`; apply 1 boon + 1 curse + 1 free → accepted.
   (Election window opens ~Nov 22, so no live rush.)
8. Parent repo (`/home/gregor/minecraft`, no remote) has local modifications:
   `.gitignore` (+plugins-src/), `docs/plugin-config-guide.md`,
   `docs/plugin-manifest.md`, live `config.yml`. `server/server.properties`
   also shows modified — NOT from this session, leave to the admin.
   Commit those when deploying.
9. Gamerule fallbacks: verify `doInsomnia`, `doWeatherCycle`, `randomTickSpeed`
   exist on MC 26.3 after first term start; swap any missing curse
   (`cursed_water_cut`/`cursed_eternal_monday` are on the disabled bench).

## Build command (docker, per AGENTS.md)

```bash
cd plugins-src/MayorSystem
docker run --rm -v "$PWD":/src -w /src -e GRADLE_USER_HOME=/src/.gradle \
  eclipse-temurin:21-jdk ./gradlew --no-daemon --console=plain build
```

- `build` = compile + tests + `verifySpigotUploadJar`; artifact
  `build/libs/MayorSystem-1.1.10-hafercraft.1.jar`.
- IMPORTANT on a small host: stop the MC server (or at least never run docker
  builds next to it continuously) — the session of 2026-11-10 got killed by
  server restarts mid-build twice.
- `.gradle/` in the repo dir is the pinned GRADLE_USER_HOME cache (gitignored);
  first run after a machine move re-downloads deps (~2-4 min).

## Interim risk while the OLD jar (1.1.9-beta) still serves

The live server currently runs the OLD jar with the NEW catalog. Consequences:
- Admin candidate/apply menus show Segnungen/Flüche names without enforcement;
  `pick_min_limit` keys are ignored by the old jar.
- If the old jar boots again (any restart before deploy), its bundled defaults
  may `syncMissingKeys`-re-add the retired `perks.sections.effects` catalog to
  the live config. If you see `effects:` back in the live file: delete that
  block before/after installing the new jar; the new jar's defaults prevent
  resurrection.
- No server restart is urgent: perks only apply at term start ( election
  opens ~Nov 22).
