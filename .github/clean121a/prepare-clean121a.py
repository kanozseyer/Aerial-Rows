#!/usr/bin/env python3
from pathlib import Path

engine = Path('.github/clean07/RedLightDiscoveryEngine.java')
text = engine.read_text()

anchor = '''    private static boolean sameProgram(ExistingProgram old, ContentValues v) {
        if (!same(old.title, v.getAsString("title"))) return false;
        if (!same(old.description, v.getAsString("description"))) return false;
        if (old.type != intValue(v, "type")) return false;
        if (!same(old.intentUri, v.getAsString("intent_uri"))) return false;
        if (old.weight != intValue(v, "weight")) return false;
        if (old.posterAspectRatio != intValue(v, "poster_art_aspect_ratio")) return false;
        // If artwork lookup is temporarily unavailable, preserve the existing poster rather
        // than treating the missing lookup as a request to clear artwork.
        if (v.containsKey("poster_art_uri")
                && !same(old.posterArtUri, v.getAsString("poster_art_uri"))) return false;
        return true;
    }
'''

replacement = '''    private static int clean121aMismatchLogs;

    private static boolean sameProgram(ExistingProgram old, ContentValues v) {
        StringBuilder mismatch = new StringBuilder();

        if (!same(old.title, v.getAsString("title"))) mismatch.append("title,");
        if (!same(old.description, v.getAsString("description"))) mismatch.append("description,");
        if (old.type != intValue(v, "type")) mismatch.append("type,");
        if (!same(old.intentUri, v.getAsString("intent_uri"))) mismatch.append("intent_uri,");
        if (old.weight != intValue(v, "weight")) mismatch.append("weight,");
        if (old.posterAspectRatio != intValue(v, "poster_art_aspect_ratio")) mismatch.append("poster_art_aspect_ratio,");
        // If artwork lookup is temporarily unavailable, preserve the existing poster rather
        // than treating the missing lookup as a request to clear artwork.
        if (v.containsKey("poster_art_uri")
                && !same(old.posterArtUri, v.getAsString("poster_art_uri"))) {
            mismatch.append("poster_art_uri,");
        }

        if (mismatch.length() == 0) return true;

        if (clean121aMismatchLogs < 8) {
            clean121aMismatchLogs++;
            Log.i(TAG, "CLEAN12.1A MISMATCH id=" + old.id
                    + " fields=" + mismatch
                    + " oldTitle=" + diag(old.title)
                    + " newTitle=" + diag(v.getAsString("title"))
                    + " oldDescription=" + diag(old.description)
                    + " newDescription=" + diag(v.getAsString("description"))
                    + " oldType=" + old.type
                    + " newType=" + intValue(v, "type")
                    + " oldIntent=" + diag(old.intentUri)
                    + " newIntent=" + diag(v.getAsString("intent_uri"))
                    + " oldWeight=" + old.weight
                    + " newWeight=" + intValue(v, "weight")
                    + " oldAspect=" + old.posterAspectRatio
                    + " newAspect=" + intValue(v, "poster_art_aspect_ratio")
                    + " oldPoster=" + diag(old.posterArtUri)
                    + " newPoster=" + diag(v.getAsString("poster_art_uri")));
        }
        return false;
    }

    private static String diag(String value) {
        if (value == null) return "<null>";
        String s = value.replace('\\n', ' ').replace('\\r', ' ');
        if (s.length() > 420) return s.substring(0, 420) + "...";
        return s;
    }
'''

if text.count(anchor) != 1:
    raise SystemExit(f'CLEAN12.1a sameProgram anchor mismatch: {text.count(anchor)}')
text = text.replace(anchor, replacement, 1)
engine.write_text(text)

check = engine.read_text()
for token in [
    'CLEAN12.1A MISMATCH id=',
    'fields=" + mismatch',
    'oldIntent=',
    'newIntent=',
    'oldPoster=',
    'newPoster=',
    'clean121aMismatchLogs < 8',
]:
    if token not in check:
        raise SystemExit('CLEAN12.1a verification failed: ' + token)

print('PASS: CLEAN12.1a mismatch diagnostics prepared')
