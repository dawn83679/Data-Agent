import { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { I18N_KEYS } from '../../constants/i18nKeys';
import { useForm, SubmitHandler } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import * as z from 'zod';
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '../ui/Dialog';
import { Button } from '../ui/Button';
import { Input } from '../ui/Input';
import { connectionService } from '../../services/connection.service';
import { driverService } from '../../services/driver.service';
import { useWorkspaceStore } from '../../store/workspaceStore';
import { useToast } from '../../hooks/useToast';
import { resolveErrorMessage } from '../../lib/errorMessage';
import { DriverManageModal } from './DriverManageModal';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

const connectionSchema = z.object({
  name: z.string().min(1, 'Name is required').max(100),
  dbType: z.string().min(1, 'Database type is required'),
  host: z.string().min(1, 'Host is required'),
  port: z.union([z.string(), z.number()]).transform((val) => typeof val === 'string' ? parseInt(val, 10) : val).pipe(z.number().min(1).max(65535)),
  database: z.string().optional(),
  username: z.string().min(1, 'Username is required'),
  password: z.string().optional(),
  driverJarPath: z.string().min(1, 'Driver path is required'),
  timeout: z.union([z.string(), z.number()]).transform((val) => typeof val === 'string' ? parseInt(val, 10) : val).pipe(z.number().min(1).max(300)),
});

type ConnectionFormValues = z.infer<typeof connectionSchema>;

export type ConnectionFormMode = 'create' | 'edit';

interface ConnectionFormModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  mode: ConnectionFormMode;
  editId?: number;
  initialDbType?: string;
  onSuccess?: () => void;
}

const DEFAULT_TIMEOUT = 30;

function getJdbcUrl(values: Partial<ConnectionFormValues>): string {
  const { dbType, host, port, database } = values;
  if (!dbType || !host) return '';

  const portStr = port ? `:${port}` : '';
  const dbStr = database ? `/${database}` : '';

  const type = dbType.toLowerCase();
  if (type.includes('mysql')) {
    return `jdbc:mysql://${host}${portStr}${dbStr}`;
  } else if (type.includes('postgresql') || type.includes('postgres')) {
    return `jdbc:postgresql://${host}${portStr}${dbStr}`;
  } else if (type.includes('oracle')) {
    return `jdbc:oracle:thin:@${host}${portStr}:${database || 'ORCL'}`;
  } else if (type.includes('sqlserver')) {
    return `jdbc:sqlserver://${host}${portStr};databaseName=${database || ''}`;
  }

  return `jdbc:${type}://${host}${portStr}${dbStr}`;
}

export function ConnectionFormModal({
  open,
  onOpenChange,
  mode,
  editId,
  initialDbType,
  onSuccess,
}: ConnectionFormModalProps) {
  const { t } = useTranslation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const dbTypes = useWorkspaceStore((s) => s.supportedDbTypes);
  
  const [step, setStep] = useState<'select-type' | 'form'>('select-type');
  const [driverModalOpen, setDriverModalOpen] = useState(false);
  const [isNameManuallyEdited, setIsNameManuallyEdited] = useState(false);

  const {
    register,
    handleSubmit,
    reset,
    setValue,
    watch,
    formState: { errors },
  } = useForm<ConnectionFormValues>({
    // @ts-ignore - Zod transform causes type mismatch with RHF
    resolver: zodResolver(connectionSchema),
    defaultValues: {
      port: 3306,
      timeout: DEFAULT_TIMEOUT,
    },
  });

  const formValues = watch();

  // Fetch connection data if in edit mode
  const { data: connection } = useQuery({
    queryKey: ['connection', editId],
    queryFn: () => connectionService.getConnectionById(editId!),
    enabled: open && mode === 'edit' && !!editId,
  });

  useEffect(() => {
    if (connection) {
      reset({
        name: connection.name,
        dbType: connection.dbType,
        host: connection.host,
        port: connection.port,
        database: connection.database || '',
        username: connection.username,
        driverJarPath: connection.driverJarPath,
        timeout: connection.timeout,
      });
      setStep('form');
      setIsNameManuallyEdited(true);
    }
  }, [connection, reset]);

  useEffect(() => {
    if (open && mode === 'create') {
      reset({
        dbType: initialDbType || '',
        port: 3306,
        timeout: DEFAULT_TIMEOUT,
        host: 'localhost',
        name: 'localhost@3306',
        database: '',
        username: '',
        password: '',
        driverJarPath: '',
      });
      setStep(initialDbType ? 'form' : 'select-type');
      setIsNameManuallyEdited(false);
    }
  }, [open, mode, initialDbType, reset]);

  // Auto-fill driver path when dbType changes
  useEffect(() => {
    if (formValues.dbType && mode === 'create') {
      driverService.listInstalledDrivers(formValues.dbType).then((drivers) => {
        if (drivers?.[0]) {
          setValue('driverJarPath', drivers[0].filePath);
        }
      });
    }
  }, [formValues.dbType, mode, setValue]);

  // Auto-fill name when host/port changes
  useEffect(() => {
    if (mode === 'create' && !isNameManuallyEdited) {
      const host = formValues.host || 'localhost';
      const port = formValues.port || '3306';
      setValue('name', `${host}@${port}`);
    }
  }, [formValues.host, formValues.port, isNameManuallyEdited, mode, setValue]);

  const testMutation = useMutation({
    mutationFn: (values: ConnectionFormValues) => connectionService.testConnection(values as any),
    onSuccess: (res) => {
      if (res.status === 'SUCCEEDED') {
        toast.success(t(I18N_KEYS.CONNECTIONS.TEST_SUCCEEDED) + ` (${t(I18N_KEYS.CONNECTIONS.TEST_PING_MS, { ping: res.ping })})`);
      } else {
        toast.error(t(I18N_KEYS.CONNECTIONS.TEST_FAILED));
      }
    },
    onError: (err) => {
      toast.error(resolveErrorMessage(err, t(I18N_KEYS.CONNECTIONS.TEST_FAILED)));
    },
  });

  const submitMutation = useMutation({
    mutationFn: (values: ConnectionFormValues) => {
      if (mode === 'edit' && editId) {
        return connectionService.updateConnection({
          ...values,
          connectionId: editId,
        } as any);
      }
      return connectionService.createConnection(values as any);
    },
    onSuccess: () => {
      toast.success(mode === 'edit' ? t(I18N_KEYS.CONNECTIONS.UPDATE_SUCCESS) : t(I18N_KEYS.CONNECTIONS.CREATE_SUCCESS));
      queryClient.invalidateQueries({ queryKey: ['connections'] });
      onOpenChange(false);
      onSuccess?.();
    },
    onError: (err) => {
      toast.error(resolveErrorMessage(err, mode === 'edit' ? t(I18N_KEYS.CONNECTIONS.UPDATE_FAILED) : t(I18N_KEYS.CONNECTIONS.CREATE_FAILED)));
    },
  });

  const onFormSubmit: SubmitHandler<any> = (values) => {
    submitMutation.mutate(values);
  };

  const handleTest = async () => {
    const values = watch();
    // Validate required fields for testing
    if (!values.host || !values.username || !values.driverJarPath) {
      toast.warning(t(I18N_KEYS.ERROR.VALIDATION));
      return;
    }
    testMutation.mutate(values as ConnectionFormValues);
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="sm:max-w-[500px] max-h-[90vh] overflow-y-auto">
          <DialogHeader>
            <DialogTitle>
              {step === 'select-type' ? t(I18N_KEYS.CONNECTIONS.SELECT_DB_TYPE) : mode === 'edit' ? t(I18N_KEYS.CONNECTIONS.EDIT) : t(I18N_KEYS.CONNECTIONS.NEW)}
            </DialogTitle>
          </DialogHeader>

          {step === 'select-type' ? (
            <div className="py-4 grid gap-4">
              <div className="grid gap-2">
                <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.DB_TYPE)}</label>
                <select
                  className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm"
                  onChange={(e) => {
                    setValue('dbType', e.target.value);
                    setStep('form');
                  }}
                  value={formValues.dbType}
                >
                  <option value="">{t(I18N_KEYS.CONNECTIONS.SELECT_DB_TYPE_DESC)}</option>
                  {dbTypes.map((opt) => (
                    <option key={opt.code} value={opt.code}>{opt.displayName}</option>
                  ))}
                </select>
              </div>
              <DialogFooter>
                <Button variant="outline" onClick={() => onOpenChange(false)}>{t(I18N_KEYS.CONNECTIONS.CANCEL)}</Button>
              </DialogFooter>
            </div>
          ) : (
            <form onSubmit={handleSubmit(onFormSubmit)} className="grid gap-4 py-4">
              <div className="grid gap-2">
                <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.NAME)}</label>
                <Input 
                  {...register('name')} 
                  onChange={(e) => {
                    register('name').onChange(e);
                    setIsNameManuallyEdited(true);
                  }}
                />
                {errors.name && <span className="text-xs text-destructive">{errors.name.message}</span>}
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="grid gap-2">
                  <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.HOST)}</label>
                  <Input {...register('host')} />
                  {errors.host && <span className="text-xs text-destructive">{errors.host.message}</span>}
                </div>
                <div className="grid gap-2">
                  <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.PORT)}</label>
                  <Input type="number" {...register('port')} />
                  {errors.port && <span className="text-xs text-destructive">{errors.port.message}</span>}
                </div>
              </div>

              <div className="grid gap-2">
                <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.DATABASE)}</label>
                <Input {...register('database')} />
              </div>

              <div className="grid gap-2">
                <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.USERNAME)}</label>
                <Input {...register('username')} />
                {errors.username && <span className="text-xs text-destructive">{errors.username.message}</span>}
              </div>

              <div className="grid gap-2">
                <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.PASSWORD)}</label>
                <Input type="password" {...register('password')} autoComplete="off" />
              </div>

              <div className="grid grid-cols-4 gap-4">
                <div className="col-span-3 grid gap-2">
                  <div className="flex items-center justify-between">
                    <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.DRIVER_JAR_PATH)}</label>
                    <Button type="button" variant="outline" size="sm" className="h-7 text-xs" onClick={() => setDriverModalOpen(true)}>
                      {t(I18N_KEYS.CONNECTIONS.MANAGE_DRIVERS)}
                    </Button>
                  </div>
                  <Input {...register('driverJarPath')} />
                  {errors.driverJarPath && <span className="text-xs text-destructive">{errors.driverJarPath.message}</span>}
                </div>
                <div className="col-span-1 grid gap-2">
                  <label className="text-sm font-medium">{t(I18N_KEYS.CONNECTIONS.TIMEOUT_SHORT)}</label>
                  <Input type="number" {...register('timeout')} />
                </div>
              </div>

              <div className="grid gap-2 p-3 rounded-md bg-muted/50 border border-border">
                <div className="text-[10px] font-bold text-muted-foreground uppercase tracking-wider">{t(I18N_KEYS.CONNECTIONS.JDBC_URL)}</div>
                <div className="text-xs font-mono break-all text-foreground/80">{getJdbcUrl(formValues)}</div>
              </div>

              <DialogFooter className="gap-2">
                <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>{t(I18N_KEYS.CONNECTIONS.CANCEL)}</Button>
                <Button type="button" variant="outline" onClick={handleTest} disabled={testMutation.isPending}>
                  {testMutation.isPending ? t(I18N_KEYS.CONNECTIONS.TESTING) : t(I18N_KEYS.CONNECTIONS.TEST_CONNECTION)}
                </Button>
                <Button type="submit" disabled={submitMutation.isPending}>
                  {submitMutation.isPending ? t(I18N_KEYS.CONNECTIONS.SAVING) : mode === 'edit' ? t(I18N_KEYS.CONNECTIONS.UPDATE) : t(I18N_KEYS.CONNECTIONS.CREATE)}
                </Button>
              </DialogFooter>
            </form>
          )}
        </DialogContent>
      </Dialog>
      <DriverManageModal
        open={driverModalOpen}
        onOpenChange={setDriverModalOpen}
        databaseType={formValues.dbType}
        onSelectDriver={(path) => setValue('driverJarPath', path)}
      />
    </>
  );
}
