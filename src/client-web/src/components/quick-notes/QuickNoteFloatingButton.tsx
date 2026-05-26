interface QuickNoteFloatingButtonProps {
  onClick: () => void;
}

export default function QuickNoteFloatingButton({ onClick }: QuickNoteFloatingButtonProps) {
  return (
    <button
      type="button"
      aria-label="打开快速记录"
      title="打开快速记录"
      onClick={onClick}
      className="fixed bottom-5 right-5 z-40 flex h-12 w-12 items-center justify-center rounded-full bg-blue-600 text-3xl font-light leading-none text-white shadow-lg shadow-blue-900/20 transition hover:bg-blue-700 focus:outline-none focus:ring-4 focus:ring-blue-200"
    >
      +
    </button>
  );
}
