import EventSection from './EventSection';
import type { OutlookAdditionalInfo as OutlookAdditionalInfoType } from '../../types';

interface OutlookAdditionalInfoProps {
  info?: OutlookAdditionalInfoType | null;
}

export default function OutlookAdditionalInfo({ info }: OutlookAdditionalInfoProps) {
  if (!info) return null;
  const hasVisibleGroups = (info.groups?.length ?? 0) > 0;
  if (!hasVisibleGroups && info.hiddenFieldCount <= 0) return null;

  return (
    <EventSection title="Outlook 附加信息">
      <div className="space-y-3">
        {info.groups.map(group => (
          <div key={group.key} className="min-w-0 overflow-hidden">
            <p className="text-sm font-medium text-slate-700">{group.label}</p>
            <ul className="mt-1 space-y-1">
              {group.items.map(item => (
                <li key={item.key} className="flex gap-2 text-sm leading-6 text-slate-600">
                  <span className="shrink-0 text-slate-400">{item.label}</span>
                  <span className="min-w-0 flex-1 break-words">{item.value}</span>
                </li>
              ))}
            </ul>
          </div>
        ))}
        {info.hiddenFieldCount > 0 && (
          <p className="text-xs text-slate-500">另有 {info.hiddenFieldCount} 项敏感字段已隐藏</p>
        )}
      </div>
    </EventSection>
  );
}
