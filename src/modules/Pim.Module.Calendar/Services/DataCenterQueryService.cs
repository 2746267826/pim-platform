using Microsoft.EntityFrameworkCore;
using Pim.Core.Exceptions;
using Pim.Core.Operations;
using Pim.Infrastructure.Audit;
using Pim.Infrastructure.Auth;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.DTOs;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public sealed class DataCenterQueryService
{
    private static readonly HashSet<string> PendingStatuses = new(StringComparer.OrdinalIgnoreCase)
    {
        OperationConfirmationStatus.Pending.ToString(),
        "pending"
    };

    private readonly PimDbContext _db;
    private readonly ICurrentUserService _currentUser;

    public DataCenterQueryService(PimDbContext db, ICurrentUserService currentUser)
    {
        _db = db;
        _currentUser = currentUser;
    }

    private Guid UserId => _currentUser.UserId ?? throw new DomainException(01002, "Login required");

    public async Task<DataCenterQueryResponse> QueryAsync(
        DataCenterQueryRequest request,
        CancellationToken ct = default)
    {
        var userId = UserId;
        var page = Math.Max(1, request.Page);
        var pageSize = Math.Clamp(request.PageSize, 1, 100);
        var items = new List<DataCenterItem>();
        var search = request.Search?.Trim();
        var hasSearch = !string.IsNullOrWhiteSpace(search);
        var loweredSearch = hasSearch ? search!.ToLowerInvariant() : null;
        var objectType = request.ObjectType?.Trim();
        var hasObjectType = !string.IsNullOrWhiteSpace(objectType);
        var sourceFilter = request.Source?.Trim();
        var hasSource = !string.IsNullOrWhiteSpace(sourceFilter);
        var pendingOnly = request.PendingOnly;

        bool ShouldLoad(string candidate)
        {
            if (pendingOnly)
                return string.Equals(candidate, "confirmation", StringComparison.OrdinalIgnoreCase);
            if (hasObjectType)
                return string.Equals(candidate, objectType, StringComparison.OrdinalIgnoreCase);
            return true;
        }

        if (ShouldLoad("event"))
        {
            var q = _db.Set<EventEntity>()
                .AsNoTracking()
                .Include(e => e.Calendar)
                .Where(e => e.Calendar.UserId == userId);
            if (hasSource)
                q = q.Where(e => e.Source == sourceFilter);
            if (hasSearch)
                q = q.Where(e => e.Title.ToLower().Contains(loweredSearch!)
                    || e.Description != null && e.Description.ToLower().Contains(loweredSearch!)
                    || e.Location != null && e.Location.ToLower().Contains(loweredSearch!)
                    || e.Source.ToLower().Contains(loweredSearch!)
                    || e.Status.ToLower().Contains(loweredSearch!));
            var events = await q.ToListAsync(ct);
            items.AddRange(events.Select(e => new DataCenterItem(
                "event",
                e.Id,
                e.Title,
                e.Source,
                e.Status,
                e.DtStart,
                e.DtEnd,
                BuildEventSummary(e))));
        }

        if (ShouldLoad("task"))
        {
            var q = _db.Set<TaskEntity>()
                .AsNoTracking()
                .Include(t => t.Calendar)
                .Where(t => t.UserId == userId);
            if (hasSearch)
                q = q.Where(t => t.Title.ToLower().Contains(loweredSearch!)
                    || t.Description != null && t.Description.ToLower().Contains(loweredSearch!)
                    || t.Status.ToLower().Contains(loweredSearch!));
            var tasks = await q.ToListAsync(ct);
            items.AddRange(tasks.Select(t => new DataCenterItem(
                "task",
                t.Id,
                t.Title,
                "manual",
                t.Status,
                t.DtStart,
                t.PlannedEnd ?? t.Due,
                FirstText(t.Description, t.Calendar?.Name))));
        }

        if (ShouldLoad("task-segment"))
        {
            var q = _db.Set<TaskExecutionSegmentEntity>()
                .AsNoTracking()
                .Include(s => s.Task)
                .Where(s => s.UserId == userId);
            if (hasSearch)
                q = q.Where(s => s.Task.Title.ToLower().Contains(loweredSearch!)
                    || s.PlanningReason != null && s.PlanningReason.ToLower().Contains(loweredSearch!)
                    || s.Source.ToLower().Contains(loweredSearch!)
                    || s.Status.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(s => s.Source == sourceFilter);
            var segments = await q.ToListAsync(ct);
            items.AddRange(segments.Select(s => new DataCenterItem(
                "task-segment",
                s.Id,
                s.Task.Title,
                s.Source,
                s.Status,
                s.StartsAt,
                s.EndsAt,
                FirstText(s.PlanningReason, s.Task.Description))));
        }

        if (ShouldLoad("habit"))
        {
            var q = _db.Set<HabitRoutineEntity>()
                .AsNoTracking()
                .Where(h => h.UserId == userId);
            if (hasSearch)
                q = q.Where(h => h.Title.ToLower().Contains(loweredSearch!)
                    || h.Description != null && h.Description.ToLower().Contains(loweredSearch!)
                    || h.Cadence.ToLower().Contains(loweredSearch!)
                    || h.Source.ToLower().Contains(loweredSearch!)
                    || h.Status.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(h => h.Source == sourceFilter);
            var habits = await q.ToListAsync(ct);
            items.AddRange(habits.Select(h => new DataCenterItem(
                "habit",
                h.Id,
                h.Title,
                h.Source,
                h.Status,
                h.CreatedAt,
                h.UpdatedAt,
                FirstText(h.Description, h.Cadence, h.RuleJson))));
        }

        if (ShouldLoad("habit-occurrence"))
        {
            var q = _db.Set<HabitOccurrenceEntity>()
                .AsNoTracking()
                .Include(o => o.HabitRoutine)
                .Where(o => o.UserId == userId);
            if (hasSearch)
                q = q.Where(o => o.HabitRoutine.Title.ToLower().Contains(loweredSearch!)
                    || o.Source.ToLower().Contains(loweredSearch!)
                    || o.Status.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(o => o.Source == sourceFilter);
            var habitOccurrences = await q.ToListAsync(ct);
            items.AddRange(habitOccurrences.Select(o => new DataCenterItem(
                "habit-occurrence",
                o.Id,
                o.HabitRoutine.Title,
                o.Source,
                o.Status,
                o.StartsAt,
                o.EndsAt,
                $"Habit occurrence for {o.HabitRoutine.Cadence} routine")));
        }

        if (ShouldLoad("availability"))
        {
            var q = _db.Set<AvailabilityWindowEntity>()
                .AsNoTracking()
                .Where(a => a.UserId == userId);
            if (hasSearch)
                q = q.Where(a => a.Title.ToLower().Contains(loweredSearch!)
                    || a.Source.ToLower().Contains(loweredSearch!)
                    || a.Kind.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(a => a.Source == sourceFilter);
            var availability = await q.ToListAsync(ct);
            items.AddRange(availability.Select(a => new DataCenterItem(
                "availability",
                a.Id,
                a.Title,
                a.Source,
                a.Kind,
                a.StartsAt,
                a.EndsAt,
                "Availability window")));
        }

        if (ShouldLoad("ai-placeholder"))
        {
            var q = _db.Set<AiPlanningPlaceholderEntity>()
                .AsNoTracking()
                .Where(p => p.UserId == userId);
            if (hasSearch)
                q = q.Where(p => p.Title.ToLower().Contains(loweredSearch!)
                    || p.Source.ToLower().Contains(loweredSearch!)
                    || p.Status.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(p => p.Source == sourceFilter);
            var placeholders = await q.ToListAsync(ct);
            items.AddRange(placeholders.Select(p => new DataCenterItem(
                "ai-placeholder",
                p.Id,
                p.Title,
                p.Source,
                p.Status,
                p.StartsAt,
                p.EndsAt,
                p.Reason)));
        }

        if (ShouldLoad("reminder"))
        {
            var q = _db.Set<ReminderEntity>()
                .AsNoTracking()
                .Where(r => r.UserId == userId);
            if (hasSearch)
                q = q.Where(r => r.Title.ToLower().Contains(loweredSearch!)
                    || r.TriggerReason != null && r.TriggerReason.ToLower().Contains(loweredSearch!)
                    || r.Body != null && r.Body.ToLower().Contains(loweredSearch!)
                    || r.Status.ToLower().Contains(loweredSearch!));
            var reminders = await q.ToListAsync(ct);
            items.AddRange(reminders.Select(r => new DataCenterItem(
                "reminder",
                r.Id,
                r.Title,
                "reminder",
                r.Status,
                r.ScheduledAt,
                null,
                FirstText(r.TriggerReason, r.Body, r.RiskLevel))));
        }

        if (ShouldLoad("reminder-delivery"))
        {
            var q = _db.Set<ReminderDeliveryEntity>()
                .AsNoTracking()
                .Include(d => d.Reminder)
                .Where(d => d.UserId == userId);
            if (hasSearch)
                q = q.Where(d => d.Reminder.Title.ToLower().Contains(loweredSearch!)
                    || d.Channel.ToLower().Contains(loweredSearch!)
                    || d.Status.ToLower().Contains(loweredSearch!));
            var reminderDeliveries = await q.ToListAsync(ct);
            items.AddRange(reminderDeliveries.Select(d => new DataCenterItem(
                "reminder-delivery",
                d.Id,
                d.Reminder.Title,
                d.Channel,
                d.Status,
                d.CreatedAt,
                d.RespondedAt,
                FirstText(d.Action, d.PayloadJson))));
        }

        if (ShouldLoad("report"))
        {
            var q = _db.Set<ReportArtifactEntity>()
                .AsNoTracking()
                .Where(r => r.UserId == userId);
            if (hasSearch)
                q = q.Where(r => r.Kind.ToLower().Contains(loweredSearch!)
                    || r.Status.ToLower().Contains(loweredSearch!));
            var reports = await q.ToListAsync(ct);
            items.AddRange(reports.Select(r => new DataCenterItem(
                "report",
                r.Id,
                $"{r.Kind} report",
                "report",
                r.Status,
                r.GeneratedAt,
                null,
                FirstText(r.ContentMarkdown, r.MetricsJson))));
        }

        if (ShouldLoad("report-suggestion"))
        {
            var q = _db.Set<ReportSuggestionEntity>()
                .AsNoTracking()
                .Include(s => s.Report)
                .Where(s => s.UserId == userId);
            if (hasSearch)
                q = q.Where(s => s.Action.ToLower().Contains(loweredSearch!)
                    || s.Status.ToLower().Contains(loweredSearch!));
            var reportSuggestions = await q.ToListAsync(ct);
            items.AddRange(reportSuggestions.Select(s => new DataCenterItem(
                "report-suggestion",
                s.Id,
                s.Action,
                "report",
                s.Status,
                s.CreatedAt,
                s.UpdatedAt,
                FirstText(s.Summary, s.PayloadJson))));
        }

        if (ShouldLoad("sync-connection"))
        {
            var q = _db.Set<OutlookConnectionEntity>()
                .AsNoTracking()
                .Where(c => c.UserId == userId);
            if (hasSearch)
                q = q.Where(c => c.Provider.ToLower().Contains(loweredSearch!)
                    || c.Status.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(c => c.Provider == sourceFilter);
            var outlookConnections = await q.ToListAsync(ct);
            items.AddRange(outlookConnections.Select(c => new DataCenterItem(
                "sync-connection",
                c.Id,
                c.Provider,
                c.Provider,
                c.Status,
                c.LastSyncedAt ?? c.CreatedAt,
                c.AccessTokenExpiresAt,
                FirstText(c.TokenHealth, c.LastError))));
        }

        if (ShouldLoad("sync-batch"))
        {
            var q = _db.Set<OutlookSyncBatchEntity>()
                .AsNoTracking()
                .Where(b => b.UserId == userId);
            if (hasSearch)
                q = q.Where(b => b.Provider.ToLower().Contains(loweredSearch!)
                    || b.Status.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(b => b.Provider == sourceFilter);
            var syncBatches = await q.ToListAsync(ct);
            items.AddRange(syncBatches.Select(b => new DataCenterItem(
                "sync-batch",
                b.Id,
                $"{b.Provider} sync batch",
                b.Provider,
                b.Status,
                b.StartedAt,
                b.FinishedAt,
                $"read={b.ReadCount}; created={b.CreatedCount}; updated={b.UpdatedCount}; conflicts={b.ConflictCount}; failures={b.FailureCount}")));
        }

        if (ShouldLoad("sync-conflict"))
        {
            var q = _db.Set<SyncConflictEntity>()
                .AsNoTracking()
                .Where(c => c.UserId == userId);
            if (hasSearch)
                q = q.Where(c => c.ConflictKind.ToLower().Contains(loweredSearch!)
                    || c.Provider.ToLower().Contains(loweredSearch!)
                    || c.Status.ToLower().Contains(loweredSearch!));
            if (hasSource)
                q = q.Where(c => c.Provider == sourceFilter);
            var syncConflicts = await q.ToListAsync(ct);
            items.AddRange(syncConflicts.Select(c => new DataCenterItem(
                "sync-conflict",
                c.Id,
                c.ConflictKind,
                c.Provider,
                c.Status,
                c.CreatedAt,
                c.UpdatedAt,
                FirstText(
                    AuditSnapshotSanitizer.SanitizeJson(c.PimSnapshotJson),
                    AuditSnapshotSanitizer.SanitizeJson(c.ExternalSnapshotJson)))));
        }

        if (ShouldLoad("audit-version"))
        {
            var q = _db.Set<AuditVersionEntity>()
                .AsNoTracking()
                .Where(v => v.UserId == userId);
            if (hasSource)
                q = q.Where(v => v.Source == sourceFilter);
            var auditVersions = await q.ToListAsync(ct);
            items.AddRange(auditVersions.Select(v => new DataCenterItem(
                "audit-version",
                v.Id,
                $"{v.ObjectType} audit version",
                v.Source,
                "recorded",
                v.CreatedAt,
                null,
                FirstText(
                    AuditSnapshotSanitizer.SanitizeJson(v.ChangedFieldsJson),
                    AuditSnapshotSanitizer.SanitizeJson(v.AfterJson),
                    AuditSnapshotSanitizer.SanitizeJson(v.BeforeJson)))));
        }

        if (ShouldLoad("confirmation"))
        {
            var q = _db.OperationConfirmations
                .AsNoTracking()
                .Where(c => c.RequestedByUserId == null || c.RequestedByUserId == userId);
            if (pendingOnly)
            {
                var now = DateTimeOffset.UtcNow;
                q = q.Where(c => PendingStatuses.Contains(c.Status) && (c.ExpiresAt == null || c.ExpiresAt > now));
            }
            else if (hasSearch)
            {
                q = q.Where(c => c.OperationType.ToLower().Contains(loweredSearch!)
                    || c.Source.ToLower().Contains(loweredSearch!)
                    || c.Status.ToLower().Contains(loweredSearch!));
            }
            if (hasSource && !pendingOnly)
                q = q.Where(c => c.Source == sourceFilter);
            var confirmations = await q.ToListAsync(ct);
            items.AddRange(confirmations.Select(c => new DataCenterItem(
                "confirmation",
                c.Id,
                c.OperationType,
                c.Source,
                c.Status,
                c.CreatedAt,
                c.ExpiresAt,
                c.Summary)));
        }

        if (ShouldLoad("recycle-bin"))
        {
            var deletedCalendars = await _db.Set<CalendarEntity>()
                .IgnoreQueryFilters()
                .AsNoTracking()
                .Where(c => c.UserId == userId && c.DeletedAt != null)
                .ToListAsync(ct);
            items.AddRange(deletedCalendars.Select(c => new DataCenterItem(
                "recycle-bin",
                c.Id,
                c.Name,
                "manual",
                "deleted",
                c.DeletedAt,
                null,
                c.Kind == "task" ? "Deleted task book" : "Deleted calendar")));

            var deletedEvents = await _db.Set<EventEntity>()
                .IgnoreQueryFilters()
                .AsNoTracking()
                .Include(e => e.Calendar)
                .Where(e => e.DeletedAt != null && e.Calendar.UserId == userId)
                .ToListAsync(ct);
            items.AddRange(deletedEvents.Select(e => new DataCenterItem(
                "recycle-bin",
                e.Id,
                e.Title,
                e.Source,
                "deleted",
                e.DtStart,
                e.DtEnd,
                FirstText(BuildEventSummary(e), $"Deleted at {e.DeletedAt:O}"))));

            var deletedTasks = await _db.Set<TaskEntity>()
                .IgnoreQueryFilters()
                .AsNoTracking()
                .Include(t => t.Calendar)
                .Where(t => t.DeletedAt != null && t.UserId == userId)
                .ToListAsync(ct);
            items.AddRange(deletedTasks.Select(t => new DataCenterItem(
                "recycle-bin",
                t.Id,
                t.Title,
                "manual",
                "deleted",
                t.DtStart,
                t.PlannedEnd ?? t.Due,
                FirstText(t.Description, t.Calendar?.Name, $"Deleted at {t.DeletedAt:O}"))));
        }

        var filtered = ApplyFilters(items, request)
            .OrderBy(i => i.StartsAt ?? DateTimeOffset.MaxValue)
            .ThenBy(i => i.EndsAt ?? DateTimeOffset.MaxValue)
            .ThenBy(i => i.ObjectType)
            .ThenBy(i => i.Title)
            .ThenBy(i => i.ObjectId)
            .ToList();

        var totalCount = filtered.Count;
        var pageItems = filtered
            .Skip((page - 1) * pageSize)
            .Take(pageSize)
            .ToList();

        return new DataCenterQueryResponse(pageItems, page, pageSize, totalCount);
    }

    private static IEnumerable<DataCenterItem> ApplyFilters(
        IEnumerable<DataCenterItem> items,
        DataCenterQueryRequest request)
    {
        var filtered = items;

        if (!string.IsNullOrWhiteSpace(request.Search))
        {
            var term = request.Search.Trim();
            filtered = filtered.Where(i =>
                ContainsIgnoreCase(i.Title, term)
                || ContainsIgnoreCase(i.Summary, term)
                || ContainsIgnoreCase(i.Source, term)
                || ContainsIgnoreCase(i.Status, term));
        }

        if (!string.IsNullOrWhiteSpace(request.ObjectType))
        {
            var objectType = request.ObjectType.Trim();
            filtered = filtered.Where(i => string.Equals(i.ObjectType, objectType, StringComparison.OrdinalIgnoreCase));
        }

        if (!string.IsNullOrWhiteSpace(request.Source))
        {
            var source = request.Source.Trim();
            filtered = filtered.Where(i => string.Equals(i.Source, source, StringComparison.OrdinalIgnoreCase));
        }

        if (request.PendingOnly)
        {
            var now = DateTimeOffset.UtcNow;
            filtered = filtered.Where(i =>
                string.Equals(i.ObjectType, "confirmation", StringComparison.OrdinalIgnoreCase)
                && PendingStatuses.Contains(i.Status)
                && (!i.EndsAt.HasValue || i.EndsAt.Value > now));
        }

        return filtered;
    }

    private static bool ContainsIgnoreCase(string? value, string term)
        => value?.Contains(term, StringComparison.OrdinalIgnoreCase) ?? false;

    private static string FirstText(params string?[] values)
        => values.FirstOrDefault(value => !string.IsNullOrWhiteSpace(value)) ?? string.Empty;

    private static string BuildEventSummary(EventEntity e)
    {
        var parts = new List<string>();
        Add(e.Description);
        Add(e.Location);
        Add(e.Calendar.Name);
        if (e.Source.StartsWith("outlook", StringComparison.OrdinalIgnoreCase))
        {
            Add("Outlook event (external identifiers hidden)");
        }

        return string.Join(" | ", parts);

        void Add(string? value)
        {
            if (!string.IsNullOrWhiteSpace(value))
                parts.Add(value);
        }
    }
}
