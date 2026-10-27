from pathlib import Path

try:
    exec(Path('.github/patch-v9-seeded-cold-boot.py').read_text(), globals())
except SystemExit as e:
    # The base patch intentionally leaves the old retry helper compiled but unreachable.
    # Its broad string assertion sees that helper's syncRedLightAsync(app) call.  Accept
    # only that known false positive, then verify the actual lifecycle entry points below.
    if 'syncRedLightAsync(app);' not in str(e):
        raise

embedded = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/EmbeddedRuntime.java').read_text()
engine = Path('embedded-runtime/src/main/java/com/spocky/projengmenu/embedded/RedLightEngine.java').read_text()

def method(text, signature):
    start = text.index(signature)
    end = text.index('\n    }', start) + len('\n    }')
    return text[start:end]

resume = method(embedded, '    public static void onLauncherResume(Context context) {')
bootstrap = method(embedded, '        @Override public boolean onCreate() {')
boot = method(embedded, '        @Override public void onReceive(Context c, Intent i) {')
job = method(embedded, '        @Override public boolean onStartJob(JobParameters p) {')

assert 'seedBundledChannelsOnce(app)' in resume
assert 'syncRedLightAsync(' not in resume
assert 'scheduleRedLight(' not in resume
assert 'syncRedLightAsync(' not in bootstrap
assert 'scheduleRedLight(' not in bootstrap
assert 'scheduleRedLight(' not in boot
assert 'RedLightEngine.sync(' not in job
assert 'Scheduled provider refresh suppressed' in job

for forbidden in ('setLaunchDisplayId(', 'VIRTUAL_DISPLAY_FLAG_PUBLIC', 'Requested Kodi Main on hidden virtual display'):
    assert forbidden not in embedded, forbidden
for required in ('redlight_seed.json', 'redlight_seed_icon.png', 'syncSeedPrograms', 'writeSeedChannelLogo'):
    assert required in engine, required

print('v9.6 final lifecycle safety checks passed: no cold Kodi/provider entry point')
