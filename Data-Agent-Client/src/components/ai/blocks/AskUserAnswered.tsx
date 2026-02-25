import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { MessageCircle, ChevronDown, ChevronRight, Tag, Check } from 'lucide-react';
import type { SingleQuestion } from './askUserQuestionTypes';
import { I18N_KEYS } from '../../../constants/i18nKeys';

export interface AskUserAnsweredProps {
  questions: SingleQuestion[];
  answer: string;
}

interface ParsedAnswer {
  question: string;
  answer: string;
}

/**
 * Parse multi-question answer format: "q1"="a1", "q2"="a2"
 */
function parseMultiQuestionAnswer(answer: string): ParsedAnswer[] {
  const regex = /"([^"]*)"\s*=\s*"([^"]*)"/g;
  const matches: ParsedAnswer[] = [];
  let match;

  while ((match = regex.exec(answer)) !== null) {
    matches.push({
      question: match[1],
      answer: match[2],
    });
  }

  return matches;
}

/**
 * Renders an answered AskUserQuestion in collapsed/expandable format.
 * Supports both single and multiple questions.
 */
export function AskUserAnswered({ questions, answer }: AskUserAnsweredProps) {
  const { t } = useTranslation();
  const [collapsed, setCollapsed] = useState(true);

  const truncateText = (text: string, maxLength: number): string => {
    if (text.length <= maxLength) return text;
    return text.slice(0, maxLength) + '...';
  };

  // Try to parse as multi-question answer
  const parsedAnswers = parseMultiQuestionAnswer(answer);
  const isMultiQuestion = parsedAnswers.length > 0 && questions.length > 1;

  // Create summary for collapsed view
  const summary = isMultiQuestion
    ? `${parsedAnswers.length} ${t(I18N_KEYS.AI.ASK_USER_QUESTION.LABEL)}${parsedAnswers.length > 1 ? 's' : ''} answered`
    : `${truncateText(questions[0]?.question || '', 40)} — ${answer}`;

  // Match answers to questions
  const questionAnswerPairs = questions.map((q) => {
    if (isMultiQuestion) {
      // Multi-question: find matching parsed answer
      const parsed = parsedAnswers.find((pa) => pa.question === q.question);
      const answerText = parsed?.answer || '';
      const answerParts = answerText.split(',').map((s) => s.trim()).filter((s) => s !== '');
      return { question: q, answerParts };
    } else {
      // Single question: use full answer
      const answerParts = answer.split(',').map((s) => s.trim()).filter((s) => s !== '');
      return { question: q, answerParts };
    }
  });

  return (
    <>
      <button
        type="button"
        onClick={() => setCollapsed((c) => !c)}
        className="w-full px-3 py-2.5 flex items-center gap-2 text-left rounded-lg transition-colors theme-text-primary hover:bg-black/5 dark:hover:bg-white/5"
        aria-expanded={!collapsed}
      >
        <MessageCircle className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
        <span className="text-[10px] font-semibold tracking-wide theme-text-secondary shrink-0">
          {t(I18N_KEYS.AI.ASK_USER_QUESTION.LABEL)}
        </span>
        <span className="min-w-0 flex-1 truncate text-[12px] theme-text-secondary">
          {summary}
        </span>
        <span className="shrink-0 opacity-60" aria-hidden>
          {collapsed ? <ChevronRight className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </span>
      </button>

      {!collapsed && (
        <div className="px-3 pt-0 pb-3 border-t theme-border">
          {questionAnswerPairs.map((pair, qIdx) => {
            const { question, answerParts } = pair;
            const allowMultiSelect = true; // Always multi-select

            return (
              <div key={qIdx} className={qIdx > 0 ? 'mt-4 pt-4 border-t theme-border' : 'mt-3'}>
                {/* Header chip */}
                {questions.length > 1 && (
                  <div className="flex items-center gap-2 mb-2">
                    <Tag className="w-3.5 h-3.5 theme-text-secondary shrink-0" aria-hidden />
                    <span className="text-[10px] font-semibold tracking-wide theme-text-secondary">
                      {truncateText(question.question, 20)}
                    </span>
                  </div>
                )}

                {/* Full question text */}
                <p className="theme-text-primary text-[13px] mb-3 whitespace-pre-wrap">
                  {question.question}
                </p>

                {/* Options with selected ones highlighted */}
                {question.options && question.options.length > 0 && (
                  <div className="flex flex-col gap-2 mb-3">
                    {question.options.map((opt, optIdx) => {
                      const isSelected = answerParts.includes(opt);

                      return (
                        <div
                          key={optIdx}
                          className={`w-full text-left px-3 py-2 rounded-md text-[12px] flex items-center gap-2 ${
                            isSelected
                              ? 'bg-[var(--accent-blue)] text-white font-medium'
                              : 'border theme-border opacity-60 theme-text-primary'
                          }`}
                        >
                          {/* Checkbox or Radio indicator */}
                          <div
                            className={`shrink-0 w-4 h-4 border flex items-center justify-center ${
                              allowMultiSelect ? 'rounded' : 'rounded-full'
                            } ${
                              isSelected
                                ? 'bg-white/20 border-white/30'
                                : 'border-current bg-transparent'
                            }`}
                          >
                            {isSelected && (
                              allowMultiSelect ? (
                                <Check className="w-3 h-3" />
                              ) : (
                                <div className="w-2 h-2 rounded-full bg-white" />
                              )
                            )}
                          </div>
                          <span>{opt}</span>
                        </div>
                      );
                    })}
                  </div>
                )}

                {/* Custom text answers (not from options) */}
                {(() => {
                  const customAnswers = answerParts.filter(
                    (part) => !question.options || !question.options.includes(part)
                  );

                  if (customAnswers.length > 0) {
                    return (
                      <div className="flex flex-col gap-2">
                        {customAnswers.map((customAns, idx) => (
                          <div
                            key={idx}
                            className="py-2 px-2.5 rounded bg-[var(--accent-blue)] text-[12px] font-medium text-white"
                          >
                            {customAns}
                          </div>
                        ))}
                      </div>
                    );
                  }
                  return null;
                })()}
              </div>
            );
          })}
        </div>
      )}
    </>
  );
}
