import { cn } from '../../lib/utils';

interface RadioOption {
  value: string;
  label: string;
  icon?: React.ReactNode;
}

interface RadioGroupProps {
  name: string;
  value: string;
  options: RadioOption[];
  onChange: (value: string) => void;
  variant?: 'horizontal' | 'vertical';
}

export function RadioGroup({ name, value, options, onChange, variant = 'vertical' }: RadioGroupProps) {
  const containerClass = variant === 'horizontal' ? 'grid grid-cols-2 gap-3' : 'space-y-2';

  return (
    <div className={containerClass}>
      {options.map((option) => (
        <label key={option.value} className="flex items-center gap-2 cursor-pointer group">
          <input
            type="radio"
            name={name}
            checked={value === option.value}
            onChange={() => onChange(option.value)}
            className="form-radio h-3.5 w-3.5 text-blue-500 bg-transparent border-theme-border focus:ring-0"
          />
          {option.icon && <span>{option.icon}</span>}
          <span className="text-xs theme-text-secondary group-hover:theme-text-primary transition-colors">
            {option.label}
          </span>
        </label>
      ))}
    </div>
  );
}

interface ButtonRadioGroupProps {
  value: string;
  options: RadioOption[];
  onChange: (value: string) => void;
}

export function ButtonRadioGroup({ value, options, onChange }: ButtonRadioGroupProps) {
  return (
    <div className="grid grid-cols-2 gap-3">
      {options.map((option) => (
        <button
          key={option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            'flex items-center justify-center gap-2 px-3 py-2 rounded border transition-all',
            value === option.value
              ? 'theme-bg-panel border-blue-500 text-blue-500'
              : 'theme-bg-main theme-border theme-text-secondary hover:theme-bg-panel'
          )}
        >
          {option.icon}
          <span>{option.label}</span>
        </button>
      ))}
    </div>
  );
}
