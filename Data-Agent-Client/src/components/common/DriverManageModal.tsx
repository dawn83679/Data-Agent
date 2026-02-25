import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { driverService } from '../../services/driver.service';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import type { InstalledDriverResponse, AvailableDriverResponse } from '../../types/driver';

interface DriverManageModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  databaseType: string;
  onSelectDriver: (driverPath: string) => void;
}

export function DriverManageModal({
  open,
  onOpenChange,
  databaseType,
  onSelectDriver,
}: DriverManageModalProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const [installed, setInstalled] = useState<InstalledDriverResponse[]>([]);
  const [available, setAvailable] = useState<AvailableDriverResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [downloadingVersion, setDownloadingVersion] = useState<string | null>(null);

  useEffect(() => {
    if (!open || !databaseType?.trim()) return;
    
    let isMounted = true;
    setLoading(true);
    
    Promise.all([
      driverService.listInstalledDrivers(databaseType),
      driverService.listAvailableDrivers(databaseType),
    ])
      .then(([inst, av]) => {
        if (isMounted) {
          setInstalled(inst);
          setAvailable(av);
        }
      })
      .catch((err) => {
        if (isMounted) {
          toast.error(resolveErrorMessage(err, t(I18N_KEYS.DRIVERS.DOWNLOAD_FAILED)));
        }
      })
      .finally(() => {
        if (isMounted) {
          setLoading(false);
        }
      });

    return () => {
      isMounted = false;
    };
  }, [open, databaseType, t, toast]);

  const handleDownload = async (version: string) => {
    setDownloadingVersion(version);
    try {
      const res = await driverService.downloadDriver(databaseType, version);
      toast.success(t(I18N_KEYS.DRIVERS.DOWNLOAD_SUCCESS));
      setInstalled((prev) => [
        ...prev,
        {
          databaseType: res.databaseType,
          fileName: res.fileName,
          version: res.version,
          filePath: res.driverPath,
          fileSize: 0,
          lastModified: new Date().toISOString(),
        },
      ]);
      setAvailable((prev) =>
        prev.map((a) => (a.version === version ? { ...a, installed: true } : a))
      );
    } catch (err) {
      toast.error(resolveErrorMessage(err, t(I18N_KEYS.DRIVERS.DOWNLOAD_FAILED)));
    } finally {
      setDownloadingVersion(null);
    }
  };

  const handleDelete = async (version: string) => {
    try {
      await driverService.deleteDriver(databaseType, version);
      toast.success(t(I18N_KEYS.DRIVERS.DELETE_SUCCESS));
      setInstalled((prev) => prev.filter((d) => d.version !== version));
      setAvailable((prev) =>
        prev.map((a) => (a.version === version ? { ...a, installed: false } : a))
      );
    } catch (err) {
      toast.error(resolveErrorMessage(err, t(I18N_KEYS.DRIVERS.DELETE_FAILED)));
    }
  };

  const handleSelect = (filePath: string) => {
    onSelectDriver(filePath);
    onOpenChange(false);
  };

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[560px] max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{t(I18N_KEYS.DRIVERS.TITLE)}</DialogTitle>
          <DialogDescription>
            {t(I18N_KEYS.DRIVERS.DATABASE_TYPE)}: {databaseType || '-'}
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-6 py-4">
          {loading ? (
            <p className="text-sm text-muted-foreground">{t(I18N_KEYS.EXPLORER.LOADING)}</p>
          ) : (
            <>
              <section>
                <h4 className="text-sm font-medium text-foreground mb-2">
                  {t(I18N_KEYS.DRIVERS.INSTALLED)}
                </h4>
                {installed.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t(I18N_KEYS.DRIVERS.NO_INSTALLED)}</p>
                ) : (
                  <ul className="border border-border rounded-md divide-y divide-border max-h-40 overflow-y-auto">
                    {installed.map((d) => (
                      <li
                        key={d.version}
                        className="flex items-center justify-between px-3 py-2 text-sm"
                      >
                        <span className="text-foreground">
                          {d.fileName} ({d.version})
                        </span>
                        <div className="flex items-center gap-2">
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => handleSelect(d.filePath)}
                          >
                            {t(I18N_KEYS.DRIVERS.SELECT_DRIVER)}
                          </Button>
                          <Button
                            type="button"
                            variant="outline"
                            size="sm"
                            onClick={() => handleDelete(d.version)}
                          >
                            {t(I18N_KEYS.DRIVERS.DELETE)}
                          </Button>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
              <section>
                <h4 className="text-sm font-medium text-foreground mb-2">
                  {t(I18N_KEYS.DRIVERS.AVAILABLE)}
                </h4>
                {available.length === 0 ? (
                  <p className="text-sm text-muted-foreground">{t(I18N_KEYS.DRIVERS.NO_AVAILABLE)}</p>
                ) : (
                  <ul className="border border-border rounded-md divide-y divide-border max-h-40 overflow-y-auto">
                    {available.map((d) => (
                      <li
                        key={d.version}
                        className="flex items-center justify-between px-3 py-2 text-sm"
                      >
                        <span className="text-foreground">{d.version}</span>
                        <Button
                          type="button"
                          variant="outline"
                          size="sm"
                          disabled={d.installed || downloadingVersion === d.version}
                          onClick={() => handleDownload(d.version)}
                        >
                          {downloadingVersion === d.version
                            ? t(I18N_KEYS.DRIVERS.DOWNLOADING)
                            : d.installed
                              ? t(I18N_KEYS.DRIVERS.INSTALLED)
                              : t(I18N_KEYS.DRIVERS.DOWNLOAD)}
                        </Button>
                      </li>
                    ))}
                  </ul>
                )}
              </section>
            </>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
