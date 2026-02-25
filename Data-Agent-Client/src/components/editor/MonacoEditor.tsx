import Editor, { loader } from '@monaco-editor/react';
import { forwardRef, useImperativeHandle, useRef } from 'react';
import { useTheme } from '../../hooks/useTheme';

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
}

export const MonacoEditor = forwardRef<MonacoEditorHandle, MonacoEditorProps>(
  ({
    value,
    onChange,
    language = 'sql',
    readOnly = false,
    height = '100%',
  }, ref) => {

    const { theme: appTheme } = useTheme();
    const editorRef = useRef<any>(null);

    const handleEditorWillMount = (monaco: any) => {
      // Define JetBrains-like dark theme
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

      // Define JetBrains-like light theme
      monaco.editor.defineTheme('jetbrains-light', {
        base: 'vs',
        inherit: true,
        rules: [
          { token: 'keyword', foreground: 'd81e1e', fontStyle: 'bold' },
          { token: 'string', foreground: '067d17' },
          { token: 'function', foreground: 'be860b' },
          { token: 'number', foreground: '0b5394' },
          { token: 'comment', foreground: '8c8c8c', fontStyle: 'italic' },
          { token: 'operator', foreground: '001080' },
          { token: 'delimiter', foreground: '001080' },
        ],
        colors: {
          'editor.background': '#ffffff',
          'editor.foreground': '#000000',
          'editor.lineHighlightBackground': '#f5f5f5',
          'editor.selectionBackground': '#add6ff',
          'editorCursor.foreground': '#000000',
          'editorLineNumber.foreground': '#999999',
          'editorLineNumber.activeForeground': '#000000',
          'editorIndentGuide.background': '#e0e0e0',
          'editor.border': '#d0d0d0',
        }
      });
    };

    const getCurrentTheme = () => {
      return appTheme === 'light' ? 'jetbrains-light' : 'jetbrains-dark';
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
        theme={getCurrentTheme()}
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
