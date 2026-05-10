import { describe, expect, it } from 'vitest';
import {
  buildAskUserQuestionAnswerMessage,
  buildAskUserQuestionDeclinedMessage,
} from './askUserQuestionAnswerFormatter';
import type { SingleQuestion } from './askUserQuestionTypes';

describe('askUserQuestion answer formatter', () => {
  it('serializes selected options as JSON with the tool call id', () => {
    const questions: SingleQuestion[] = [{
      question: '请选择要查询订单和订阅数据的数据库连接：',
      options: ['test1 (MySQL)', 'test2 (MySQL)', 'test3 (MySQL)'],
    }];
    const answers = new Map([
      [0, { selectedOptions: ['test3 (MySQL)'], customText: '' }],
    ]);

    const payload = JSON.parse(buildAskUserQuestionAnswerMessage(questions, answers, 'call-1'));

    expect(payload).toEqual({
      type: 'ask_user_question_answer',
      toolCallId: 'call-1',
      message: '请基于用户回答继续当前任务；只有信息仍不足时才再次追问。如果 rejectedOptions 不为空，请不要把 rejectedOptions 中的候选当作用户已确认的答案。',
      questions: [{
        index: 1,
        question: '请选择要查询订单和订阅数据的数据库连接：',
        options: ['test1 (MySQL)', 'test2 (MySQL)', 'test3 (MySQL)'],
        selectedOptions: ['test3 (MySQL)'],
      }],
    });
    expect(JSON.stringify(payload)).not.toContain('answerSemantics');
    expect(JSON.stringify(payload)).not.toContain('continue_instruction');
  });

  it('serializes custom-only input with rejectedOptions copied from original options', () => {
    const questions: SingleQuestion[] = [{
      question: '订单和订阅数据通常存放在哪个数据库中？',
      options: ['deploy', 'zoer_dev', 'enterprise_gateway_dev'],
    }];
    const answers = new Map([
      [0, { selectedOptions: [], customText: '应该在其他数据库' }],
    ]);

    const payload = JSON.parse(buildAskUserQuestionAnswerMessage(questions, answers, 'call-2'));

    expect(payload.questions[0]).toEqual({
      index: 1,
      question: '订单和订阅数据通常存放在哪个数据库中？',
      options: ['deploy', 'zoer_dev', 'enterprise_gateway_dev'],
      customAnswer: '应该在其他数据库',
      rejectedOptions: ['deploy', 'zoer_dev', 'enterprise_gateway_dev'],
    });
  });

  it('preserves selected options and additional custom answer without rejecting options', () => {
    const questions: SingleQuestion[] = [{
      question: '需要操作哪个数据库的数据？',
      options: ['deploy', 'zoer_dev', 'enterprise_gateway_dev'],
    }];
    const answers = new Map([
      [0, { selectedOptions: ['enterprise_gateway_dev'], customText: '订单号是 2026050922001445251447350050' }],
    ]);

    const payload = JSON.parse(buildAskUserQuestionAnswerMessage(questions, answers, 'call-3'));

    expect(payload.questions[0]).toEqual({
      index: 1,
      question: '需要操作哪个数据库的数据？',
      options: ['deploy', 'zoer_dev', 'enterprise_gateway_dev'],
      selectedOptions: ['enterprise_gateway_dev'],
      customAnswer: '订单号是 2026050922001445251447350050',
    });
    expect(payload.questions[0].rejectedOptions).toBeUndefined();
  });

  it('serializes declined answers with original questions and options', () => {
    const questions: SingleQuestion[] = [{
      question: '订单和订阅数据通常存放在哪个数据库中？',
      options: ['deploy', 'zoer_dev', 'enterprise_gateway_dev'],
    }];

    const payload = JSON.parse(buildAskUserQuestionDeclinedMessage(questions, 'call-4'));

    expect(payload).toEqual({
      type: 'ask_user_question_answer',
      toolCallId: 'call-4',
      status: 'declined',
      message: '用户拒绝回答本轮澄清问题。请基于当前上下文判断是否可以继续；如果仍缺少必要信息，请说明缺少的信息。',
      questions: [{
        index: 1,
        question: '订单和订阅数据通常存放在哪个数据库中？',
        options: ['deploy', 'zoer_dev', 'enterprise_gateway_dev'],
      }],
    });
  });
});
