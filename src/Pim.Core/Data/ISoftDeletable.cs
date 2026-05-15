namespace Pim.Core.Data;

public interface ISoftDeletable
{
    DateTimeOffset? DeletedAt { get; set; }
}
