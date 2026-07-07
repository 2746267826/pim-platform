import type {
  MobileAppCatalogOverride,
  MobileAppCategoryRule,
  MobileAppCategoryRuleUpsertRequest,
} from '../../api/mobile';
import { MOBILE_LIFE_CATEGORIES } from '../../api/mobile';

export interface MobileAppCatalogManagerProps {
  overrides: MobileAppCatalogOverride[];
  rules: MobileAppCategoryRule[];
  isLoading?: boolean;
  isSaving?: boolean;
  onSaveOverride: (override: MobileAppCatalogOverride) => void;
  onDeleteOverride: (packageName: string) => void;
  onSaveRule: (rule: MobileAppCategoryRule | MobileAppCategoryRuleUpsertRequest) => void;
  onDeleteRule: (id: string) => void;
}

function readString(formData: FormData, key: string) {
  const value = formData.get(key);
  return typeof value === 'string' ? value.trim() : '';
}

export default function MobileAppCatalogManager({
  overrides,
  rules,
  isLoading = false,
  isSaving = false,
  onSaveOverride,
  onDeleteOverride,
  onSaveRule,
  onDeleteRule,
}: MobileAppCatalogManagerProps) {
  function handleOverrideSubmit(event: { preventDefault: () => void; currentTarget: HTMLFormElement }) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const packageName = readString(formData, 'packageName').toLowerCase();
    if (!packageName) return;

    onSaveOverride({
      packageName,
      displayNameOverride: readString(formData, 'displayNameOverride') || null,
      lifeCategory: readString(formData, 'lifeCategory') || MOBILE_LIFE_CATEGORIES[15],
      isSystemNoise: formData.has('isSystemNoise'),
      hideShortEvents: formData.has('hideShortEvents'),
    });
    event.currentTarget.reset();
  }

  function handleRuleSubmit(event: { preventDefault: () => void; currentTarget: HTMLFormElement }) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const pattern = readString(formData, 'pattern').toLowerCase();
    if (!pattern) return;

    onSaveRule({
      ruleType: readString(formData, 'ruleType') || 'package-prefix',
      pattern,
      lifeCategory: readString(formData, 'lifeCategory') || MOBILE_LIFE_CATEGORIES[15],
      priority: Number(readString(formData, 'priority')) || 100,
      isEnabled: formData.has('isEnabled'),
      displayNameOverride: readString(formData, 'displayNameOverride') || null,
      isSystemNoise: formData.has('isSystemNoise'),
    });
    event.currentTarget.reset();
  }

  return (
    <section className="rounded-md border border-slate-200 bg-white p-4">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold text-slate-950">应用管理</h2>
        <span className="text-xs text-slate-500">{isSaving ? '保存中' : '全局生效'}</span>
      </div>

      <div className="mt-4 grid gap-4 lg:grid-cols-2">
        <div>
          <h3 className="text-xs font-semibold text-slate-500">手动修正</h3>
          <form
            data-action="create-override"
            onSubmit={handleOverrideSubmit}
            className="mt-2 grid grid-cols-1 gap-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-xs"
          >
            <input
              name="packageName"
              aria-label="应用包名"
              placeholder="com.example.app"
              className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
            />
            <input
              name="displayNameOverride"
              aria-label="显示名称"
              placeholder="显示名称"
              className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
            />
            <select
              name="lifeCategory"
              aria-label="修正分类"
              defaultValue={MOBILE_LIFE_CATEGORIES[15]}
              className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
            >
              {MOBILE_LIFE_CATEGORIES.map(category => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>
            <div className="flex flex-wrap gap-3 text-slate-600">
              <label className="inline-flex items-center gap-1">
                <input name="isSystemNoise" type="checkbox" />
                系统噪声
              </label>
              <label className="inline-flex items-center gap-1">
                <input name="hideShortEvents" type="checkbox" defaultChecked />
                隐藏短事件
              </label>
            </div>
            <button
              type="submit"
              className="h-8 rounded-md border border-slate-200 bg-white px-2 text-slate-700 hover:bg-slate-50"
            >
              新增修正
            </button>
          </form>
          <div className="mt-2 space-y-2">
            {overrides.map(override => (
              <div key={override.packageName} className="grid grid-cols-[1fr_auto_auto] items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm">
                <span className="min-w-0">
                  <span className="block truncate font-medium text-slate-800">{override.displayNameOverride ?? override.packageName}</span>
                  <span className="block truncate text-xs text-slate-500">{override.packageName} · {override.lifeCategory}</span>
                </span>
                <button
                  type="button"
                  data-action="save-override"
                  data-package-name={override.packageName}
                  onClick={() => onSaveOverride(override)}
                  className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-700 hover:bg-slate-50"
                >
                  保存
                </button>
                <button
                  type="button"
                  data-action="delete-override"
                  data-package-name={override.packageName}
                  onClick={() => onDeleteOverride(override.packageName)}
                  className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-700 hover:bg-slate-50"
                >
                  删除
                </button>
              </div>
            ))}
            {overrides.length === 0 && <p className="text-xs text-slate-500">暂无手动修正</p>}
          </div>
        </div>

        <div>
          <h3 className="text-xs font-semibold text-slate-500">批量规则</h3>
          <form
            data-action="create-rule"
            onSubmit={handleRuleSubmit}
            className="mt-2 grid grid-cols-1 gap-2 rounded-md border border-slate-200 bg-slate-50 p-3 text-xs"
          >
            <div className="grid grid-cols-2 gap-2">
              <select
                name="ruleType"
                aria-label="规则类型"
                defaultValue="package-prefix"
                className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
              >
                <option value="package-exact">包名精确</option>
                <option value="package-prefix">包名前缀</option>
                <option value="package-keyword">包名关键词</option>
                <option value="display-keyword">名称关键词</option>
                <option value="keyword">包名或名称关键词</option>
              </select>
              <input
                name="priority"
                aria-label="优先级"
                type="number"
                defaultValue={100}
                className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
              />
            </div>
            <input
              name="pattern"
              aria-label="匹配模式"
              placeholder="com.tencent."
              className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
            />
            <input
              name="displayNameOverride"
              aria-label="规则显示名称"
              placeholder="可选显示名称"
              className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
            />
            <select
              name="lifeCategory"
              aria-label="规则分类"
              defaultValue={MOBILE_LIFE_CATEGORIES[0]}
              className="h-9 rounded-md border border-slate-200 bg-white px-2 text-slate-900"
            >
              {MOBILE_LIFE_CATEGORIES.map(category => (
                <option key={category} value={category}>{category}</option>
              ))}
            </select>
            <div className="flex flex-wrap gap-3 text-slate-600">
              <label className="inline-flex items-center gap-1">
                <input name="isEnabled" type="checkbox" defaultChecked />
                启用
              </label>
              <label className="inline-flex items-center gap-1">
                <input name="isSystemNoise" type="checkbox" />
                系统噪声
              </label>
            </div>
            <button
              type="submit"
              className="h-8 rounded-md border border-slate-200 bg-white px-2 text-slate-700 hover:bg-slate-50"
            >
              新增规则
            </button>
          </form>
          <div className="mt-2 space-y-2">
            {rules.map(rule => (
              <div key={rule.id} className="grid grid-cols-[1fr_auto_auto] items-center gap-2 rounded-md border border-slate-200 px-3 py-2 text-sm">
                <span className="min-w-0">
                  <span className="block truncate font-medium text-slate-800">{rule.pattern}</span>
                  <span className="block truncate text-xs text-slate-500">
                    {rule.ruleType} · {rule.lifeCategory} · 优先级 {rule.priority}
                    {rule.displayNameOverride ? ` · ${rule.displayNameOverride}` : ''}
                    {rule.isSystemNoise ? ' · 系统噪声' : ''}
                  </span>
                </span>
                <button
                  type="button"
                  data-action="save-rule"
                  data-rule-id={rule.id}
                  onClick={() => onSaveRule(rule)}
                  className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-700 hover:bg-slate-50"
                >
                  保存
                </button>
                <button
                  type="button"
                  data-action="delete-rule"
                  data-rule-id={rule.id}
                  onClick={() => onDeleteRule(rule.id)}
                  className="rounded border border-slate-200 px-2 py-1 text-xs text-slate-700 hover:bg-slate-50"
                >
                  删除
                </button>
              </div>
            ))}
            {rules.length === 0 && <p className="text-xs text-slate-500">暂无批量规则</p>}
          </div>
        </div>
      </div>

      <label className="mt-4 block text-xs font-medium text-slate-500">
        分类参考
        <select className="mt-1 h-9 rounded-md border border-slate-200 bg-white px-3 text-sm text-slate-900">
          {MOBILE_LIFE_CATEGORIES.map(category => (
            <option key={category} value={category}>{category}</option>
          ))}
        </select>
      </label>
      {isLoading && <p className="mt-3 text-xs text-slate-500">正在加载应用管理</p>}
    </section>
  );
}
