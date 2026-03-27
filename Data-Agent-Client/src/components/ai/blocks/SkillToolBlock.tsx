import { useMemo, useState } from 'react';
import { BookOpen, ChevronDown, ChevronRight } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import { useMarkdownComponents, markdownRemarkPlugins } from './markdownComponents';
import { cn } from '../../../lib/utils';
import {
  getToolCardClassName,
  TOOL_CARD_CONTENT_CLASSNAME,
  TOOL_CARD_HEADER_CLASSNAME,
  TOOL_CARD_META_CLASSNAME,
} from './toolRunStyles';

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
    <div className={getToolCardClassName(!collapsed)}>
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className={TOOL_CARD_HEADER_CLASSNAME}
      >
        {responseError ? (
          <BookOpen className="w-3.5 h-3.5 text-red-500 shrink-0" />
        ) : (
          <BookOpen className="w-3.5 h-3.5 text-blue-500 shrink-0" />
        )}
        <span className="min-w-0 flex-1 truncate text-[12px] font-medium theme-text-primary">
          {responseError ? 'Failed to load skill: ' : 'Loaded skill: '}
          {skillName}
        </span>
        <span className={cn(TOOL_CARD_META_CLASSNAME, 'shrink-0')}>
          Skill
        </span>
        <span className="shrink-0 theme-text-secondary">
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && responseData && (
        <div className={TOOL_CARD_CONTENT_CLASSNAME}>
          <div className="rounded-lg border theme-border theme-bg-secondary px-3 py-2 text-[12px] leading-relaxed overflow-x-auto">
          {skillMarkdownBody && (
            <ReactMarkdown components={markdownComponents} remarkPlugins={markdownRemarkPlugins}>
              {skillMarkdownBody}
            </ReactMarkdown>
          )}
          </div>
        </div>
      )}
    </div>
  );
}
