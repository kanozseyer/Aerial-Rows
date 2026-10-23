from pathlib import Path

path = Path('app/src/main/java/com/conreo/couchytv/ui/SettingsSheet.kt')
text = path.read_text()
old = '''    val targetIndex = rememberedFocus
        ?.let(appSettingIds::indexOf)
        ?.takeIf { it >= 0 }
        ?: 0

    LaunchedEffect(appSettingIds, rememberedFocus) {
        if (appSettingIds.isEmpty()) return@LaunchedEffect
        listState.scrollToItem(targetIndex)
        withFrameNanos { }
        val targetId = appSettingIds[targetIndex]
'''
new = '''    // Capture the remembered target only when this Apps screen/app-set enters
    // composition. Normal row-to-row focus updates must not re-run scrollToItem,
    // otherwise D-pad navigation snaps each newly focused row back to the top.
    val restoreTargetId = remember(appSettingIds) {
        rememberedFocus?.takeIf { it in requesters } ?: appSettingIds.firstOrNull()
    }

    LaunchedEffect(appSettingIds, restoreTargetId) {
        val targetId = restoreTargetId ?: return@LaunchedEffect
        val targetIndex = appSettingIds.indexOf(targetId).takeIf { it >= 0 } ?: return@LaunchedEffect
        listState.scrollToItem(targetIndex)
        withFrameNanos { }
'''
if old not in text:
    raise SystemExit('Apps restore anchor missing')
path.write_text(text.replace(old, new, 1))
