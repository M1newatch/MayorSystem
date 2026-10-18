# Integrations

All integrations are optional unless your server workflow requires them. MayorSystem auto-detects supported plugins and addons when they are installed, and most are drop-in: install the integration plugin, restart or reload, then use `/mayor admin health` and the relevant admin menus to finish setup.

If an optional plugin is missing, MayorSystem uses disabled/no-op behavior for that integration instead of making the whole plugin unusable.

## Economy
- Vault plus a Vault-compatible economy plugin powers candidate apply costs.

## LuckPerms
- Used for mayor username group rewards and display reward rank mode.
- See [LuckPerms setup](LUCKPERMS_SETUP.md).

## DeluxeTags
- Used for display reward tag mode.
- MayorSystem grants the configured DeluxeTags permission through LuckPerms.

## PlaceholderAPI
- Used in messages, broadcasts, and the MayorSystem placeholder expansion.
- See [placeholders](placeholders.md).

## Nexo
- Optional glyph support uses Nexo's own glyph and shift resolvers in display names, menu titles/items, configured messages, and broadcasts. Use `<glyph:your_glyph_id>` and `<shift:-8>` with the glyph IDs defined on your server. Nexo handles custom fonts, bitmap arguments, and glyph colors; MayorSystem includes no server-specific rank table or resource pack.
- Glyph definitions are read from the installed Nexo plugin. Reloading Nexo affects newly parsed text without restarting MayorSystem. Existing display-reward tag prefixes have a 750 ms cache, and open menus refresh when reopened.
- Plain-text names and Bedrock inventory titles use readable glyph IDs, for example `[town badge]` for `town_badge`, and omit Nexo spacing characters. This does not convert or install a Bedrock resource pack.
- Nexo is a soft dependency. When it is absent or disabled, MayorSystem preserves unresolved tags instead of inventing glyphs. Ordinary player input and external PlaceholderAPI expansions retain their existing escaping; enabling glyph support does not enable Nexo's PlaceholderAPI resolver.

## NPCs
- Citizens or FancyNpcs can provide the Mayor NPC backend.
- Configure with `npc.mayor.provider` or leave on `auto`.

## Holograms
- DecentHolograms or FancyHolograms can provide the leaderboard hologram backend.
- Configure with `hologram.leaderboard.provider` or leave on `auto`.

## External Perk Sources
- SystemSellAddon can seed economy perks.
- SystemSkyblockStyleAddon can seed skyblock-style perks.
- New addons should use the [MayorSystem addon API](addons/README.md).
