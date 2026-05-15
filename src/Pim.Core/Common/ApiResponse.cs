namespace Pim.Core.Common;

public record ApiResponse<T>(
    int Code,
    string Message,
    T? Data,
    DateTimeOffset Timestamp
)
{
    public static ApiResponse<T> Ok(T data) =>
        new(0, "success", data, DateTimeOffset.UtcNow);

    public static ApiResponse<T> Error(int code, string message) =>
        new(code, message, default, DateTimeOffset.UtcNow);
}
