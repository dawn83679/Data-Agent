import { useState, useMemo } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../../constants/i18nKeys';
import { AskUserQuestionPayload, normalizeToQuestions, SingleQuestion } from './askUserQuestionTypes';
import { HelpCircle, Check } from 'lucide-react';
import { useAIAssistantContext } from '../AIAssistantContext';

export interface AskUserQuestionCardProps {
    askUserPayload: AskUserQuestionPayload;
    submittedAnswer?: string;
}

export function AskUserQuestionCard({ askUserPayload, submittedAnswer }: AskUserQuestionCardProps) {
    const { t } = useTranslation();
    const { submitMessage, isLoading } = useAIAssistantContext();
    const [isSubmitted, setIsSubmitted] = useState(false);
    const [activeTab, setActiveTab] = useState(0);
    const questions = useMemo(() => normalizeToQuestions(askUserPayload), [askUserPayload]);

    // Form state tracker similar to AskUserUnanswered
    interface AnswerValue {
        selectedOptions: string[];
        customText: string;
    }

    const [answers, setAnswers] = useState<Map<number, AnswerValue>>(() => {
        const initial = new Map<number, AnswerValue>();
        questions.forEach((_, idx) => {
            initial.set(idx, { selectedOptions: [], customText: '' });
        });
        return initial;
    });

    const handleOptionClick = (questionIdx: number, option: string, allowMultiSelect: boolean) => {
        setAnswers((prev) => {
            const newAnswers = new Map(prev);
            const current = newAnswers.get(questionIdx) || { selectedOptions: [], customText: '' };

            if (allowMultiSelect) {
                const isSelected = current.selectedOptions.includes(option);
                const newSelected = isSelected
                    ? current.selectedOptions.filter((o) => o !== option)
                    : [...current.selectedOptions, option];
                newAnswers.set(questionIdx, { ...current, selectedOptions: newSelected });
            } else {
                newAnswers.set(questionIdx, { ...current, selectedOptions: [option] });
            }
            return newAnswers;
        });
    };

    const handleCustomTextChange = (questionIdx: number, text: string) => {
        setAnswers((prev) => {
            const newAnswers = new Map(prev);
            const current = newAnswers.get(questionIdx) || { selectedOptions: [], customText: '' };
            newAnswers.set(questionIdx, { ...current, customText: text });
            return newAnswers;
        });
    };

    // Check if all questions have at least one answer
    const isSubmitDisabled = isLoading || !questions.every((_, idx) => {
        const answer = answers.get(idx);
        return answer && (answer.selectedOptions.length > 0 || answer.customText.trim() !== '');
    });

    const handleSubmit = () => {
        if (isSubmitDisabled) return;

        let formattedAnswer = t(I18N_KEYS.AI.ASK_USER_QUESTION.ANSWER_PREFIX) + '\n\n';

        questions.forEach((q, idx) => {
            const answer = answers.get(idx);
            if (!answer) return;

            const selectedParts = answer.selectedOptions;
            const customPart = answer.customText.trim();

            let answerStr = '';
            if (selectedParts.length > 0 && customPart) {
                answerStr = `${selectedParts.join(', ')} | ${t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_LABEL)}: ${customPart}`;
            } else if (selectedParts.length > 0) {
                answerStr = selectedParts.join(', ');
            } else if (customPart) {
                answerStr = `${t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_LABEL)}: ${customPart}`;
            }

            if (answerStr) {
                // If there's only one question, we don't strictly need the question title,
                // but adding it makes the markdown structure consistent and clear.
                formattedAnswer += `- **${q.question}**\n  👉 ${answerStr}\n\n`;
            }
        });

        formattedAnswer += t(I18N_KEYS.AI.ASK_USER_QUESTION.CONTINUE_SUFFIX);

        // Append original questions context to remind the AI what it asked
        if (questions.length === 1) {
            formattedAnswer += `\n\n${t(I18N_KEYS.AI.ASK_USER_QUESTION.ORIGINAL_QUESTION_PREFIX)}\n> ${questions[0].question}`;
        } else {
            formattedAnswer += `\n\n${t(I18N_KEYS.AI.ASK_USER_QUESTION.ORIGINAL_QUESTION_PREFIX)}\n`;
            questions.forEach(q => {
                formattedAnswer += `> - ${q.question}\n`;
            });
        }

        submitMessage(formattedAnswer);
        setIsSubmitted(true);
    };

    const handleReject = () => {
        let msg = t(I18N_KEYS.AI.ASK_USER_QUESTION.REJECT_MESSAGE);

        // Append original questions context to remind the AI what it asked
        if (questions.length === 1) {
            msg += `\n\n${t(I18N_KEYS.AI.ASK_USER_QUESTION.ORIGINAL_QUESTION_PREFIX)}\n> ${questions[0].question}`;
        } else {
            msg += `\n\n${t(I18N_KEYS.AI.ASK_USER_QUESTION.ORIGINAL_QUESTION_PREFIX)}\n`;
            questions.forEach(q => {
                msg += `> - ${q.question}\n`;
            });
        }

        submitMessage(msg);
        setIsSubmitted(true);
    };

    if (submittedAnswer || isSubmitted) {
        return null;
    }

    const renderQuestionContent = (question: SingleQuestion, qIdx: number) => {
        const answer = answers.get(qIdx) || { selectedOptions: [], customText: '' };
        const allowMultiSelect = question.allowMultiSelect ?? false;

        return (
            <div key={qIdx} className="mb-4 last:mb-0">
                <p className="theme-text-primary text-[13px] mb-3 whitespace-pre-wrap font-medium">
                    {question.question}
                </p>

                {question.options && question.options.length > 0 && (
                    <div className="flex flex-col gap-2 mb-3">
                        {question.options.map((opt, optIdx) => {
                            const isSelected = answer.selectedOptions.includes(opt);

                            return (
                                <button
                                    key={optIdx}
                                    type="button"
                                    disabled={isLoading}
                                    onClick={() => handleOptionClick(qIdx, opt, allowMultiSelect)}
                                    className={`w-full text-left px-3 py-2 rounded-md text-[12px] transition-colors border theme-text-primary disabled:opacity-60 disabled:cursor-not-allowed flex items-center gap-2 ${isSelected
                                        ? 'theme-bg-selected theme-border-accent border'
                                        : 'theme-border theme-bg-hover'
                                        }`}
                                >
                                    <div
                                        className={`shrink-0 w-4 h-4 border theme-border flex items-center justify-center ${allowMultiSelect ? 'rounded' : 'rounded-full'
                                            } ${isSelected
                                                ? 'bg-[var(--accent-blue)] border-[var(--accent-blue)]'
                                                : 'bg-transparent'
                                            }`}
                                    >
                                        {isSelected && (
                                            allowMultiSelect ? (
                                                <Check className="w-3 h-3 text-white" />
                                            ) : (
                                                <div className="w-2 h-2 rounded-full bg-white" />
                                            )
                                        )}
                                    </div>
                                    <span>{opt}</span>
                                </button>
                            );
                        })}
                    </div>
                )}

                <div className="mb-3">
                    <input
                        type="text"
                        value={answer.customText}
                        onChange={(e) => handleCustomTextChange(qIdx, e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && !isSubmitDisabled) {
                                handleSubmit();
                            }
                        }}
                        placeholder={
                            question.freeTextHint || t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_INPUT_PLACEHOLDER)
                        }
                        disabled={isLoading}
                        className="w-full px-2.5 py-1.5 rounded-md border theme-border theme-bg-panel theme-text-primary text-[12px] placeholder:theme-text-secondary focus:outline-none focus:ring-1 focus:ring-[var(--accent-blue)] focus:border-[var(--accent-blue)] disabled:opacity-60 disabled:cursor-not-allowed"
                        aria-label={t(I18N_KEYS.AI.ASK_USER_QUESTION.CUSTOM_INPUT_PLACEHOLDER)}
                    />
                </div>
            </div>
        );
    };

    return (
        <div className="mb-2 overflow-hidden rounded-lg border theme-border theme-bg-main shadow-sm flex flex-col">
            <div className="flex items-center gap-2 px-4 py-3 bg-[var(--accent-blue)]/5 border-b theme-border text-[var(--accent-blue)] font-medium">
                <HelpCircle className="w-4 h-4" />
                <span className="text-[11px] uppercase tracking-wide">{t(I18N_KEYS.AI.ASK_USER_QUESTION.LABEL)}</span>
            </div>

            {questions.length > 1 && (
                <div className="flex overflow-x-auto border-b theme-border theme-bg-surface scrollbar-hide">
                    {questions.map((q, idx) => {
                        const answer = answers.get(idx);
                        const isAnswered = answer && (answer.selectedOptions.length > 0 || answer.customText.trim());
                        return (
                            <button
                                key={idx}
                                onClick={() => setActiveTab(idx)}
                                className={`px-4 py-2.5 text-[12px] font-medium whitespace-nowrap border-b-2 transition-colors flex items-center gap-2 ${activeTab === idx
                                    ? 'border-[var(--accent-blue)] text-[var(--accent-blue)] bg-[var(--accent-blue)]/5'
                                    : 'border-transparent theme-text-secondary hover:theme-text-primary'
                                    }`}
                            >
                                <span className="max-w-[150px] truncate">{q.question}</span>
                                {isAnswered ? (
                                    <Check className="w-3.5 h-3.5 text-green-500 shrink-0" />
                                ) : (
                                    <div className="w-3.5 h-3.5 rounded-full border border-dashed theme-border opacity-50 shrink-0" />
                                )}
                            </button>
                        );
                    })}
                </div>
            )}

            <div className="p-4 space-y-4">
                {renderQuestionContent(questions[activeTab], activeTab)}
            </div>

            <div className="p-3 border-t theme-border theme-bg-surface flex justify-end gap-2">
                {questions.length > 1 && activeTab < questions.length - 1 && (
                    <button
                        type="button"
                        onClick={() => setActiveTab((p) => p + 1)}
                        className="px-4 py-1.5 rounded-md text-[12px] font-medium theme-bg-panel theme-text-primary border theme-border hover:theme-bg-hover transition-colors"
                    >
                        Next
                    </button>
                )}
                <button
                    type="button"
                    onClick={handleReject}
                    disabled={isLoading}
                    className="px-4 py-1.5 rounded-md text-[12px] font-medium theme-bg-panel theme-text-secondary border theme-border hover:theme-bg-hover transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                >
                    {t(I18N_KEYS.AI.ASK_USER_QUESTION.REJECT) || 'Reject'}
                </button>
                <button
                    type="button"
                    onClick={handleSubmit}
                    disabled={isSubmitDisabled}
                    className="px-4 py-1.5 rounded-md text-[12px] font-medium bg-[var(--accent-blue)] text-white hover:opacity-90 disabled:opacity-50 disabled:cursor-not-allowed transition-opacity"
                >
                    {t(I18N_KEYS.AI.ASK_USER_QUESTION.SUBMIT_ANSWER)}
                </button>
            </div>
        </div>
    );
}
