import { Send, Mic, Paperclip, ChevronDown } from 'lucide-react';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '../ui/DropdownMenu';
import type { AgentType } from './ChatInput';
import { AGENT_COLORS } from './ChatInput';

const MODELS = ['Gemini 3 Pro', 'GPT-4o', 'Claude 3.5'];

interface AgentOption {
  type: AgentType;
  icon: React.ElementType;
  label: string;
}

interface ChatInputToolbarProps {
  agent: AgentType;
  setAgent: (agent: AgentType) => void;
  model: string;
  setModel: (model: string) => void;
  onSend: () => void;
  agents: AgentOption[];
  CurrentAgentIcon: React.ElementType;
}

export function ChatInputToolbar({
  agent,
  setAgent,
  model,
  setModel,
  onSend,
  agents,
  CurrentAgentIcon,
}: ChatInputToolbarProps) {
  return (
    <div className="flex items-center justify-between px-2 pb-2">
      <div className="flex items-center space-x-2">
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className={`flex items-center space-x-1.5 text-[10px] theme-text-primary border rounded-full px-2 py-0.5 transition-colors ${AGENT_COLORS[agent].bg} ${AGENT_COLORS[agent].border} border hover:opacity-90`}
            >
              <CurrentAgentIcon className={`w-3 h-3 ${AGENT_COLORS[agent].icon}`} />
              <span className="font-medium">{agents.find((a) => a.type === agent)?.label}</span>
              <ChevronDown className="w-2.5 h-2.5 opacity-50" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="w-32">
            {agents.map((a) => (
              <DropdownMenuItem
                key={a.type}
                onClick={() => setAgent(a.type)}
                className="text-[10px] flex items-center space-x-2"
              >
                <a.icon className={`w-3 h-3 ${AGENT_COLORS[a.type].icon}`} />
                <span>{a.label}</span>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>

        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button
              type="button"
              className="text-[10px] theme-text-secondary hover:theme-text-primary transition-colors flex items-center space-x-1"
            >
              <span>{model}</span>
              <ChevronDown className="w-2.5 h-2.5 opacity-50" />
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="start" className="w-32">
            {MODELS.map((m) => (
              <DropdownMenuItem key={m} onClick={() => setModel(m)} className="text-[10px]">
                {m}
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
      </div>

      <div className="flex items-center space-x-2">
        <button
          type="button"
          className="p-1.5 theme-text-secondary hover:theme-text-primary transition-colors"
          aria-label="Microphone"
        >
          <Mic className="w-3.5 h-3.5" />
        </button>
        <button
          type="button"
          className="p-1.5 theme-text-secondary hover:theme-text-primary transition-colors"
          aria-label="Attachment"
        >
          <Paperclip className="w-3.5 h-3.5" />
        </button>
        <button
          type="button"
          onClick={onSend}
          className={`p-1.5 transition-colors ${AGENT_COLORS[agent].sendBtn}`}
          aria-label="Send"
        >
          <Send className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
}
