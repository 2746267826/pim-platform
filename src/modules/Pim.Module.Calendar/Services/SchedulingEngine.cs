using System.Net.Http.Json;
using System.Text.Json;
using Microsoft.EntityFrameworkCore;
using Pim.Infrastructure.Data;
using Pim.Module.Calendar.Entities;

namespace Pim.Module.Calendar.Services;

public class SchedulingEngine
{
    private readonly PimDbContext _db;
    private readonly IHttpClientFactory _httpClientFactory;
    private readonly List<ISchedulingAlgorithm> _algorithms;

    public SchedulingEngine(PimDbContext db, IHttpClientFactory httpClientFactory)
    {
        _db = db;
        _httpClientFactory = httpClientFactory;
        _algorithms = new List<ISchedulingAlgorithm>
        {
            new GreedyScheduler(),
            new CspScheduler(),
            new GeneticScheduler()
        };
    }

    public async Task<List<ScheduleSolution>> GeneratePlansAsync(
        Guid userId, List<Guid> taskIds, CancellationToken ct)
    {
        var tasks = await _db.Set<TaskEntity>()
            .Where(t => taskIds.Contains(t.Id) && t.EstimatedDuration.HasValue)
            .ToListAsync(ct);

        var events = await _db.Set<EventEntity>()
            .Where(e => e.Calendar.UserId == userId)
            .ToListAsync(ct);

        var tasksToSchedule = tasks.Select(t => new TaskToSchedule(
            t.Id, t.Title, t.Priority,
            t.EstimatedDuration ?? TimeSpan.FromHours(1),
            t.MinimumSegment, t.Due, 1.0)).ToList();

        var busySlots = events.Select(e => new BusySlot(e.DtStart, e.DtEnd)).ToList();
        var now = DateTimeOffset.UtcNow;
        var searchEnd = now.AddDays(14);

        // Get user preference weights from feedback
        var weights = await GetUserWeightsAsync(userId);

        var solutions = new List<ScheduleSolution>();
        foreach (var algo in _algorithms)
        {
            var solution = await algo.SolveAsync(
                tasksToSchedule, busySlots, now, searchEnd, weights, ct);
            if (solution is not null) solutions.Add(solution);
        }

        // If all algorithms failed, try LLM fallback
        if (!solutions.Any(s => s.Slots.Count > 0))
        {
            var llmSolution = await TryLlmFallbackAsync(
                tasksToSchedule, busySlots, now, searchEnd, ct);
            if (llmSolution is not null) solutions.Add(llmSolution);
        }

        return solutions;
    }

    private async Task<Dictionary<string, double>> GetUserWeightsAsync(Guid userId)
    {
        var feedbacks = await _db.Set<SchedulingFeedbackEntity>()
            .Where(f => f.UserId == userId)
            .OrderByDescending(f => f.CreatedAt)
            .Take(50)
            .ToListAsync();

        if (feedbacks.Count < 5)
            return new Dictionary<string, double>
            {
                ["priority"] = 0.5,
                ["coverage"] = 0.3,
                ["compactness"] = 0.2
            };

        // Simple average weight estimation from feedback
        return new Dictionary<string, double>
        {
            ["priority"] = 0.6,
            ["coverage"] = 0.25,
            ["compactness"] = 0.15
        };
    }

    private async Task<ScheduleSolution?> TryLlmFallbackAsync(
        List<TaskToSchedule> tasks,
        List<BusySlot> busy,
        DateTimeOffset start, DateTimeOffset end,
        CancellationToken ct)
    {
        var client = _httpClientFactory.CreateClient("llm");
        var prompt = BuildLlmPrompt(tasks, busy, start, end);

        var response = await client.PostAsJsonAsync("/v1/chat/completions", new
        {
            model = "gpt-4",
            messages = new[]
            {
                new { role = "system", content = "You are a task scheduling assistant. Output JSON only." },
                new { role = "user", content = prompt }
            }
        }, ct);

        if (!response.IsSuccessStatusCode) return null;

        // Parse LLM response into ScheduleSolution
        var jsonResponse = await response.Content.ReadAsStringAsync(ct);
        return ParseLlmScheduleResponse(jsonResponse);
    }

    private string BuildLlmPrompt(List<TaskToSchedule> tasks,
        List<BusySlot> busy, DateTimeOffset start, DateTimeOffset end)
    {
        var taskDescriptions = tasks.Select(t =>
            $"- {t.Title}: {t.Duration.TotalHours:F1}h, priority {t.Priority}/9, deadline {t.Deadline}");
        var busyDescriptions = busy.Select(b =>
            $"- Busy: {b.Start:yyyy-MM-dd HH:mm} to {b.End:HH:mm}");

        var jsonExample = "{\"taskIndex\": 0, \"start\": \"ISO8601\", \"end\": \"ISO8601\"}";
        return $"""
            Schedule these tasks into free time slots between {start:yyyy-MM-dd} and {end:yyyy-MM-dd}:
            Tasks:
            {string.Join("\n", taskDescriptions)}
            Busy slots:
            {string.Join("\n", busyDescriptions)}
            Return a JSON array of {jsonExample}.
            Tasks can be split into minimum 30-minute segments.
            Higher priority tasks should be scheduled earlier.
            """;
    }

    private ScheduleSolution? ParseLlmScheduleResponse(string json)
    {
        try
        {
            var slots = JsonSerializer.Deserialize<List<LlmSlot>>(json);
            return slots is null ? null : new ScheduleSolution("llm",
                slots.Select(s => new ScheduledSlot(
                    Guid.Empty, $"Task #{s.TaskIndex}",
                    DateTimeOffset.Parse(s.Start), DateTimeOffset.Parse(s.End))).ToList(),
                new Dictionary<string, double> { ["source"] = 1.0 });
        }
        catch { return null; }
    }

    private record LlmSlot(int TaskIndex, string Start, string End);
}
