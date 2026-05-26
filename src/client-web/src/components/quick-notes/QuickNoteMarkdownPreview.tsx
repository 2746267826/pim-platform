import type { QuickNoteAttachment } from '../../types';
import QuickNoteEditor from './QuickNoteEditor';

export interface QuickNoteMarkdownPreviewProps {
  markdown: string;
  attachments?: QuickNoteAttachment[];
  minHeight?: number;
}

export default function QuickNoteMarkdownPreview({
  markdown,
  attachments = [],
  minHeight = 120,
}: QuickNoteMarkdownPreviewProps) {
  return (
    <div className="space-y-3">
      <QuickNoteEditor value={markdown || ' '} minHeight={minHeight} readOnly />
      {attachments.length > 0 && (
        <div className="border-t border-slate-100 pt-3">
          <h3 className="text-xs font-semibold text-slate-500">引用附件</h3>
          <ul className="mt-2 space-y-1">
            {attachments.map(attachment => (
              <li key={attachment.id}>
                <a
                  className="break-all text-sm font-medium text-blue-600 hover:underline"
                  href={attachment.downloadUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  {attachment.fileName}
                </a>
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}
