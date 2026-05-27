# Unified LLM Gateway Design

## Purpose

PIM needs one governed way to call large language models before adding AI features to files, quick notes, PC activity classification, reviews, scheduling, or future MCP tools.

The goal is not to build a custom LLM framework. The goal is to put a PIM-specific control plane around mature components:

- `Microsoft.Extensions.AI` is the .NET abstraction used by PIM code.
- LiteLLM Proxy is the local Docker LLM gateway used for provider routing, spend tracking, budgets, rate limits, and future fallback.
- `AiGateway` is the PIM service that adds business context, schema validation, full request logging, retry limits, auditability, and user-visible failures.

This stage should land before the file AI layer. Later modules must call LLMs through this gateway instead of talking to provider SDKs directly.

## Scope

This design covers:

- System-level AI configuration.
- LiteLLM Proxy as the first LLM gateway service.
- .NET AI abstraction with `Microsoft.Extensions.AI`.
- PIM `IAiGateway` as the only business-facing LLM entry point.
- Complete request and response logging.
- Token and estimated cost recording.
- JSON Schema validation for structured outputs.
- Hard retry limits so validation failures cannot create unbounded spend.
- AI status, request log, request detail, and usage summary APIs.
- A Web settings page for AI status, usage, logs, and request inspection.

This design does not cover:

- Per-user provider keys.
- LiteLLM UI customization.
- Agent workflows.
- MCP exposure.
- Automatic execution of LLM-generated high-risk actions.
- Embedding/vector storage. Embeddings remain a separate local-first capability.

## Architecture

The call chain is:

```text
PIM module
  -> PIM AiGateway
      -> Microsoft.Extensions.AI IChatClient
          -> LiteLLM Proxy
              -> OpenAI / Claude / Gemini / DeepSeek / local model / ...
```

### Responsibilities

`Microsoft.Extensions.AI`

- Provides the .NET `IChatClient` abstraction.
- Keeps business modules decoupled from specific provider SDKs.
- Targets the OpenAI-compatible endpoint exposed by LiteLLM.

LiteLLM Proxy

- Runs as a Docker service.
- Stores its own spend and routing data.
- Owns upstream provider keys.
- Exposes an OpenAI-compatible endpoint to PIM.
- Provides provider routing, virtual keys, budgets, rate limits, spend tracking, and future fallback.

PIM `AiGateway`

- Is the only service business modules call for LLM work.
- Adds module, purpose, source object, and correlation metadata.
- Applies system policies and hard limits.
- Saves complete prompt and response data in PIM.
- Saves token usage, estimated cost, status, errors, and schema validation results.
- Validates structured output.
- Retries at most the configured number of attempts.
- Returns user-visible failure details when AI work cannot complete.

Business modules

- Build sanitized business context.
- Choose a registered purpose and optional schema.
- Receive an `AiResult`.
- Never call LiteLLM, OpenAI, or provider SDKs directly.
- Never execute high-risk LLM output without a service-side preview and user confirmation.

## System Configuration

AI configuration is system-level in the first version. Users do not provide their own API keys.

Configuration keys:

- `Ai:Enabled`
- `Ai:Provider=litellm`
- `Ai:BaseUrl=http://litellm:4000`
- `Ai:ApiKey`
- `Ai:DefaultModel`
- `Ai:TimeoutSeconds`
- `Ai:MaxOutputTokensPerRequest`
- `Ai:MaxAttemptsPerRequest`
- `Ai:SaveFullPrompts=true`
- `Ai:SaveFullResponses=true`

LiteLLM stores upstream provider keys and its own virtual keys. PIM stores only the LiteLLM access configuration it needs.

PIM must never persist:

- Upstream provider API keys.
- Authorization headers.
- Nextcloud app passwords.
- Refresh tokens or login secrets.
- Raw HTTP headers containing credentials.

## Core Interfaces

`IAiGateway`

Business-facing entry point.

Inputs:

- Module name.
- Purpose name.
- Source object type and id.
- Messages.
- Model override, optional.
- Schema name/version, optional.
- Max output tokens.
- Max attempts.
- Metadata.

Outputs:

- Status.
- Response text.
- Parsed structured output, when applicable.
- Schema validation errors, when applicable.
- Token usage.
- Log id.
- User-facing error.

`IAiRequestLogWriter`

Writes each attempt and its outcome. It must log failures as well as successes.

`IAiSchemaRegistry`

Registers schemas by name and version. First implementation can register schemas in code. Persisting schema snapshots in request logs is required so historic outputs can be understood after schema changes.

`IAiUsageService`

Reads AI request logs and returns filtered logs, request detail, and usage summaries.

## Data Model

### `ai_provider_settings`

System-level provider state.

- `id`
- `provider`: `litellm`
- `base_url`
- `virtual_key_secret`
- `default_model`
- `status`: `enabled`, `disabled`, or `error`
- `last_health_check_at`
- `last_error`
- `created_at`
- `updated_at`

The `virtual_key_secret` must be stored using the same secret handling pattern used for other sensitive server-side settings.

### `ai_request_logs`

Every PIM AI request attempt is stored.

- `id`
- `user_id`
- `module`
- `purpose`
- `source_object_type`
- `source_object_id`
- `provider`: `litellm`
- `model`
- `litellm_request_id`
- `correlation_id`
- `status`: `succeeded`, `failed`, `blocked`, `timed_out`, or `failed_validation`
- `attempt_number`
- `max_attempts`
- `started_at`
- `finished_at`
- `duration_ms`
- `request_messages_json`
- `request_payload_json`
- `response_raw_json`
- `response_text`
- `parsed_output_json`
- `schema_name`
- `schema_version`
- `schema_json_snapshot`
- `schema_validation_errors_json`
- `prompt_tokens`
- `completion_tokens`
- `total_tokens`
- `estimated_cost`
- `currency`
- `input_chars`
- `output_chars`
- `input_hash`
- `output_hash`
- `error_code`
- `error_message`
- `metadata_json`

Complete prompt and response content is saved by default because this is a self-hosted personal system and detailed traceability is required. Credential-bearing fields are still redacted.

### `ai_usage_daily`

Optional aggregation cache. First implementation can compute usage directly from `ai_request_logs`; this table can be added later if log volume warrants it.

- `date`
- `module`
- `purpose`
- `model`
- `request_count`
- `success_count`
- `failure_count`
- `prompt_tokens`
- `completion_tokens`
- `total_tokens`
- `estimated_cost`

### `ai_prompt_templates`

Prompt templates can be registered in code in the first version. If persisted later, store:

- `id`
- `name`
- `version`
- `module`
- `purpose`
- `system_prompt`
- `user_template`
- `schema_name`
- `status`
- `created_at`
- `updated_at`

The request log must always store the final rendered prompt/messages.

### `ai_schema_definitions`

Schemas can be registered in code in the first version. The request log stores a schema snapshot. If persisted later, store:

- `name`
- `version`
- `json_schema`
- `description`
- `created_at`

## API Design

`GET /api/v1/ai/status`

Returns:

- AI enabled state.
- Provider.
- LiteLLM base URL or host display.
- Default model.
- Last health check.
- Last error.
- Recent successful call timestamp.

`POST /api/v1/ai/test`

Runs a small test prompt through `IAiGateway` using purpose `ai.test`. It verifies:

- PIM can reach LiteLLM.
- LiteLLM virtual key works.
- The model works.
- Token usage is captured.
- A request log is written.

`GET /api/v1/ai/requests`

Filters:

- Time range.
- Module.
- Purpose.
- Source object type and id.
- Model.
- Status.
- User.
- Page and page size.

List row fields:

- Timestamp.
- Module and purpose.
- Model.
- Status.
- Token totals.
- Estimated cost.
- Duration.
- Source object.
- Error summary.

`GET /api/v1/ai/requests/{id}`

Returns full detail:

- Complete messages.
- Complete request payload.
- Raw provider response.
- Response text.
- Parsed output.
- Schema validation errors.
- Token usage.
- Estimated cost.
- LiteLLM request id.
- Correlation id.
- Error detail.

`GET /api/v1/ai/usage/summary`

Returns request counts, token totals, estimated cost, failure rates, and grouping by:

- Module.
- Purpose.
- Model.
- Status.

`POST /api/v1/ai/health-check`

Manually checks LiteLLM reachability and the default model.

## Web Design

Add `/settings/ai`.

Sections:

1. Configuration status
   - AI enabled state.
   - Provider.
   - Default model.
   - LiteLLM health.
   - Recent error.
   - Test connection action.

2. Usage overview
   - Today, week, and month request counts.
   - Prompt, completion, and total tokens.
   - Estimated cost.
   - Failure rate.
   - Usage by module and model.

3. Request logs
   - Filterable table.
   - Timestamp, module, purpose, model, status, tokens, duration, and error summary.

4. Request detail
   - Complete prompt/messages.
   - Complete output.
   - Raw JSON.
   - Parsed JSON.
   - Schema errors.
   - Token and cost data.

The Web page does not edit upstream provider keys. Provider key management stays in LiteLLM configuration and secrets.

## Structured Output

Structured tasks must declare:

- `schemaName`
- `schemaVersion`
- JSON Schema
- `maxTokens`
- `maxAttempts`

`AiGateway` asks for JSON output and validates the returned content against the registered JSON Schema.

If validation fails:

1. The failed attempt is logged with full response and validation errors.
2. If attempts remain, a short repair prompt is issued.
3. The repair prompt must not expand the original context.
4. When attempts are exhausted, the result is `failed_validation`.
5. The user sees a clear message that no suggestion was produced because the AI response did not match the required format.

Default attempt policy:

- Interactive single request: max 2 attempts.
- Background/batch task: max 1 attempt.
- System hard limit: no more than 2 attempts in the first version.

This prevents malformed model output from creating unbounded cost.

## Failure Modes

`disabled`

- AI is off.
- Log status is `blocked`.
- No LiteLLM call is made.

`policy_blocked`

- Module, purpose, object, directory, or sensitivity policy blocks the call.
- Log status is `blocked`.
- No LiteLLM call is made.

`provider_unavailable`

- LiteLLM is unreachable or the model is unavailable.
- Log status is `failed`.
- User sees a recoverable provider error.

`timed_out`

- The request exceeds timeout.
- Log status is `timed_out`.

`schema_validation_failed`

- Output does not match schema and attempts are exhausted.
- Log status is `failed_validation`.
- No downstream business suggestion is created.

`partial_success`

- Batch workflows must expose failed items.
- Failed items are not silently ignored.

## Security And Privacy

Full prompts and outputs are saved because the system is self-hosted and traceability is valuable. However, credentials and transport secrets are still redacted.

Redaction must cover:

- `Authorization` headers.
- API keys.
- LiteLLM virtual keys.
- Nextcloud app passwords.
- JWTs and refresh tokens.
- Known credential fields in JSON payloads.

Modules are responsible for sanitizing domain-specific context before calling `AiGateway`. For example, PC activity LLM calls must use the existing URL sanitizer and avoid sending query strings, fragments, userinfo, or token-like path segments.

## Relationship To Future Modules

PC activity LLM suggestions:

- Use `IAiGateway` for draft rule generation and natural-language correction.
- Store LLM outputs in existing suggestion records as derived business data.
- Keep accepted rules behind user confirmation.

Quick notes AI:

- Use `IAiGateway` to propose structured conversions.
- Keep original quick note content unchanged.
- Require confirmation before creating tasks, events, reminders, or file operations.

File AI:

- Use local text extraction and local embeddings for retrieval.
- Use `IAiGateway` through LiteLLM for summaries, tags, and organization suggestions.
- Never call LLM provider APIs directly.

Reviews and recommendations:

- Use `IAiGateway` for natural-language summaries.
- Treat AI output as advice, not fact.

MCP:

- MCP tools call PIM APIs and never bypass `AiGateway` for AI generation.

## Deployment

Docker Compose adds `litellm`.

LiteLLM:

- Uses Postgres for spend/log persistence.
- Holds upstream provider keys in env/secrets.
- Exposes internal OpenAI-compatible endpoint to PIM.
- Provides a virtual key for PIM.
- May have budgets and rate limits configured in LiteLLM.

PIM API:

- Uses `Ai:BaseUrl=http://litellm:4000`.
- Uses the PIM LiteLLM virtual key.
- Logs all PIM-level request detail.

LiteLLM UI is not part of the normal user workflow. PIM provides the Chinese AI settings and logs page.

## Testing

Unit tests:

- `AiGateway` logs successful requests.
- Disabled AI produces `blocked` without provider call.
- Provider failures are logged.
- Timeouts are logged.
- Structured JSON success returns parsed output.
- Structured JSON validation failure retries no more than configured attempts.
- Repair prompt does not expand original context.
- Credentials are redacted from logs.
- Token usage is persisted when present.
- Usage summary groups by module, purpose, model, and status.

API tests:

- AI endpoint paths are stable.
- Request list filters work.
- Request detail returns complete prompt/output fields.
- Usage summary returns grouped token totals.

Manual verification:

- Start Docker Compose with LiteLLM.
- Configure at least one upstream model.
- Open `/settings/ai`.
- Run test connection.
- Confirm `ai_request_logs` contains full prompt and response.
- Confirm token usage appears in the Web UI.
- Stop LiteLLM and confirm user-visible failure without breaking core app.
- Run a schema task with invalid output and confirm attempts stop at the configured limit.

## Completion Definition

This stage is complete when:

- PIM can call a LiteLLM-backed model through `Microsoft.Extensions.AI`.
- Business modules have a single `IAiGateway` entry point.
- Every call attempt is logged with full prompt, full output, token usage, status, and source object metadata.
- JSON Schema validation exists with hard retry limits.
- AI status, request logs, request detail, and usage summaries are visible in Web.
- Provider errors and schema failures are visible to users.
- No module needs to know upstream provider keys or call provider SDKs directly.
