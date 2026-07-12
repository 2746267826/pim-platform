# src/client-web/src/ui/EditorDrawer.tsx

## 元信息
- 语言：TypeScript/TSX
- 程序集或包：client-web
- 职责：UI 组件 `EditorDrawer`：交互面板/控件，展示数据并回传用户操作。
- 主要依赖：无项目内相对导入（或仅外部包）
- 被谁使用：阅读时由总控/关系图汇总；本文件边中列出 depends_on

## 函数级结构化伪代码

### getFocusableElements
#### getFocusableElements(无)
- 输入：无显式参数
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `getFocusableElements`
  2. 赋值 `drawer` = drawerRef.current
  3. 执行：if (!drawer) return [];
  4. 返回 Array.from(
  5. 执行：drawer.querySelectorAll<HTMLElement>(
  6. 执行：'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [ta
  7. 执行：).filter(element => !element.hasAttribute('aria-hidden'));
- 分支与异常：if (!drawer) return [];
- 调用：getFocusableElements、Array.from、not、filter、element.hasAttribute

### handleKeyDown
#### handleKeyDown(e: KeyboardEvent<HTMLElement>)
- 输入：e: KeyboardEvent<HTMLElement>
- 输出：见返回值/JSX/Unit
- 副作用：见步骤中的状态更新/IO/导航
- 步骤：
  1. 定义函数 `handleKeyDown`
  2. 若 (e.key === 'Escape') 则
  3. 执行：e.stopPropagation();
  4. 执行：onClose();
  5. 返回（空）
  6. 执行：if (e.key !== 'Tab') return;
  7. 赋值 `focusableElements` = getFocusableElements()
  8. 若 (focusableElements.length === 0) 则
  9. 执行：e.preventDefault();
  10. 执行：drawerRef.current?.focus();
  11. 赋值 `firstElement` = focusableElements[0]
  12. 赋值 `lastElement` = focusableElements[focusableElements.length - 1]
  13. 赋值 `activeElement` = document.activeElement
  14. 若 (e.shiftKey && (activeElement === firstElement || activeElement === drawerRef.current)) 则
  15. 执行：lastElement.focus();
  16. 执行：firstElement.focus();
- 分支与异常：if (e.key === 'Escape') {；if (e.key !== 'Tab') return;；if (focusableElements.length === 0) {；if (e.shiftKey && (activeElement === firstElement || activeElement === drawerRef.current)) {
- 调用：handleKeyDown、e.stopPropagation、onClose、getFocusableElements、e.preventDefault、focus、lastElement.focus、firstElement.focus

## 近逐行中文伪代码

1. [L3] 定义类型 `Props`
2. [L4] 执行：open: boolean;
3. [L5] 执行：title: string;
4. [L6] 执行：subtitle?: string;
5. [L7] 执行：onClose: () => void;
6. [L8] 执行：children: ReactNode;
7. [L9] 执行：footer: ReactNode;
8. [L12] 默认导出函数 `EditorDrawer`
9. [L15] 执行：subtitle,
10. [L17] 执行：children,
11. [L20] Hook `useRef` 绑定 `drawerRef`
12. [L21] Hook `usedRef` 绑定 `previouslyFocusedRef`
13. [L22] 赋值 `titleId` = useId()
14. [L24] 注册 `useEffect` 副作用
15. [L25] 执行：if (!open) return;
16. [L27] 执行：previouslyFocusedRef.current = document.activeElement instanceof HTMLElement
17. [L28] 执行：? document.activeElement
18. [L31] 赋值 `drawer` = drawerRef.current
19. [L32] 执行：drawer?.focus();
20. [L34] 返回 JSX/结构
21. [L35] 执行：previouslyFocusedRef.current?.focus();
22. [L36] 执行：previouslyFocusedRef.current = null;
23. [L40] 执行：if (!open) return null;
24. [L42] 定义函数 `getFocusableElements`
25. [L43] 赋值 `drawer` = drawerRef.current
26. [L44] 执行：if (!drawer) return [];
27. [L46] 返回 Array.from(
28. [L47] 执行：drawer.querySelectorAll<HTMLElement>(
29. [L48] 执行：'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [ta
30. [L50] 执行：).filter(element => !element.hasAttribute('aria-hidden'));
31. [L53] 定义函数 `handleKeyDown`
32. [L54] 若 (e.key === 'Escape') 则
33. [L55] 执行：e.stopPropagation();
34. [L56] 执行：onClose();
35. [L57] 返回（空）
36. [L60] 执行：if (e.key !== 'Tab') return;
37. [L62] 赋值 `focusableElements` = getFocusableElements()
38. [L63] 若 (focusableElements.length === 0) 则
39. [L64] 执行：e.preventDefault();
40. [L65] 执行：drawerRef.current?.focus();
41. [L66] 返回（空）
42. [L69] 赋值 `firstElement` = focusableElements[0]
43. [L70] 赋值 `lastElement` = focusableElements[focusableElements.length - 1]
44. [L71] 赋值 `activeElement` = document.activeElement
45. [L73] 若 (e.shiftKey && (activeElement === firstElement || activeElement === drawerRef.current)) 则
46. [L74] 执行：e.preventDefault();
47. [L75] 执行：lastElement.focus();
48. [L77] 执行：e.preventDefault();
49. [L78] 执行：firstElement.focus();
50. [L82] 返回 JSX/结构
51. [L83] 执行：<div className="fixed inset-0 z-50 flex justify-end">
52. [L85] 执行：className="absolute inset-0 bg-slate-950/20"
53. [L86] 执行：onClick={onClose}
54. [L89] 执行：ref={drawerRef}
55. [L90] 执行：role="dialog"
56. [L91] 执行：aria-modal="true"
57. [L92] 执行：aria-labelledby={titleId}
58. [L93] 执行：tabIndex={-1}
59. [L94] 执行：onKeyDown={handleKeyDown}
60. [L95] 执行：className="relative flex h-full w-full max-w-[420px] flex-col border-l border-slate-200 bg-white shadow-2xl"
61. [L97] 执行：<header className="border-b border-slate-200 px-5 py-4">
62. [L98] 执行：<div className="flex items-start justify-between gap-3">
63. [L99] 执行：<div className="min-w-0">
64. [L100] 执行：<h2 id={titleId} className="text-base font-semibold text-slate-950">{title}</h2>
65. [L101] 执行：{subtitle && <p className="mt-1 text-sm text-slate-500">{subtitle}</p>}
66. [L103] 执行：<button type="button" onClick={onClose} className="pim-button-secondary px-3 py-1.5 text-sm">
67. [L105] 执行：</button>
68. [L107] 执行：</header>
69. [L108] 执行：<div className="flex-1 overflow-auto px-5 py-4">{children}</div>
70. [L109] 执行：<footer className="flex items-center justify-between gap-3 border-t border-slate-200 px-5 py-4">
71. [L111] 执行：</footer>

## 关系边
```json
{
  "nodes": [
    {
      "id": "src/client-web/src/ui/EditorDrawer.tsx",
      "label": "EditorDrawer",
      "path": "src/client-web/src/ui/EditorDrawer.tsx",
      "doc": "docs/pseudocode/files/src/client-web/src/ui/EditorDrawer.tsx.md",
      "layer": "client-web",
      "kind": "ui"
    }
  ],
  "edges": []
}
```
