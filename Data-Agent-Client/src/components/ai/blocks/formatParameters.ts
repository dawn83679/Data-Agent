/**
 * Format tool parameters for display.
 * Attempts to parse as JSON and pretty-print, falls back to raw string.
 */
export function formatParameters(parametersData: string): {
  formattedParameters: string;
  isParametersJson: boolean;
} {
  if (!parametersData?.trim()) {
    return { formattedParameters: parametersData, isParametersJson: false };
  }

  try {
    const parsed = JSON.parse(parametersData);
    return {
      formattedParameters: JSON.stringify(parsed, null, 2),
      isParametersJson: true,
    };
  } catch {
    return { formattedParameters: parametersData, isParametersJson: false };
  }
}
