from pathlib import Path
import re
import sys

ROOT = Path(sys.argv[1] if len(sys.argv) > 1 else "/tmp/pj")

# Proven billing-source no-op from CLEAN 01E / 01F.
p = ROOT / "smali/it1.smali"
s = p.read_text()
m = re.search(r"\.method public final g\(\)V\n.*?\.end method", s, re.S)
if not m:
    raise SystemExit("Lit1.g() not found")
s = s[:m.start()] + ".method public final g()V\n    .locals 0\n    # CLEAN 01G: Google Billing connection disabled.\n    return-void\n.end method" + s[m.end():]
p.write_text(s)

# Strip Android-visible billing integration and premium activities.
p = ROOT / "AndroidManifest.xml"
s = p.read_text()
patterns = [
    r'\s*<uses-permission android:name="com\.android\.vending\.BILLING"\s*/>',
    r'\s*<activity[^>]*android:name="com\.android\.billingclient\.api\.ProxyBillingActivity(?:V2)?"[^>]*/>',
    r'\s*<meta-data[^>]*android:name="com\.google\.android\.play\.billingclient\.version"[^>]*/>',
    r'\s*<intent>\s*<action android:name="com\.android\.vending\.billing\.InAppBillingService\.BIND"\s*/>\s*</intent>',
    r'\s*<intent>\s*<action android:name="com\.google\.android\.apps\.play\.billingtestcompanion\.BillingOverrideService\.BIND"\s*/>\s*</intent>',
    r'\s*<activity[^>]*android:name="com\.spocky\.projengmenu\.ui\.guidedActions\.activities\.about\.PremiumActivity"[^>]*/>',
    r'\s*<activity[^>]*android:name="com\.spocky\.projengmenu\.ui\.guidedActions\.activities\.about\.PremiumCardActivity"[^>]*/>',
]
for pat in patterns:
    s = re.sub(pat, "", s)
p.write_text(s)

premium = "Lcom/spocky/projengmenu/ui/guidedActions/activities/about/PremiumActivity;"
callers = [
    ROOT / "smali_classes2/com/spocky/projengmenu/ui/launcherActivities/ImportThemeActivity.smali",
    ROOT / "smali_classes2/com/spocky/projengmenu/ui/launcherActivities/SetBackgroundActivity.smali",
    ROOT / "smali_classes2/com/spocky/projengmenu/ui/launcherActivities/ToggleParentalControlActivity.smali",
    ROOT / "smali_classes2/gh.smali",
    ROOT / "smali_classes2/ft.smali",
]
for p in callers:
    s = p.read_text()
    if premium not in s:
        raise SystemExit(f"premium ref not found in {p}")
    pos = s.find(premium)
    start = s.rfind("    new-instance ", 0, pos)
    if start < 0 or "Landroid/content/Intent;" not in s[start:s.find("\n", start) + 1]:
        start = s.find("    new-instance ", pos)
        while start >= 0 and "Landroid/content/Intent;" not in s[start:s.find("\n", start) + 1]:
            start = s.find("    new-instance ", start + 1)
    if start < 0:
        raise SystemExit(f"premium Intent start not found in {p}")
    e = s.find("->startActivity(Landroid/content/Intent;)V", start)
    if e < 0:
        raise SystemExit(f"premium startActivity not found in {p}")
    e = s.find("\n", e) + 1
    s = s[:start] + "    # CLEAN 01G: premium upgrade redirect removed.\n    nop\n" + s[e:]
    # ft preloads the class before entering the redirect branch.
    s = s.replace(premium, "Ljava/lang/Object;")
    if "PremiumActivity;" in s:
        raise SystemExit(f"remaining PremiumActivity ref in {p}")
    p.write_text(s)

# About list: [About App, Premium, About Device, About Background]
# -> [About App, About Device, About Background].
p = ROOT / "smali/w40.smali"
s = p.read_text()
prem_ref = "Lcom/spocky/projengmenu/ui/guidedActions/activities/about/PremiumCardActivity;->e0:Lp;"
pp = s.find(prem_ref)
if pp < 0:
    raise SystemExit("PremiumCardActivity menu ref not found")
arr_start = s.rfind("    new-array v0, v4, [Ld90;", 0, pp)
if arr_start < 0:
    raise SystemExit("four-item About array start not found")
last = "    aput-object v15, v0, v3"
arr_end = s.find(last, arr_start)
if arr_end < 0 or arr_end > pp:
    raise SystemExit("four-item About array end not found before premium ref")
arr_end = s.find("\n", arr_end) + 1
old = s[arr_start:arr_end]
for required in (
    "aput-object v12, v0, v11",
    "aput-object v13, v0, v1",
    "aput-object v14, v0, v2",
    "aput-object v15, v0, v3",
):
    if required not in old:
        raise SystemExit("unexpected About array shape: " + required)
new = (
    "    const/4 v0, 0x3\n"
    "    new-array v0, v0, [Ld90;\n"
    "    aput-object v12, v0, v11\n"
    "    aput-object v14, v0, v1\n"
    "    aput-object v15, v0, v2\n"
)
s = s[:arr_start] + new + s[arr_end:]
needle = "    sget-object v19, " + prem_ref
if s.count(needle) != 1:
    raise SystemExit("PremiumCardActivity sget count unexpected")
s = s.replace(needle, "    const/4 v19, 0x0", 1)
p.write_text(s)

# No live code may reference premium activities outside their own dead definitions.
bad = []
for root in [ROOT / "smali", ROOT / "smali_classes2", ROOT / "smali_classes3"]:
    for f in root.rglob("*.smali"):
        if f.name in ("PremiumActivity.smali", "PremiumCardActivity.smali"):
            continue
        t = f.read_text(errors="ignore")
        if "PremiumActivity;" in t or "PremiumCardActivity;" in t:
            bad.append(str(f))
if bad:
    raise SystemExit("remaining premium refs: " + repr(bad))

print("PASS: CLEAN 01G source patch assertions passed")
