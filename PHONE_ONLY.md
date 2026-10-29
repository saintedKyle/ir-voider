# Build and publish an APK from your phone

This repo has a GitHub Actions workflow that builds a **signed APK and AAB** in the
cloud. Your PC can be off; you only need Qoder Mobile / GitHub access.

## How it works

```
Qoder Mobile Cloud Task
   → edit code, bump version, commit, push
   → create a tag (v1.3.1 etc.) or use GitHub's web UI
   → GitHub Actions builds signed APK + AAB
   → release gets the APK/AAB assets
```

## One-time setup (already done)

The workflow reads four encrypted repository secrets:

- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`
- `KEY_PASSWORD`

The keystore itself is **not** stored in the repository. It lives in GitHub
Secrets and is decoded only inside the build job. Never commit
`keys/` or `keystore.properties`.

Because the repo is public, keep the main branch protected and do not merge
workflow changes from strangers; repository secrets are not exposed to fork PRs.

## Publish from the phone

1. In Qoder Mobile, start a **Cloud Task** on `main`.
2. Make your code changes.
3. Bump the version:
   - `versionCode` +1
   - `versionName` (e.g. `1.4`)
4. Commit and push.
5. Create a tag/release:
   - Ask the cloud agent to run `git tag v1.4 && git push origin v1.4`, or
   - GitHub mobile web → Releases → Draft a new release → new tag `v1.4`.
6. Actions → **Build signed APK and AAB** builds and attaches:
   - `IRVoider.apk`
   - `IRVoider.aab`
7. Download the APK from the release and test.

You can also run the workflow manually from the **Actions** tab
(`workflow_dispatch`); it uploads artifacts without creating a release.

## Limits

- The cloud has no access to your local uncommitted files.
- It cannot upload to Google Play Console; upload the AAB manually.
- If secrets are missing, the build fails at “Decode signing key.”
- Rotate the upload key in Play Console if you ever suspect the keystore or
  secrets were exposed.
