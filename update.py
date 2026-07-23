import re

with open('/data/data/com.termux/files/home/vocab-master/.planning/phases/04-sync-integration-verification/04-01-PLAN.md', 'r') as f:
    content = f.read()

# Fix SettingsScreen path
content = content.replace('ui/screen/SettingsScreen.kt', 'ui/screens/SettingsScreen.kt')

# Fix VocabDao path
content = content.replace('data/local/dao/VocabDao.kt', 'data/database/VocabDao.kt')

# Add SettingsScreenContent.kt to files_modified
if 'SettingsScreenContent.kt' not in content:
    content = content.replace(
        '  - app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt\n',
        '  - app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt\n  - app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreenContent.kt\n'
    )
    
    # Add to artifacts
    content = content.replace(
        '    - "app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt"\n',
        '    - "app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt"\n    - "app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreenContent.kt"\n'
    )

    # Add to Task 1 files
    content = content.replace(
        'app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt,',
        'app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreen.kt, app/src/main/java/com/nhimz/vocabmaster/ui/screens/SettingsScreenContent.kt,'
    )

# Add key_link
if 'SyncManager.performSync() returns SyncResult' not in content:
    content = content.replace(
        '  key_links:\n',
        '  key_links:\n    - "SyncManager.performSync() returns SyncResult sealed class (Success/Error)"\n'
    )

# Add rationale to objective
if 'Nyquist Wave 0 deviation accepted' not in content:
    content = content.replace(
        '</objective>',
        'Rationale: Nyquist Wave 0 deviation accepted; test creation is handled via inline TDD in Task 2 instead of a separate Wave 0 plan.\n</objective>'
    )

with open('/data/data/com.termux/files/home/vocab-master/.planning/phases/04-sync-integration-verification/04-01-PLAN.md', 'w') as f:
    f.write(content)
