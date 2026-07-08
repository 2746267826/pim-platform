interface BeforeAfterDiffProps {
  beforeJson?: string | null;
  afterJson?: string | null;
  changedFields?: string[] | null;
}

export default function BeforeAfterDiff({ beforeJson, afterJson, changedFields }: BeforeAfterDiffProps) {
  return (
    <section className="rounded-lg border border-slate-200 bg-white p-3" aria-label="变更前后对比">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-xs font-semibold text-slate-500">变更前后</h3>
        <div className="flex flex-wrap gap-1">
          {(changedFields ?? []).map(field => (
            <span key={field} className="rounded-full bg-blue-50 px-2 py-0.5 text-[11px] font-semibold text-blue-700">
              {field}
            </span>
          ))}
        </div>
      </div>
      <div className="mt-3 grid gap-3 md:grid-cols-2">
        <pre className="max-h-52 overflow-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-100">
          {beforeJson || '{}'}
        </pre>
        <pre className="max-h-52 overflow-auto rounded-lg bg-slate-950 p-3 text-xs text-slate-100">
          {afterJson || '{}'}
        </pre>
      </div>
    </section>
  );
}
