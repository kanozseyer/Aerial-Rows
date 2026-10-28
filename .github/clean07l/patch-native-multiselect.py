#!/usr/bin/env python3
import runpy

# Preserve CLEAN 07K byte-for-byte behavior first, then add only the Red Light
# source-heading presentation patch to the same decoded APK tree.
runpy.run_path('.github/clean07k/patch-native-multiselect.py', run_name='__main__')
runpy.run_path('.github/clean07l/patch-redlight-label.py', run_name='__main__')
