# PC Activity Understanding Stage 2 Acceptance

Stage 2 verifies the local, non-LLM PC activity understanding loop: persisted classifications, rule-based recompute, reviewable suggestions, quick correction, and adjustable display granularity.

## Scope

- Persist classification snapshots for interpreted PC activity records.
- Prefer persisted classifications in PC summary, timeline, and detail queries.
- Keep user or corrected classifications protected from automatic overwrite.
- Support local rule preview and confirmed apply over an explicit date range.
- Produce reviewable suggestions for unknown or low-confidence activity.
- Allow quick correction from the PC records page.
- Allow classification rule inspection from a dedicated management page.
- Allow the recommended minimum classification duration to be tuned without deleting raw facts.
- Leave AI/LLM classification integration for a later stage.

## Manual Checks

- Open the PC records page for a day that includes coding, documents, terminal, file manager, browser, and communication activity.
- Confirm the timeline shows category, project tag, confidence, source, and explanation.
- Confirm unknown or low-confidence activity appears as reviewable suggestion cards.
- Open a suggestion with quick correction and confirm category, project tag, date range, preview, and apply controls are visible.
- Confirm preview shows affected record count, affected duration, current category distribution, and new category distribution.
- Apply a rule to today only and confirm other dates are not changed.
- Confirm rejected suggestions disappear from the review list.
- Open classification management from the sidebar.
- Confirm rules are visible and selecting a rule shows its category, project tag, priority, confidence, explanation, and condition JSON.
- Change recommended minimum classification duration to 1, 5, and 10 minutes.
- Confirm 1 minute shows more detail and 10 minutes smooths incidental fragments.
- Confirm short but clearly independent activity, such as a brief communication burst, remains visible when appropriate.
- Confirm an audit log entry exists for an applied rule with the selected range and affected count.

## Verification Commands

Run backend unit tests:

```powershell
dotnet test tests/Pim.UnitTests/Pim.UnitTests.csproj
```

Build the web client:

```powershell
npm --prefix src/client-web run build
```

Check current git state:

```powershell
git status --short --branch
```

## Runtime Notes

- The local API is expected at `http://127.0.0.1:5858`.
- The web client can be run with `npm --prefix src/client-web run dev`.
- Classification management is available at `/pc-classification`.
- Quick correction lives on the PC records page and always requires preview before apply.
- Recommended minimum classification duration affects smoothing and suggestion grouping only; raw ActivityWatch and KeyStats facts remain intact.
