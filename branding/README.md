# PIM Branding

## Canonical mark

- File: `pim-mark.svg`
- Design: four-color mosaic **P** (approved 2026-07-12)
- Colors: `#f25022` `#7fba00` `#00a4ef` `#ffb900`
- Spec: `docs/superpowers/specs/2026-07-12-version-icons-github-actions-design.md`

Do not hand-edit derived icons under `src/client-web/public/`, `src/client-windows/**/app.ico`, or Android `mipmap-*`. Regenerate with:

```bash
node scripts/branding/export-icons.mjs
```

After changing `pim-mark.svg`, run export and commit both source and derivatives.
