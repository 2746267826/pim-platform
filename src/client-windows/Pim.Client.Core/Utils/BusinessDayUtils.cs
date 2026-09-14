namespace Pim.Client.Core.Utils;

/// <summary>
/// 业务日口径统一工具类（EPIC #254 / D-1 架构决策）：
/// - 业务日以 Asia/Shanghai（UTC+8）为基准
/// - 凌晨 04:00 起算：D = [D 04:00, D+1 04:00)（左闭右开）
/// - 00:00 - 03:59:59 的活动归属前一个日历日
/// </summary>
public static class BusinessDayUtils
{
    public const int BusinessDayStartHour = 4;
    public static readonly TimeSpan ChinaOffset = TimeSpan.FromHours(8);

    /// <summary>
    /// 获取时间戳对应的业务日（Asia/Shanghai 04:00 起算）
    /// </summary>
    public static DateOnly GetBusinessDate(DateTimeOffset ts)
    {
        var cst = ts.ToOffset(ChinaOffset);
        var date = DateOnly.FromDateTime(cst.DateTime);
        if (cst.Hour < BusinessDayStartHour)
        {
            date = date.AddDays(-1);
        }
        return date;
    }

    /// <summary>
    /// 获取时间戳对应的业务日字符串（yyyy-MM-dd）
    /// </summary>
    public static string GetBusinessDateString(DateTimeOffset ts)
    {
        return GetBusinessDate(ts).ToString("yyyy-MM-dd");
    }

    /// <summary>
    /// 获取指定业务日的起始绝对时刻（UTC）
    /// </summary>
    public static DateTimeOffset GetBusinessDayStart(DateOnly date)
    {
        var dt = new DateTime(date.Year, date.Month, date.Day, BusinessDayStartHour, 0, 0, DateTimeKind.Unspecified);
        return new DateTimeOffset(dt, ChinaOffset).ToUniversalTime();
    }

    /// <summary>
    /// 获取给定时间戳之后最近的下一个业务日分界点（下一个 04:00 UTC+8）
    /// </summary>
    public static DateTimeOffset GetNextBusinessDayStart(DateTimeOffset ts)
    {
        var curDate = GetBusinessDate(ts);
        var nextDate = curDate.AddDays(1);
        return GetBusinessDayStart(nextDate);
    }
}
