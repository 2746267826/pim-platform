import { useEffect, useMemo, useState } from 'react';
import MarkdownIt from 'markdown-it';
import { splitPseudocodeSections } from '../lib/mdSplit';
import type { DocSection } from '../lib/types';

const md = new MarkdownIt({ html: false, linkify: true, breaks: true });

export function DocViewer({
  fileId,
  section,
  onSection,
}: {
  fileId: string | null;
  section: DocSection;
  onSection: (s: DocSection) => void;
}) {
  const [raw, setRaw] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    if (!fileId) return;
    setError('');
    setRaw('');
    const url = `./docs-files/${fileId}.md`;
    fetch(url)
      .then((r) => {
        if (!r.ok) throw new Error(`无法加载 ${url}`);
        return r.text();
      })
      .then(setRaw)
      .catch((e) => setError(String(e.message || e)));
  }, [fileId]);

  const split = useMemo(() => (raw ? splitPseudocodeSections(raw) : null), [raw]);
  const body = section === 'function' ? split?.functionBody : split?.lineBody;

  if (!fileId) return <div className="muted">从左侧选择文件</div>;
  if (error) return <div className="error">{error}</div>;
  if (!split) return <div className="muted">加载中…</div>;

  return (
    <div className="doc-viewer">
      <h1 className="doc-title">{split.title || fileId}</h1>
      <pre className="doc-meta">{split.meta}</pre>
      <div className="seg">
        <button
          type="button"
          className={section === 'function' ? 'active' : ''}
          onClick={() => onSection('function')}
        >
          函数级
        </button>
        <button
          type="button"
          className={section === 'line' ? 'active' : ''}
          onClick={() => onSection('line')}
        >
          近逐行
        </button>
      </div>
      <div
        className="md-body"
        dangerouslySetInnerHTML={{ __html: md.render(body || '_（本节为空）_') }}
      />
    </div>
  );
}
