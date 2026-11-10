# Hafercraft build & deploy notes (continuation for the next machine)

This file travels with the repo. Read `AGENTS.md` first (language policy,
branch layout, landmines). This note is the operational continuation of the
2026-11-09/10 session.

## State (as of 2026-11-10, ~11:40 — DEPLOYED)

Deployed to the live server (homelab, via Tailscale `homelab-ts`):
- Built in docker (JDK 21): clean compile, tests + `verifySpigotUploadJar` pass.
  Jar `MayorSystem-1.1.10-hafercraft.1.jar`, sha256
  `44feabb0996cd1f98301bcfbd4d434a932163388cd0057b49992aa5e05a9a601`.
- Manifest updated (MayorSystem row now the fork build); old 1.1.9-beta jar
  retired to `plugins-retired/` and committed in the parent repo (`af7437d`).
- Resurrected `effects:` catalog deleted from live config (backup at
  `config.yml.bak-pre-fork`); new jar booted, sync added the new
  messages/gui keys, `effects:` stayed gone after the sync rewrite.
- Verified on live: `[MayorSystem] Ready.` (1.1.10-hafercraft.1), catalog
  8 boons + 8 curses enabled, minima 1/1.

STILL OPEN:
1. Smoke test as a candidate (needs an in-game player): apply with 3 boons →
   blocked by `perk_section_minimum`; apply 1 boon + 1 curse + 1 free →
   accepted. NOTE: the term-0 election is ALREADY OPEN (announced; office
   starts 2026-11-11 18:47 — the older "~Nov 22" estimate was stale). There
   are no candidates yet, so the first real applicant doubles as the smoke
   test. No restart needed after deploy; state is in sqlite.
2. Gamerule fallbacks: verify `doInsomnia`, `doWeatherCycle`, `randomTickSpeed`
   exist on MC 26.3 after first term start (Nov 11 18:47+); swap any missing
   curse (`cursed_water_cut`/`cursed_eternal_monday` are on the disabled
   bench).
3. `server/server.properties` in the parent repo is dirty — NOT from this
   session, leave to the admin.

## Deploy history (how it was done, for the next time)

Build (this repo, docker per AGENTS.md):
```bash
docker run --rm -v "$PWD":/src -w /src -e GRADLE_USER_HOME=/src/.gradle \
  eclipse-temurin:21-jdk ./gradlew --no-daemon --console=plain build
```
- `.gradle/` in the repo dir is the pinned GRADLE_USER_HOME cache (gitignored);
  first run after a machine move re-downloads deps (~2-4 min).
- On a small host: never run docker builds next to the MC server continuously —
  a 2026-11-10 session got killed by server restarts mid-build twice. Build
  from the laptop (schlepptop), deploy over Tailscale.

Deploy path (works from away-from-home, via Tailscale):
1. `scp` jar → `homelab:/home/gregor/minecraft/pending/`, update
   `docs/plugin-manifest.md` row (version + sha256), scp back.
2. `scripts/deploy-mayorsystem-1.1.10.sh` (committed in the parent repo)
   does: announce (60 s) → flush → `systemctl stop` → delete resurrected
   `effects:` block (pattern-verified, backup made) → retire old jar →
   install new jar → boot → verification bundle.
3. Needs sudo interactively (`sudo` over SSH has no NOPASSWD), so run:
   `ssh -t homelab-ts 'sudo bash /home/gregor/minecraft/scripts/deploy-mayorsystem-1.1.10.sh'`
4. Commit the parent repo (manifest, live configs, retired jar, script).

