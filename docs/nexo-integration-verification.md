# Nexo integration verification

Date: 2026-10-18. Scope: Tier 2 optional text integration, Java presentation and Bedrock inventory-title fallback. Local implementation only; no production changes or deployment.

## Contract and causal evidence

The user requires server-independent Nexo support with no StarForged association. Commit `822c7348f236e2a6c3b22f97c973204e33ddd55f` introduced the first Nexo support: a fixed StarForged rank table, codepoints, gradients and `nexo:default`. There was no generic Nexo integration before that commit. Source trace: `PlayerDisplayNameService` -> `DisplayTextParser` -> the fixed `NexoGlyphText` substitutions; `Menu.open` separately adapted the title for Bedrock. Missing-plugin and foreign-font reproductions both failed against that implementation.

PROVEN: the old adapter fabricated recognized rank glyphs without Nexo and relabeled unrelated fonts' private-use characters. PROVEN: the replacement links only the public Nexo glyph/shift resolvers and looks up current glyph definitions for readable labels. PROVEN: shared template parser instances observe provider changes; no parsed-text cache is used while Nexo is available. UNKNOWN: appearance with the operator's actual Nexo pack and Java/Bedrock clients; requires a server/client smoke check.

## Authorities and change radius

Nexo owns glyph IDs, fonts, unicode assignment, bitmap arguments, tinting, spacing and reloads. `NexoTextIntegration` owns optional-plugin lifecycle; `DisplayTextParser` owns trusted display parsing; `NexoGlyphText` only transforms loaded glyph characters into literal readable labels. `BedrockPlayerDetector` remains the client identity owner. Menus, messages and broadcasts share the same parser; no additional PlaceholderAPI, command, economy, permission or inventory transaction capability is added.

The public Nexo API dependency is compile-only, non-transitive, pinned to `1.29.1`; its public spacing-provider type uses `creative-api:1.15.3`, matching Nexo's published POM, also compile-only and non-transitive. Neither is bundled. Spacing removal uses the actual current `Shift.fontProvider.advances()` characters and `ShiftTag.FONT`, never all private-use characters in a presumed font. Only the optional provider imports Nexo types. Its lifecycle adapter has a test that forbids loading any Nexo class when Nexo is absent. Dynamic tags bypass the bounded 512-entry component cache; readable conversion scans the loaded in-memory glyph definitions once per conversion and retains only characters present in that component. No I/O, scheduler, worker, or durable writer is introduced. Current consumers render on their existing platform thread.

## Compatibility

Unchanged durable contract: no configuration keys/defaults, persisted IDs, database schema, readers/writers, APIs/events or queued payloads change. Existing glyph tags and raw components are interpreted using the installed Nexo definitions. The readable fallback preserves unregistered codepoints; unknown glyph IDs follow Nexo's own resolver behavior. The removed StarForged-specific substitutions are intentionally unsupported when Nexo is absent, as requested. No migration is required. Reverting this change restores the earlier renderer without discarding any newly written data because this change writes none. Existing config/default and storage suites remain part of the full test gate; application/platform downgrades are not claimed.

## Primary-source comparison

Inspected 2026-10-18:

| Source | Exact surface | Decision |
| --- | --- | --- |
| [Nexo glyph documentation](https://docs.nexomc.com/configuration/glyphs) and public `com.nexomc:nexo:1.29.1` artifact | `GlyphTag.RESOLVER`, `ShiftTag.RESOLVER`, `NexoPlugin.instance().fontManager().glyphs()`, `Glyph.id/font/unicodes`, `ShiftTag.FONT`, `Shift.fontProvider.advances()` | Adopt direct typed API. Preserve arguments and font identity. Avoid fixed codepoints, font guesses, and the full Nexo serializer's additional PAPI capabilities. |
| [NexoProxy GlyphListener](https://github.com/Nexo-MC/NexoProxy/blob/master/src/main/kotlin/com/nexomc/nexoproxy/glyphs/GlyphListener.kt), master at inspection | Runtime glyph records carry ID, font and unicode lists; `GlyphStore.glyphs` | Adapt runtime-definition ownership and font-aware identity. Reject copying its proxy transport into this backend-only text adapter. |
| [Oraxen GlyphTag](https://github.com/oraxen/oraxen/blob/master/src/main/java/io/th0rgal/oraxen/glyphs/GlyphTag.java), master at inspection | Native glyph resolver delegates to the registered glyph's component and arguments | Adopt provider-owned components as the pattern; reject adding Oraxen support or assuming its API/format is Nexo's. |

## Security and verification record

Assets: trustworthy display text and server availability. Inputs: operator-authored templates, existing trusted prefix rendering, escaped player text and external placeholder output. Boundaries: optional plugin/classloader, trusted markup vs literal component text, Java font vs Bedrock title. No authoritative state transition changes. Applicable rules: MCSEC-TEXT-001/002, MCSEC-DOS-001/002, MCSEC-BEDROCK-001, MCSEC-ARCH-001/002 and MCSEC-SUPPLY-002. Tests exercise arbitrary IDs/fonts, reload, disable/absence, supplementary Unicode, foreign-font collisions, unknown glyphs, literal tags/actions, formatting preservation, spacing and existing display-name behavior.

The readable fallback edits the component tree; it never serializes and reparses untrusted text. Shared MiniMessage disables virtual formatting wrappers so serializing a transformed Bedrock title cannot restore the original Java glyphs. Existing placeholder sanitization is retained. The optional resolver adds only glyph/spacing tags, not Nexo's PAPI resolver.

Final local verification: `gradlew.bat clean build --warning-mode all --console=plain` succeeded after the final spacing correction: 208 tests, zero failures, errors or skips. The full build included both JARs and `verifySpigotUploadJar`. ZIP inspection confirmed each artifact contains the provider class and neither bundles any `com/nexomc/` or `team/unnamed/creative/` classes. Focused red/green checks reproduced both the original server-specific substitutions and the shared-font spacing edge case before their fixes.

Manual final-diff review covered optional linkage, public API signatures, source-to-sink text trust, font inheritance/collisions, glyph argument forwarding, reload behavior, cache boundaries, lifecycle cleanup and unchanged durable contracts. `DisplayRewardTagResolver` retains its existing 750 ms rendered-prefix cache; parser caching is bypassed while Nexo is active, and open menus update when reopened. No inventory click/session ownership was changed.

Scanner output: `build/nexo-investigation/security-before.json` has 82 repository candidates; `security-after.json` has 83. Eleven final candidates are in changed files. The new FILE-001 hit is MiniMessage `TagResolver.resolve`, not a filesystem operation. TEXT-001 rendering hits are the existing trusted-display/config template boundaries with escaped player/placeholder insertion, covered by the focused literal/action tests. SCRIPT-001 hits are existing constant-name optional-plugin/API reflection; Menu FILE-001 hits are existing player-name service calls, not path traversal. These were manually classified in `build/nexo-investigation/changed-findings-review.json`. Repository-wide matches outside this diff are not certified or repaired by this focused change. A full production security/release PASS remains BLOCKED on runtime/client evidence and a separate full audit; the local implementation and automated verification are complete.

## Remaining runtime check

On an authorized local/staging server, use a non-StarForged glyph with a custom font in a configured menu title and an existing admin-authored prefix. Verify Java appearance, bitmap/colorable options and shift; reopen after a Nexo glyph reload; verify a Bedrock title stays readable. Verify ordinary messages still render with Nexo removed. No live server installation or restart is authorized by this task.
