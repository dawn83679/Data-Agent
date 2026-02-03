import React from 'react';
import { Send, Mic, Paperclip, Infinity, ListTodo, LineChart, ChevronDown } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "../ui/DropdownMenu";

export type AgentType = 'Agent' | 'Plan' | 'Analysis';

interface ChatInputProps {
  input: string;
  setInput: (value: string) => void;
  onSend: () => void;
  agent: AgentType;
  setAgent: (agent: AgentType) => void;
  model: string;
  setModel: (model: string) => void;
}

export function ChatInput({ 
  input, 
  setInput, 
  onSend, 
  agent, 
  setAgent, 
  model, 
  setModel 
}: ChatInputProps) {
  const { t } = useTranslation();

  const agents: { type: AgentType; icon: any; label: string }[] = [
    { type: 'Agent', icon: Infinity, label: t('ai.agent') },
    { type: 'Plan', icon: ListTodo, label: t('ai.plan') },
    { type: 'Analysis', icon: LineChart, label: t('ai.analysis') },
  ];

  const models = ['Gemini 3 Pro', 'GPT-4o', 'Claude 3.5'];

  const handleKeyDown = (e: React.KeyboardEvent) => {
    // Enter: Send message
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      onSend();
    }

    // Tab: Switch model (simple cycle)
    if (e.key === 'Tab' && !e.shiftKey) {
      e.preventDefault();
      const nextIndex = (models.indexOf(model) + 1) % models.length;
      setModel(models[nextIndex]);
    }

    // Shift+Tab: Switch agent mode
    if (e.key === 'Tab' && e.shiftKey) {
      e.preventDefault();
      const agentsList: AgentType[] = ['Agent', 'Plan', 'Analysis'];
      const nextIndex = (agentsList.indexOf(agent) + 1) % agentsList.length;
      setAgent(agentsList[nextIndex]);
    }
  };

  const CurrentAgentIcon = agents.find(a => a.type === agent)?.icon || Infinity;

  return (
    <div className="p-2 theme-bg-panel border-t theme-border shrink-0">
      <div className="rounded-lg border theme-border theme-bg-main relative focus-within:border-primary/50 transition-colors flex flex-col">
        <textarea 
          data-ai-input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={t('ai.placeholder')}
          className="w-full h-24 bg-transparent text-xs p-3 focus:outline-none resize-none placeholder:text-muted-foreground/50"
        />
        
        <div className="flex items-center justify-between px-2 pb-2">
          <div className="flex items-center space-x-2">
            {/* Agent Selector */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button className="flex items-center space-x-1.5 text-[10px] theme-text-primary bg-accent/30 border theme-border rounded-full px-2 py-0.5 hover:bg-accent/50 transition-colors">
                  <CurrentAgentIcon className="w-3 h-3 text-purple-400" />
                  <span className="font-medium">{agents.find(a => a.type === agent)?.label}</span>
                  <ChevronDown className="w-2.5 h-2.5 opacity-50" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start" className="w-32">
                {agents.map(a => (
                  <DropdownMenuItem key={a.type} onClick={() => setAgent(a.type)} className="text-[10px] flex items-center space-x-2">
                    <a.icon className="w-3 h-3" />
                    <span>{a.label}</span>
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>

            {/* Model Selector */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <button className="text-[10px] theme-text-secondary hover:theme-text-primary transition-colors flex items-center space-x-1">
                  <span>{model}</span>
                  <ChevronDown className="w-2.5 h-2.5 opacity-50" />
                </button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="start" className="w-32">
                {models.map(m => (
                  <DropdownMenuItem key={m} onClick={() => setModel(m)} className="text-[10px]">
                    {m}
                  </DropdownMenuItem>
                ))}
              </DropdownMenuContent>
            </DropdownMenu>
          </div>

          <div className="flex items-center space-x-2">
            <button className="p-1.5 theme-text-secondary hover:theme-text-primary transition-colors">
              <Mic className="w-3.5 h-3.5" />
            </button>
            <button className="p-1.5 theme-text-secondary hover:theme-text-primary transition-colors">
              <Paperclip className="w-3.5 h-3.5" />
            </button>
            <button 
              onClick={onSend}
              className="p-1.5 text-primary hover:text-primary/80 transition-colors"
            >
              <Send className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
