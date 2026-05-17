import PcDetailQueryPanel from '../components/pc-tracker/PcDetailQueryPanel';

export default function PcDetailQueryPage() {
  return (
    <div className="max-w-5xl mx-auto">
      <h2 className="text-xl font-bold mb-6">PC记录 详细数据</h2>
      <div className="bg-white rounded-xl shadow-sm border p-5">
        <PcDetailQueryPanel />
      </div>
    </div>
  );
}
