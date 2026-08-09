export interface SplitDoc {
  title: string;
  meta: string;
  functionBody: string;
  lineBody: string;
  raw: string;
}

export function splitPseudocodeSections(md: string): SplitDoc {
  const title = (md.match(/^#\s+(.+)$/m) || [, ''])[1].trim();
  const parts = md.split(/^## /m);
  let meta = '';
  let functionBody = '';
  let lineBody = '';
  for (const p of parts) {
    if (p.startsWith('元信息')) meta = p.replace(/^元信息\s*/, '').trim();
    else if (p.startsWith('函数级')) functionBody = p.replace(/^函数级结构化伪代码\s*/, '').trim();
    else if (p.startsWith('近逐行')) lineBody = p.replace(/^近逐行中文伪代码\s*/, '').trim();
  }
  return { title, meta, functionBody, lineBody, raw: md };
}
