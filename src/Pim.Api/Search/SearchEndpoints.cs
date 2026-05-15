using Pim.Core.Common;
using Pim.Core.Modules;

namespace Pim.Api.Search;

public static class SearchEndpoints
{
    public static void MapSearchEndpoints(this IEndpointRouteBuilder endpoints)
    {
        var group = endpoints.MapGroup("/api/v1/search")
            .RequireAuthorization();

        group.MapGet("/", async (
            string? q,
            string? type,
            int? limit,
            IEnumerable<ISearchProvider> providers,
            CancellationToken ct) =>
        {
            if (string.IsNullOrWhiteSpace(q))
                return Results.Ok(ApiResponse<PagedResult<SearchResult>>.Ok(
                    new PagedResult<SearchResult>(Array.Empty<SearchResult>(), 1, 20, 0, 0)));

            var maxLimit = Math.Min(limit ?? 20, 100);
            var typeFilter = type?.Split(',', StringSplitOptions.RemoveEmptyEntries)
                .Select(t => t.Trim().ToLowerInvariant()).ToHashSet();

            var tasks = providers
                .Where(p => typeFilter is null || typeFilter.Count == 0 ||
                            typeFilter.Contains(p.ModuleName.ToLowerInvariant()))
                .Select(p => p.SearchAsync(q, maxLimit, ct));

            var results = await Task.WhenAll(tasks);
            var merged = results.SelectMany(r => r)
                .OrderByDescending(r => r.Title.Contains(q, StringComparison.OrdinalIgnoreCase))
                .Take(maxLimit)
                .ToList();

            return Results.Ok(ApiResponse<PagedResult<SearchResult>>.Ok(
                new PagedResult<SearchResult>(merged, 1, maxLimit, merged.Count,
                    (int)Math.Ceiling(merged.Count / (double)maxLimit))));
        });
    }
}
