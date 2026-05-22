# PC Activity LLM Classification Follow-Up

This follow-up plan should be written after the local classifier step lands.

Required scope:

- Add an LLM provider interface and configuration.
- Add request/response contracts for cluster suggestions.
- Use `ActivityUrlSanitizer` for all provider-bound URLs.
- Add `/classification/suggestions/{id}/llm`.
- Add `/classification/suggestions/{id}/correct`.
- Support natural-language correction that revises a draft rule without activating it.
- Add impact preview before accepting a draft.
- Verify that query strings, fragments, userinfo, and token-like URL data never reach provider payloads.
