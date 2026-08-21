export interface NavItem {
  label: string;
  path: string;
  short: string;
}

export const NAV_ITEMS: NavItem[] = [
  { label: '今日', path: '/today', short: '今' },
  { label: '日历', path: '/calendar', short: '历' },
  { label: '工作台', path: '/workbench', short: '工' },
  { label: '确认', path: '/confirmations', short: '确' },
  { label: '数据中心', path: '/data-center', short: '数' },
  { label: '提醒', path: '/reminders', short: '提' },
  { label: '报告', path: '/reports', short: '报' },
  { label: '习惯', path: '/habits', short: '习' },
  { label: '快速记录', path: '/quick-notes', short: '记' },
  { label: '文件', path: '/files', short: '文' },
  { label: '任务', path: '/tasks', short: '任' },
  { label: '电脑记录', path: '/pc-tracker', short: '电' },
  { label: '手机记录', path: '/mobile-records', short: '机' },
  { label: '历史位置', path: '/location-history', short: '位' },
  { label: '应用知识库', path: '/app-knowledge-base', short: '库' },
  { label: '状态信息', path: '/status', short: '态' },
  { label: '设置', path: '/settings', short: '设' },
];
