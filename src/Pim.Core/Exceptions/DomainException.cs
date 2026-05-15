namespace Pim.Core.Exceptions;

public class DomainException : Exception
{
    public int ErrorCode { get; }

    public DomainException(int errorCode, string message) : base(message)
    {
        ErrorCode = errorCode;
    }
}
