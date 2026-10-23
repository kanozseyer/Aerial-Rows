# Research Index / Provenance

This index records the completed Projectivy and Couchy/Aerial-Rows research so future contributors know what was already closed and what not to redo.

## Aerial-Rows source baseline

- repository `kanozseyer/Aerial-Rows`
- branch `aerialrows-development`
- research pin `3c1cfd56234f1d386f4553dd6c69523670500d70`
- tree `457f4ab1d08a8d545bdf5545777cd31f907ae24d`
- commit message `Wire row editor state into Sliding Rows host`
- branch reverified unchanged at C12 completion on 2026-10-23

## Couchy/Aerial-Rows comparison reports

All status: COMPLETE + REVIEWED.

| Group | Scope | Final report SHA-256 |
|---|---|---|
| C1 | Build, manifest, lifecycle, architectural skeleton | `c85c2188906a849854865fc549e1f62c697899d7a648d05fb3f1fd6954e88cea` |
| C2 | Home rows, focus, rail, Compose rendering | `2c9732f438cdfc075239ba4c06754821d09846e4d706fbf7dc181dc480e0e381` |
| C3 | Apps, categories, card/context actions | `c756b965caa99ac016eb312a9828be05cfcd6618d666c5b41b1ca333c6761f03` |
| C4 | Android TV Channels + Watch Next | `935945ec232fe56e6e64e2112eb19179a3297e6194594234d239b9cde5ebd297` |
| C5 | Wallpaper + Aerial architecture | `e7603080accb1bdb90cf2672048f647276671649d3cfc61c4712130950bd4b78` |
| C6 | Settings architecture | `0bf49b7fed0fa4fee3abe2c86f133c3f62fb1eba708821c4b2cebf12cba37b18` |
| C7 | Status, media, notifications | `3947a7627f2fed66747704557066a7c81894f94399f12116370342663c94be3c` |
| C8 | Physical TV inputs/device integration | `1b5d4ac78301cc6a635d9554cdde44ae61e607fc8857044c391779ed18f5019b` |
| C9 | HOME, startup, remote, Accessibility, idle | `1234bd850995ccecefa08d8c78316de1da403f439e2f0e898f3b70540141be2c` |
| C10 | Persistence, backup, themes, icons, shortcuts, parental | `e08ee8ae66118f595cba576834cb3b95f70827508729395a4d2a05b31a32be54` |
| C11 | Performance and reliability | `20c18dd880f8ea15ce0ffa92aa08c9c5fcc38bb686897e9eb6b46342e14dc5f1` |
| C12 | Final Couchy ↔ Projectivy synthesis | `c0b2ccf993f1333426aae1cd55507b0f31cf8955c1243c4dac13850c22a5a68a` |

The detailed findings are consolidated in `COUCHY_DETAILED_FINDINGS.md`; implementation order is in `C12_FINAL_BLUEPRINT.md`.

## Projectivy target provenance

- package `com.spocky.projengmenu`
- version 4.72 / code 97
- minSdk 23 / targetSdk 37
- 7395 classes observed
- 1171 resource entries observed
- 3 DEX files
- native `libptutils.so` for common four ABIs
- MainActivity `com.spocky.projengmenu.ui.home.MainActivity`
- APK SHA256 `a312935837b7cf323880455c322f4c98060e3e37e8c6187963fb2c4764534aae`
- certificate SHA256 `f6697bf4082ee97511e4de07863193884a015b7ab5860430321bda1042b0aadd`

## Original Projectivy reports and hashes

| File | SHA-256 |
|---|---|
| group01.md | `8776c66d67db00866b28c25c7b43b48daf4076c174709df88bd7f0a1c3df3f7d` |
| group01_postclosure_audit.md | `9fc4b5dea909163b704bb88c678059fdb88807062cc00fd64f32446e43e08770` |
| group02.md | `82ff747a7ddd4d9e14293cd8459685e1bc41e16f1c3a24ca07234580a3e47b55` |
| group02_postclosure_audit.md | `61cbbc67441bed68d7d837fc1f43d1f3ca22ae8eeb9b7c860ae9a7bf853cd373` |
| group03.md | `d3beda827648c7612494e1e97c5898255af840efdab91b71fbc352ddfc50afc9` |
| group03_postclosure_audit.md | `b7bbc6d739a90bc64f3c52f8d56551d670ea9a126d8769726fe02734923339f3` |
| group04.md | `1ef505a55393f1952896cdd6c11e398df3e8e380593360662ad1617a71377de3` |
| group04_postclosure_audit.md | `25e6fcdb32775249e577abe8b55108ae0521647bde50225e2bb170b56f86aa9d` |
| group05.md | `20e6be9fd02df485749cac62ae4b9826177cf5997b370b02206cc6cab6e5113f` |
| group05_postclosure_audit.md | `eee0869a2988ab51be75410059dec13f1a7d7fa5bc0a2c4ba3e2b953be96734a` |
| group06.md | `a227c5e9a123244f2383b45b30337692457562891deba97194bb5dc2edacc5fd` |
| group06_postclosure_audit.md | `cc6046f4661d9ddeb468103cecb161433c427e55665d1b9c71efb1f6122afed2` |
| group07.md | `2bbd244f3113dbd3854ce3ef1d555027d5d00eda1cd23080acbc4153c74eba5d` |
| group07_postclosure_audit.md | `1c10815c1f2cc0de9cc26395ff902d488a017ab1d5cb1da6d19f928e5b7cbdc1` |
| group08.md | `668b12eba61d08be0d7e74904b3e60c6ae035f37c775db152e03747bc983f98e` |
| group08_postclosure_audit_v2.md | `0ba786c46fd5e6aa19f1266e7b434d2739bb73b4e8e74ff6953aa05007d8a3b3` |
| group09.md | `a5b966da60c9a9d6940e0d4669176be500681c75764c4999d28e9bdf0eb74aa4` |
| group09_postclosure_audit_v3.md | `81fee3e161b399cd24836408bfb508473402040e70deef4604ab983ac4aec07d` |
| group10.md | `bbdd9ea9811e69e96e1e78b725cac9b7cdc75fee1793c02a2829a291d68c8a3c` |
| group10_postclosure_audit_v2.md | `d75640cc4a23d2a7d392ee3322a50d59c942fad9c09094b4e38a58c130d2c126` |
| group11.md | `4f9c9bd36edba833c20b7964a12aae1182396ebde85e74aceca3da7396c4caca` |
| group11_postclosure_audit_v2.md | `a875f79d450182038e6a0763ac320a870acb016da456a757d1e73b72b79890e1` |
| group12.md | `6f066515287480a1666edd4a73b49be233d57592d267049a78dc60561f8bf8b6` |
| group12_postclosure_audit_v2.md | `55b146b0bc63bbd0436039be1774c88438b88e97cd0c4d41f86f213c5ea0753c` |

Use the post-closure audits to correct/refine the original report, especially Groups 8–12.

## Important corrected Projectivy reports

- Group 8 v2 — TV inputs/display profiles: `0ba786c46fd5e6aa19f1266e7b434d2739bb73b4e8e74ff6953aa05007d8a3b3`
- Group 9 v3 — HOME/startup/remotes/Accessibility/idle: `81fee3e161b399cd24836408bfb508473402040e70deef4604ab983ac4aec07d`
- Group 10 v2 — persistence/backups/themes/icons/shortcuts/parental: `d75640cc4a23d2a7d392ee3322a50d59c942fad9c09094b4e38a58c130d2c126`
- Group 11 v2 — obfuscated/native closure: `a875f79d450182038e6a0763ac320a870acb016da456a757d1e73b72b79890e1`
- Group 12 v2 — final reconstruction plan: `55b146b0bc63bbd0436039be1774c88438b88e97cd0c4d41f86f213c5ea0753c`

## Tool/process provenance

Toolchain used for Projectivy analysis:

- JADX headless MCP 0.7.1 all-in-one
- REA 6.7.0
- Java 21
- Node 22.20.0
- persistent JADX MCP daemon
- complete class inventory: 7395/7395 unique
- separate complete APK resource inventory: 1171 entries

Historical analysis paths:

- `/mnt/data/rea-portable`
- `/mnt/data/rea`
- `/mnt/data/launcherproject_work`

These paths are historical only; do not assume they exist in a fresh environment.

Final audit process contract v18 SHA:

`052e739b2bb985c6cccc6693ee474d89439623c0f1cf181e8257e8225eb203ef`

Final process-tool bundle SHA:

`251521a123ba52f3b497a6743448b77f17ead8b388a3727f1af4907f6bfd4b12`

## Closure standard used

Every final audit closed or explicitly marked not-applicable across:

1. entry points
2. base classes/inheritance
3. all DEX files
4. obfuscated collaborators
5. models/data flow
6. persistence
7. resources/XML
8. Android framework APIs
9. callbacks/observers
10. launch/actions/lifecycle
11. native/JNI

The research programme is complete enough to implement. A future agent should not rerun these groups simply because it lacks the original chat; use this handoff instead.
