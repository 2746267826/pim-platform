using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using Xunit;

namespace Pim.UnitTests.Invariants;

/// <summary>
/// 真库凭证与目标库的"守门"断言（评审要求：把靠人提醒的约定变成自动化门禁）。
/// <para>本仓库已经三次出现"测试里硬编码真库口令 / 静默跳过真库用例"的问题，因此这里用断言固定两条规则：</para>
/// <list type="number">
///   <item><description>测试源码里不得出现镜像库口令字面量（历史遗留文件进白名单，且白名单只减不增）。</description></item>
///   <item><description>任何地方都不得出现指向 <c>pim_prod</c> 的数据库连接串（AGENTS.md 硬规则）。</description></item>
/// </list>
/// </summary>
public class RealDbCredentialPolicyTests
{
    /// <summary>本地镜像库口令：新代码一律走 <c>PIM_TEST_CONN</c> 环境变量，不得写进源码。</summary>
    private const string MirrorPasswordLiteral = "62f0a50bb963bb648f8e400399def95a";

    /// <summary>
    /// 遗留文件白名单：这些文件在本门禁建立之前就已内置口令，验证方式仍是真库回放。
    /// 迁移到 <c>PIM_TEST_CONN</c> 后应从白名单移除；<b>只减不增</b>，新增文件命中即失败。
    /// </summary>
    private static readonly IReadOnlyDictionary<string, string> LegacyPasswordAllowlist = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
    {
        ["tests/Pim.UnitTests/Harness/RealDb/PimDbFixture.cs"] = "真库回放 Fixture（#255 时期引入），待迁移到 PIM_TEST_CONN",
        ["tests/Pim.UnitTests/Harness/RealDb/PcTrackerDedupRealDbTests.cs"] = "PC 去重真库用例，待迁移到 PIM_TEST_CONN",
        ["tests/Pim.UnitTests/Harness/Generators/RealDataSampler.cs"] = "真实数据采样器，待迁移到 PIM_TEST_CONN",
        ["tests/Pim.UnitTests/Mobile/DeviceMergeRealDbTests.cs"] = "设备合并真库用例，待迁移到 PIM_TEST_CONN"
    };

    [Fact]
    public void NoTestFileEmbedsTheMirrorDatabasePasswordExceptDocumentedLegacyFiles()
    {
        var sources = EnumerateTestSources().ToList();

        var offenders = sources
            // 本文件本身就是这条规则的实现，必须持有口令字面量才能检查它，故排除自身。
            .Where(file => !file.RelativePath.EndsWith("Invariants/RealDbCredentialPolicyTests.cs", StringComparison.OrdinalIgnoreCase))
            .Where(file => file.Content.Contains(MirrorPasswordLiteral, StringComparison.Ordinal))
            .Select(file => file.RelativePath)
            .Where(path => !LegacyPasswordAllowlist.ContainsKey(path))
            .ToList();

        Assert.True(
            offenders.Count == 0,
            "以下测试文件内置了镜像库口令，应改为读取环境变量（例如通过 Harness/RealDb/RealDbTestConnection）："
            + Environment.NewLine + string.Join(Environment.NewLine, offenders));

        // 白名单里的文件也必须真的还在被扫描到，否则白名单会烂成"永久豁免"。
        Assert.All(LegacyPasswordAllowlist.Keys, path =>
            Assert.True(
                sources.Any(file => string.Equals(file.RelativePath, path, StringComparison.OrdinalIgnoreCase)),
                $"{path} 已不存在，请从白名单移除"));
    }

    [Fact]
    public void NoSourceTargetsTheProductionDatabase()
    {
        var productionConnection = new Regex(
            @"(Host|Server)\s*=[^;""]*;[^""]*Database\s*=\s*pim_prod|Database\s*=\s*pim_prod[^_a-zA-Z0-9]",
            RegexOptions.IgnoreCase);

        var offenders = EnumerateSources()
            .Where(file => productionConnection.IsMatch(file.Content))
            .Select(file => file.RelativePath)
            .ToList();

        Assert.True(
            offenders.Count == 0,
            "AGENTS.md 硬规则：任何代码/测试都不得指向生产库 pim_prod。命中文件："
            + Environment.NewLine + string.Join(Environment.NewLine, offenders));

        // 反向验证：这条规则真的能认出生产库连接串，而不是永远返回空集。
        // 样本在运行时拼出来，免得本文件自己命中这条规则。
        var productionSample = "Host=127.0.0.1;Port=5432;Database=" + "pim_prod" + ";Username=pim";
        var mirrorSample = "Host=127.0.0.1;Port=5432;Database=" + "pim_test" + ";Username=opencode";
        Assert.True(productionConnection.IsMatch(productionSample));
        Assert.False(productionConnection.IsMatch(mirrorSample));
    }

    [Fact]
    public void RealDbTestsUseTheSharedEnvironmentBasedConnectionHelper()
    {
        var invariantsRealDbTest = EnumerateTestSources()
            .Single(file => file.RelativePath.EndsWith("Invariants/LiveDbQualityInspectionTests.cs", StringComparison.OrdinalIgnoreCase));

        // 真库用例必须显式跳过而不是静默 return，并且不得内置口令。
        Assert.Contains("[SkippableFact]", invariantsRealDbTest.Content, StringComparison.Ordinal);
        Assert.Contains("RealDbTestConnection.Require()", invariantsRealDbTest.Content, StringComparison.Ordinal);
        Assert.DoesNotContain(MirrorPasswordLiteral, invariantsRealDbTest.Content, StringComparison.Ordinal);
        Assert.DoesNotContain("PIM_LIVE_DB_CONNECTION", invariantsRealDbTest.Content, StringComparison.Ordinal);
    }

    private static IEnumerable<(string RelativePath, string Content)> EnumerateTestSources() =>
        EnumerateSources().Where(file => file.RelativePath.StartsWith("tests/", StringComparison.Ordinal));

    private static IEnumerable<(string RelativePath, string Content)> EnumerateSources()
    {
        var root = ResolveRepositoryRoot();
        foreach (var path in Directory.EnumerateFiles(root, "*.cs", SearchOption.AllDirectories))
        {
            var relative = Path.GetRelativePath(root, path).Replace('\\', '/');
            if (relative.StartsWith("bin/", StringComparison.Ordinal)
                || relative.Contains("/bin/", StringComparison.Ordinal)
                || relative.Contains("/obj/", StringComparison.Ordinal))
            {
                continue;
            }

            yield return (relative, File.ReadAllText(path));
        }
    }

    /// <summary>从测试输出的 bin 目录向上找到含 Pim.sln 的仓库根。</summary>
    private static string ResolveRepositoryRoot()
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);
        while (directory != null)
        {
            if (File.Exists(Path.Combine(directory.FullName, "Pim.sln")))
            {
                return directory.FullName;
            }

            directory = directory.Parent;
        }

        throw new InvalidOperationException("未找到仓库根目录（缺少 Pim.sln）");
    }
}
