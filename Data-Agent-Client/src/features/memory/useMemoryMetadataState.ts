import { useCallback, useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { memoryService } from '../../services/memory.service';
import type { MemoryMetadataResponse } from '../../types/memory';
import {
  buildFallbackMemoryMetadata,
  buildSubTypeOptionsByType,
  normalizeMemoryMetadata,
} from './memoryPageUtils';

const fallbackMemoryMetadata = buildFallbackMemoryMetadata();

export function useMemoryMetadataState() {
  const { t } = useTranslation();
  const toast = useToast();
  const [memoryMetadata, setMemoryMetadata] = useState<MemoryMetadataResponse>(fallbackMemoryMetadata);
  const [metadataLoading, setMetadataLoading] = useState(false);

  const loadMemoryMetadata = useCallback(async () => {
    setMetadataLoading(true);
    try {
      const metadata = await memoryService.getMetadata();
      setMemoryMetadata(normalizeMemoryMetadata(metadata));
    } catch (error) {
      setMemoryMetadata(buildFallbackMemoryMetadata());
      toast.error(resolveErrorMessage(error, t(I18N_KEYS.MEMORY_PAGE.METADATA_LOAD_FAILED)));
    } finally {
      setMetadataLoading(false);
    }
  }, [t, toast]);

  useEffect(() => {
    void loadMemoryMetadata();
  }, [loadMemoryMetadata]);

  const subTypeOptionsByType = useMemo(
    () => buildSubTypeOptionsByType(memoryMetadata.memoryTypes),
    [memoryMetadata.memoryTypes],
  );

  return {
    memoryMetadata,
    metadataLoading,
    subTypeOptionsByType,
  };
}
