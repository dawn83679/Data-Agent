import { useMemo, useState } from 'react';
import { BookOpen, ChevronDown, ChevronRight } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { useMarkdownComponents, markdownRemarkPlugins } from './markdownComponents';
import { cn } from '../../../lib/utils';

export interface SkillToolBlockProps {
  skillName: string;
  responseData: string;
  responseError: boolean;
}

function stripSkillFrontmatter(raw: string): string {
  const match = raw.match(/^---\r?\n([\s\S]*?)\r?\n---\r?\n?/);
  if (!match) {
    return raw.trim();
  }

  return raw.slice(match[0].length).trim();
}

/**
 * Renders an activateSkill tool result with the response rendered as markdown.
 * Shows skill name in a collapsible header; expands to show formatted markdown content.
 */
export function SkillToolBlock({ skillName, responseData, responseError }: SkillToolBlockProps) {
  const [collapsed, setCollapsed] = useState(true);
  const markdownComponents = useMarkdownComponents();
  const skillMarkdownBody = useMemo(() => stripSkillFrontmatter(responseData), [responseData]);

  return (
    <div
      className={cn(
        'mb-2 text-xs rounded transition-colors',
        collapsed ? 'opacity-70 theme-text-secondary' : 'opacity-100 theme-text-primary'
      )}
    >
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className="w-full py-1.5 flex items-center gap-2 text-left rounded transition-colors theme-text-primary hover:bg-black/5 dark:hover:bg-white/5"
      >
        {responseError ? (
          <BookOpen className="w-3.5 h-3.5 text-red-500 shrink-0" />
        ) : (
          <BookOpen className="w-3.5 h-3.5 text-blue-500 shrink-0" />
        )}
        <span className="font-medium">
          {responseError ? 'Failed to load skill: ' : 'Loaded skill: '}
          {skillName}
        </span>
        <span className={cn('ml-auto shrink-0', collapsed ? 'opacity-60' : 'opacity-80')}>
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && responseData && (
        <div className="mt-1 px-3 py-2 rounded-md border theme-border theme-bg-secondary text-[12px] leading-relaxed overflow-x-auto">
          {skillMarkdownBody && (
            <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
              {skillMarkdownBody}
            </ReactMarkdown>
          )}
        </div>
      )}
    </div>
  );
}
