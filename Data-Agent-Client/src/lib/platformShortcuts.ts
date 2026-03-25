type NavigatorWithUserAgentData = Navigator & {
  userAgentData?: {
    platform?: string;
  };
};

const APPLE_PLATFORM_PATTERN = /mac|iphone|ipad|ipod/i;

function readPlatformHints(): string[] {
  if (typeof navigator === 'undefined') {
    return [];
  }

  const browserNavigator = navigator as NavigatorWithUserAgentData;

  return [
    browserNavigator.userAgentData?.platform,
    navigator.platform,
    navigator.userAgent,
  ].filter((value): value is string => Boolean(value));
}

export function isApplePlatform(): boolean {
  return readPlatformHints().some((value) => APPLE_PLATFORM_PATTERN.test(value));
}

export function getPrimaryModifierLabel(): 'Cmd' | 'Ctrl' {
  return isApplePlatform() ? 'Cmd' : 'Ctrl';
}

export function formatPrimaryShortcut(...keys: string[]): string {
  return [getPrimaryModifierLabel(), ...keys].join('+');
}

export function getPlatformShortcuts() {
  return {
    runQuery: formatPrimaryShortcut('Enter'),
    openSettings: formatPrimaryShortcut('Shift', ','),
    toggleAI: formatPrimaryShortcut('B'),
  };
}
