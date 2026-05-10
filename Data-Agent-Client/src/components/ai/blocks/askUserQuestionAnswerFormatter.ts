import type { SingleQuestion } from './askUserQuestionTypes';

export interface AskUserAnswerValue {
  selectedOptions: string[];
  customText: string;
}

interface AnswerQuestionJson {
  index: number;
  question: string;
  options?: string[];
  selectedOptions?: string[];
  customAnswer?: string;
  rejectedOptions?: string[];
}

interface AskUserAnswerJson {
  type: 'ask_user_question_answer';
  toolCallId?: string;
  status?: 'declined';
  message: string;
  questions: AnswerQuestionJson[];
}

function cleanText(value: string | null | undefined): string {
  return (value ?? '').trim();
}

function cleanOptions(values: string[] | undefined): string[] {
  return (values ?? []).map(cleanText).filter(Boolean);
}

function buildQuestionBase(question: SingleQuestion, index: number): AnswerQuestionJson {
  const options = cleanOptions(question.options);
  return {
    index: index + 1,
    question: question.question,
    ...(options.length > 0 && { options }),
  };
}

export function buildAskUserQuestionAnswerMessage(
  questions: SingleQuestion[],
  answers: Map<number, AskUserAnswerValue>,
  toolCallId?: string,
): string {
  const payload: AskUserAnswerJson = {
    type: 'ask_user_question_answer',
    ...(toolCallId && { toolCallId }),
    message: '请基于用户回答继续当前任务；只有信息仍不足时才再次追问。如果 rejectedOptions 不为空，请不要把 rejectedOptions 中的候选当作用户已确认的答案。',
    questions: [],
  };

  questions.forEach((question, index) => {
    const answer = answers.get(index);
    if (!answer) return;

    const selectedOptions = cleanOptions(answer.selectedOptions);
    const customAnswer = cleanText(answer.customText);
    if (selectedOptions.length === 0 && !customAnswer) return;

    const originalOptions = cleanOptions(question.options);
    payload.questions.push({
      ...buildQuestionBase(question, index),
      ...(selectedOptions.length > 0 && { selectedOptions }),
      ...(customAnswer && { customAnswer }),
      ...(selectedOptions.length === 0 && customAnswer && originalOptions.length > 0 && {
        rejectedOptions: originalOptions,
      }),
    });
  });

  return JSON.stringify(payload, null, 2);
}

export function buildAskUserQuestionDeclinedMessage(
  questions: SingleQuestion[],
  toolCallId?: string,
): string {
  const payload: AskUserAnswerJson = {
    type: 'ask_user_question_answer',
    ...(toolCallId && { toolCallId }),
    status: 'declined',
    message: '用户拒绝回答本轮澄清问题。请基于当前上下文判断是否可以继续；如果仍缺少必要信息，请说明缺少的信息。',
    questions: questions.map(buildQuestionBase),
  };

  return JSON.stringify(payload, null, 2);
}
