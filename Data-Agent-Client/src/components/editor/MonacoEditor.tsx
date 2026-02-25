import Editor, { loader } from '@monaco-editor/react';
import { forwardRef, useImperativeHandle, useRef } from 'react';

// Configure Monaco CDN
loader.config({ paths: { vs: 'https://cdn.jsdelivr.net/npm/monaco-editor@0.43.0/min/vs' } });

export interface MonacoEditorHandle {
  /**
   * Returns the selected text if selection exists, otherwise returns full editor content
   */
  getSelectionOrAllContent(): string;

  /**
   * Programmatically set editor content
   */
  setValue(value: string): void;
}

interface MonacoEditorProps {
  value: string;
  onChange?: (value: string | undefined) => void;
  language?: string;
  readOnly?: boolean;
  height?: string | number;
  theme?: 'jetbrains-dark' | 'vs-dark' | 'light';
}

export const MonacoEditor = forwardRef<MonacoEditorHandle, MonacoEditorProps>(
  ({
    value,
    onChange,
    language = 'sql',
    readOnly = false,
    height = '100%',
    theme = 'jetbrains-dark'
  }, ref) => {

    const editorRef = useRef<any>(null);

    const handleEditorWillMount = (monaco: any) => {
      // Define JetBrains-like theme
      monaco.editor.defineTheme('jetbrains-dark', {
        base: 'vs-dark',
        inherit: true,
        rules: [
          { token: 'keyword', foreground: 'cc7832', fontStyle: 'bold' },
          { token: 'string', foreground: '6a8759' },
          { token: 'function', foreground: 'ffc66d' },
          { token: 'number', foreground: '6897bb' },
          { token: 'comment', foreground: '808080', fontStyle: 'italic' },
          { token: 'operator', foreground: 'a9b7c6' },
          { token: 'delimiter', foreground: 'a9b7c6' },
        ],
        colors: {
          'editor.background': '#1e1f22',
          'editor.foreground': '#bcbec4',
          'editor.lineHighlightBackground': '#2e2f33',
          'editor.selectionBackground': '#2e436e',
          'editorCursor.foreground': '#a9b7c6',
          'editorLineNumber.foreground': '#606366',
          'editorLineNumber.activeForeground': '#a9b7c6',
          'editorIndentGuide.background': '#373737',
          'editor.border': '#4e5155',
        }
      });
    };

    useImperativeHandle(ref, () => ({
      getSelectionOrAllContent(): string {
        const editor = editorRef.current;
        if (!editor) return '';
        const selection = editor.getSelection();
        if (selection && !selection.isEmpty()) {
          return editor.getModel()?.getValueInRange(selection) ?? '';
        }
        return editor.getValue();
      },
      setValue(newValue: string): void {
        if (editorRef.current) {
          editorRef.current.setValue(newValue);
        }
      }
    }), []);

    return (
      <Editor
        height={height}
        language={language}
        value={value}
        theme={theme}
        onChange={onChange}
        beforeMount={handleEditorWillMount}
        onMount={(editor) => {
          editorRef.current = editor;
        }}
        options={{
          readOnly,
          minimap: { enabled: false },
          fontSize: 13,
          fontFamily: "'JetBrains Mono', monospace",
          lineHeight: 1.5,
          scrollBeyondLastLine: false,
          automaticLayout: true,
          padding: { top: 10, bottom: 10 },
          renderLineHighlight: 'all',
          scrollbar: {
            vertical: 'visible',
            horizontal: 'visible',
            verticalScrollbarSize: 10,
            horizontalScrollbarSize: 10,
          }
        }}
      />
    );
  }
);

MonacoEditor.displayName = 'MonacoEditor';
