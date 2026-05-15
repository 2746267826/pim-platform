namespace Pim.Core.Modules;

public interface ISearchProvider
{
    string ModuleName { get; }
    Task<IReadOnlyList<SearchResult>> SearchAsync(string query, int limit, CancellationToken ct);
}

public record SearchResult(
    string ModuleName,
    string Type,
    string Id,
    string Title,
    string Snippet,
    string Url
);
