from pathlib import Path

root = Path('/tmp/projectivy-decoded')
bs4 = root / 'smali/bs4.smali'
aerial_id = '8c00feaf-c7be-43c2-80f9-d49853aaa99a'
text = bs4.read_text()

# Add the Aerial Rows provider ID to Projectivy's built-in/trusted wallpaper
# registry. Existing IDs are preserved; this only registers the new feature.
old_ids = '''    const-string v0, "e77610c3-d495-4090-b387-fae3faafa718"\n\n    .line 120\n    .line 121\n    const-string v1, "eea7a685-d68f-4fb5-b46e-b4897af5413f"\n\n    .line 122\n    .line 123\n    const-string v2, "773b5948-b4d1-49c5-b994-999210bc8877"\n\n    .line 124\n    .line 125\n    const-string v3, "326618d8-3242-4322-a1b1-9e60e5d3dfc3"\n\n    .line 126\n    .line 127\n    filled-new-array {v2, v3, v0, v1}, [Ljava/lang/String;'''
new_ids = f'''    const-string v0, "e77610c3-d495-4090-b387-fae3faafa718"\n\n    .line 120\n    .line 121\n    const-string v1, "eea7a685-d68f-4fb5-b46e-b4897af5413f"\n\n    .line 122\n    .line 123\n    const-string v2, "773b5948-b4d1-49c5-b994-999210bc8877"\n\n    .line 124\n    .line 125\n    const-string v3, "326618d8-3242-4322-a1b1-9e60e5d3dfc3"\n\n    const-string v4, "{aerial_id}"\n\n    .line 126\n    .line 127\n    filled-new-array {{v4, v2, v3, v0, v1}}, [Ljava/lang/String;'''
assert text.count(old_ids) == 1, 'trusted wallpaper registry anchor mismatch'
text = text.replace(old_ids, new_ids, 1)

# Preserve every provider's relative order, but move Aerial Rows to the first
# slot when its companion service is installed/discovered.
old_return = '''    :cond_7\n    new-instance v1, Ljava/util/LinkedHashMap;\n\n    .line 497\n    .line 498\n    invoke-direct {v1, v0}, Ljava/util/LinkedHashMap;-><init>(Ljava/util/Map;)V\n\n    .line 499\n    .line 500\n    .line 501\n    return-object v1\n.end method'''
new_return = f'''    :cond_7\n    const-string v1, "{aerial_id}"\n\n    invoke-interface {{v0, v1}}, Ljava/util/Map;->remove(Ljava/lang/Object;)Ljava/lang/Object;\n\n    move-result-object v2\n\n    new-instance v3, Ljava/util/LinkedHashMap;\n\n    invoke-direct {{v3}}, Ljava/util/LinkedHashMap;-><init>()V\n\n    if-eqz v2, :aerial_rows_order_done\n\n    invoke-interface {{v3, v1, v2}}, Ljava/util/Map;->put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;\n\n    :aerial_rows_order_done\n    invoke-interface {{v3, v0}}, Ljava/util/Map;->putAll(Ljava/util/Map;)V\n\n    return-object v3\n.end method'''
assert text.count(old_return) == 1, 'provider order anchor mismatch'
text = text.replace(old_return, new_return, 1)

bs4.write_text(text)
print('Integrated Aerial Rows provider:', aerial_id)
