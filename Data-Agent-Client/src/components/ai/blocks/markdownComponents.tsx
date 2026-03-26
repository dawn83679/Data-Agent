import React from 'react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { SqlCodeBlock } from '../../common/SqlCodeBlock';
import { cn } from '../../../lib/utils';
import { LightDataTable, type LightDataTableCell } from './LightDataTable';

export const markdownRemarkPlugins = [remarkGfm];

type MarkdownTableNode = {
  tagName?: string;
  value?: string;
  children?: MarkdownTableNode[];
};

interface MarkdownComponentsOptions {
  enableTableActions?: boolean;
}

interface ParsedMarkdownTable {
  headers: string[];
  rows: string[][];
}

function normalizeCellText(value: string): string {
  return value.replace(/\s+/g, ' ').trim();
}

function extractTextFromTableNode(node: MarkdownTableNode | null | undefined): string {
  if (!node) {
    return '';
  }
  const ownValue = typeof node.value === 'string' ? node.value : '';
  const childValue = Array.isArray(node.children)
    ? node.children.map((child) => extractTextFromTableNode(child)).join('')
    : '';
  return normalizeCellText(ownValue + childValue);
}

function collectRowsFromSection(node: MarkdownTableNode | null | undefined): string[][] {
  if (!node || !Array.isArray(node.children)) {
    return [];
  }

  return node.children
    .filter((child) => child?.tagName === 'tr')
    .map((rowNode) => {
      const cells = Array.isArray(rowNode.children) ? rowNode.children : [];
      return cells
        .filter((cellNode) => cellNode?.tagName === 'th' || cellNode?.tagName === 'td')
        .map((cellNode) => extractTextFromTableNode(cellNode));
    })
    .filter((row) => row.length > 0);
}

function parseMarkdownTable(node: MarkdownTableNode | null | undefined): ParsedMarkdownTable {
  if (!node || !Array.isArray(node.children)) {
    return { headers: [], rows: [] };
  }

  const headerSection = node.children.find((child) => child?.tagName === 'thead');
  const bodySection = node.children.find((child) => child?.tagName === 'tbody');
  const directRows = node.children.filter((child) => child?.tagName === 'tr');

  const headerRows = collectRowsFromSection(headerSection);
  const bodyRows = collectRowsFromSection(bodySection);
  const fallbackRows = directRows.length > 0
    ? directRows.map((rowNode) => {
        const cells = Array.isArray(rowNode.children) ? rowNode.children : [];
        return cells
          .filter((cellNode) => cellNode?.tagName === 'th' || cellNode?.tagName === 'td')
          .map((cellNode) => extractTextFromTableNode(cellNode));
      }).filter((row) => row.length > 0)
    : [];

  const allRows = bodyRows.length > 0 || headerRows.length > 0 ? [...headerRows, ...bodyRows] : fallbackRows;
  if (allRows.length === 0) {
    return { headers: [], rows: [] };
  }

  const headers = headerRows[0] ?? allRows[0];
  const rows = headerRows.length > 0 ? bodyRows : allRows.slice(1);
  return { headers, rows };
}

export function useMarkdownComponents(
  options: MarkdownComponentsOptions = {}
): React.ComponentProps<typeof ReactMarkdown>['components'] {
  const { enableTableActions = true } = options;

  return {
    code({ className, children, ...props }) {
      const match = /language-(\w+)/.exec(className || '');
      const language = match ? match[1] : '';
      const isInline = !match;
      if (!isInline) {
        const code = String(children).replace(/\n$/, '');
        return <SqlCodeBlock variant="block" sql={code} language={language || 'text'} />;
      }
      return (
        <code className={cn('bg-accent/50 px-1 rounded text-[11px] font-mono', className)} {...props}>
          {children}
        </code>
      );
    },
    p: ({ children }: { children?: React.ReactNode }) => (
      <p className="mb-2 last:mb-0 leading-relaxed">{children}</p>
    ),
    ul: ({ children }: { children?: React.ReactNode }) => (
      <ul className="list-disc pl-4 mb-2">{children}</ul>
    ),
    ol: ({ children }: { children?: React.ReactNode }) => (
      <ol className="list-decimal pl-4 mb-2">{children}</ol>
    ),
    table: ({ node, children }: { node?: MarkdownTableNode; children?: React.ReactNode }) => {
      const tableData = parseMarkdownTable(node);
      return (
        <LightDataTable
          headers={tableData.headers}
          rows={tableData.rows.map((row) =>
            row.map<LightDataTableCell>((cell) => ({ text: cell, title: cell }))
          )}
          enableActions={enableTableActions}
        >
        {children}
        </LightDataTable>
      );
    },
    thead: ({ children }: { children?: React.ReactNode }) => (
      <thead className="theme-bg-hover">{children}</thead>
    ),
    tbody: ({ children }: { children?: React.ReactNode }) => <tbody>{children}</tbody>,
    tr: ({ children }: { children?: React.ReactNode }) => (
      <tr className="border-b theme-border">{children}</tr>
    ),
    th: ({ children }: { children?: React.ReactNode }) => (
      <th className="border theme-border px-2 py-1.5 text-left font-medium theme-text-secondary">
        {children}
      </th>
    ),
    td: ({ children }: { children?: React.ReactNode }) => (
      <td className="border theme-border px-2 py-1.5 text-left">{children}</td>
    ),
    img: ({ src, alt }: { src?: string; alt?: string }) => {
      // Filter out Aliyun OSS images to avoid duplicated rendering.
      if (src && src.includes('alipayobjects.com')) {
        return null;
      }
      return <img src={src} alt={alt} className="max-w-full h-auto rounded my-2" />;
    },
  };
}
