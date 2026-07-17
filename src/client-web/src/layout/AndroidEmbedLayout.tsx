import { Component, type ReactNode } from 'react';

interface State {
  hasError: boolean;
}

export default class AndroidEmbedLayout extends Component<{ children: ReactNode }, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(): State {
    return { hasError: true };
  }

  componentDidCatch() {
    // error captured; state already set by getDerivedStateFromError
  }

  render() {
    if (this.state.hasError) {
      return (
        <div
          role="alert"
          className="h-screen w-full overflow-y-auto bg-white flex flex-col items-center justify-center gap-4 p-4"
          style={{
            paddingTop: 'env(safe-area-inset-top, 0px)',
            paddingRight: 'env(safe-area-inset-right, 0px)',
            paddingBottom: 'env(safe-area-inset-bottom, 0px)',
            paddingLeft: 'env(safe-area-inset-left, 0px)',
          }}
        >
          <h2 className="text-lg font-semibold text-gray-800">页面暂时无法显示</h2>
          <button
            type="button"
            className="rounded bg-blue-600 px-4 py-2 text-white text-sm"
            onClick={() => window.location.reload()}
          >
            重新加载
          </button>
        </div>
      );
    }

    return (
      <div
        className="h-screen w-full overflow-y-auto bg-white"
        style={{
          paddingTop: 'env(safe-area-inset-top, 0px)',
          paddingRight: 'env(safe-area-inset-right, 0px)',
          paddingBottom: 'env(safe-area-inset-bottom, 0px)',
          paddingLeft: 'env(safe-area-inset-left, 0px)',
        }}
      >
        {this.props.children}
      </div>
    );
  }
}
