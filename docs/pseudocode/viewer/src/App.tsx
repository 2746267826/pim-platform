export default function App() {
  return (
    <div className="app-shell">
      <header className="topbar">
        <strong>PIM 伪代码工作台</strong>
        <span className="muted">scaffold</span>
      </header>
      <main className="three-pane">
        <aside className="pane pane-left">树</aside>
        <section className="pane pane-center">文档</section>
        <aside className="pane pane-right">关系</aside>
      </main>
    </div>
  );
}
