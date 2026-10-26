from pathlib import Path

root = Path('/tmp/projectivy-decoded')
bs4 = root / 'smali/bs4.smali'
native_id = 'd0c1c218-4906-4ed5-a520-a9aee0b87541'

provider = r'''.class public final Laer0;
.super Lz05;
.source "AerialRowsNative"

.method public constructor <init>()V
    .locals 1
    const-string v0, "AIR"
    invoke-direct {p0, v0}, Lz05;-><init>(Ljava/lang/String;)V
    return-void
.end method

.method public final A(Ltv/projectivy/plugin/wallpaperprovider/api/Event;)Ljava/util/List;
    .locals 8
    invoke-virtual {p1}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
    new-instance v0, Ltv/projectivy/plugin/wallpaperprovider/api/Wallpaper;
    const-string v1, "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/comp_GMT026_363A_103NC_E1027_KOREA_JAPAN_NIGHT_v18_SDR_PS_20180907_SDR_2K_AVC.mov"
    const/4 v2, 0x4
    const/4 v3, 0x1
    const-string v4, "East Asia"
    const-string v5, "Aerial Rows · Apple TV"
    const/4 v6, 0x0
    const/4 v7, 0x0
    invoke-direct/range {v0 .. v7}, Ltv/projectivy/plugin/wallpaperprovider/api/Wallpaper;-><init>(Ljava/lang/String;IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
    invoke-static {v0}, Lab0;->I(Ljava/lang/Object;)Ljava/util/List;
    move-result-object v0
    return-object v0
.end method

.method public final N()Lta3;
    .locals 1
    sget-object v0, Lta3;->vp:Lta3;
    return-object v0
.end method

.method public final e()I
    .locals 1
    const p0, 0x36ee80
    return p0
.end method

.method public final getId()Ljava/lang/String;
    .locals 1
    const-string v0, "d0c1c218-4906-4ed5-a520-a9aee0b87541"
    return-object v0
.end method

.method public final getName()Ljava/lang/String;
    .locals 1
    const-string v0, "Aerial Rows"
    return-object v0
.end method
'''
(root / 'smali/aer0.smali').write_text(provider)

text = bs4.read_text()

old_ids = '''    const-string v0, "e77610c3-d495-4090-b387-fae3faafa718"\n\n    .line 120\n    .line 121\n    const-string v1, "eea7a685-d68f-4fb5-b46e-b4897af5413f"\n\n    .line 122\n    .line 123\n    const-string v2, "773b5948-b4d1-49c5-b994-999210bc8877"\n\n    .line 124\n    .line 125\n    const-string v3, "326618d8-3242-4322-a1b1-9e60e5d3dfc3"\n\n    .line 126\n    .line 127\n    filled-new-array {v2, v3, v0, v1}, [Ljava/lang/String;'''
new_ids = f'''    const-string v0, "e77610c3-d495-4090-b387-fae3faafa718"\n\n    .line 120\n    .line 121\n    const-string v1, "eea7a685-d68f-4fb5-b46e-b4897af5413f"\n\n    .line 122\n    .line 123\n    const-string v2, "773b5948-b4d1-49c5-b994-999210bc8877"\n\n    .line 124\n    .line 125\n    const-string v3, "326618d8-3242-4322-a1b1-9e60e5d3dfc3"\n\n    const-string v4, "{native_id}"\n\n    .line 126\n    .line 127\n    filled-new-array {{v4, v2, v3, v0, v1}}, [Ljava/lang/String;'''
assert text.count(old_ids) == 1, 'native wallpaper ID registry anchor mismatch'
text = text.replace(old_ids, new_ids, 1)

old_ctor = '''    new-instance v10, Lem4;\n\n    .line 123\n    .line 124\n    invoke-direct {v10}, Lem4;-><init>()V\n\n    .line 125\n    .line 126\n    .line 127\n    const/16 v11, 0xa'''
new_ctor = '''    new-instance v10, Lem4;\n\n    .line 123\n    .line 124\n    invoke-direct {v10}, Lem4;-><init>()V\n\n    new-instance v15, Laer0;\n\n    invoke-direct {v15}, Laer0;-><init>()V\n\n    .line 125\n    .line 126\n    .line 127\n    const/16 v11, 0xb'''
assert text.count(old_ctor) == 1, 'native provider constructor anchor mismatch'
text = text.replace(old_ctor, new_ctor, 1)

start = text.index('    const/4 v13, 0x0\n\n    .line 132\n    aput-object v1, v12, v13')
end_anchor = '    aput-object v10, v12, v5'
end = text.index(end_anchor, start) + len(end_anchor)
new_array = '''    const/4 v13, 0x0

    aput-object v15, v12, v13

    const/4 v14, 0x1
    aput-object v1, v12, v14
    const/4 v1, 0x1

    const/4 v14, 0x2
    aput-object v2, v12, v14
    const/4 v2, 0x2

    const/4 v14, 0x3
    aput-object v3, v12, v14
    const/4 v3, 0x3

    const/4 v14, 0x4
    aput-object v4, v12, v14

    const/4 v14, 0x5
    aput-object v5, v12, v14

    const/4 v14, 0x6
    aput-object v6, v12, v14

    const/4 v3, 0x6
    const/4 v14, 0x7
    aput-object v7, v12, v14

    const/4 v4, 0x7
    const/16 v14, 0x8
    aput-object v8, v12, v14

    const/16 v5, 0x8
    const/16 v14, 0x9
    aput-object v9, v12, v14

    const/16 v5, 0x9
    const/16 v14, 0xa
    aput-object v10, v12, v14'''
text = text[:start] + new_array + text[end:]
bs4.write_text(text)

print('Aerial Rows native provider injected:', native_id)
