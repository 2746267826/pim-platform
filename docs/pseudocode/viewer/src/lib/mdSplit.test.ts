import { describe, it, expect } from 'vitest';
import { splitPseudocodeSections } from './mdSplit';

describe('splitPseudocodeSections', () => {
  it('splits function and line sections', () => {
    const md = `# a.cs

## 元信息
- 职责：x

## 函数级结构化伪代码
### Foo
- 步骤：1

## 近逐行中文伪代码
1. 做 A

## 关系边
\`\`\`json
{}\`\`\`
`;
    const s = splitPseudocodeSections(md);
    expect(s.meta).toContain('职责');
    expect(s.functionBody).toContain('Foo');
    expect(s.lineBody).toContain('做 A');
  });
});
