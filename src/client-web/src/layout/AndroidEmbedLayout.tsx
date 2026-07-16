import type { ReactNode } from 'react';

export default function AndroidEmbedLayout({ children }: { children: ReactNode }) {
  return (
    <div className="h-screen w-full overflow-y-auto bg-white">
      {children}
    </div>
  );
}
