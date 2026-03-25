import type { QueryClient } from '@tanstack/react-query';
import { databaseService } from '../services/database.service';
import { functionService, type FunctionMetadata } from '../services/function.service';
import { procedureService, type ProcedureMetadata } from '../services/procedure.service';
import { schemaService } from '../services/schema.service';
import { tableService } from '../services/table.service';
import { triggerService, type TriggerMetadata } from '../services/trigger.service';
import { viewService } from '../services/view.service';

const EXPLORER_METADATA_QUERY_ROOT = 'explorer-metadata';
const EXPLORER_METADATA_STALE_TIME_MS = 60 * 1000;

type CachedListOptions = {
  force?: boolean;
  staleTime?: number;
};

const normalizeScope = (value?: string) => value ?? '';

export const getExplorerDatabasesQueryKey = (connectionId: string) =>
  [EXPLORER_METADATA_QUERY_ROOT, 'databases', connectionId] as const;

export const getExplorerSchemasQueryKey = (connectionId: string, catalog?: string) =>
  [EXPLORER_METADATA_QUERY_ROOT, 'schemas', connectionId, normalizeScope(catalog)] as const;

export const getExplorerTablesQueryKey = (connectionId: string, catalog?: string, schema?: string) =>
  [EXPLORER_METADATA_QUERY_ROOT, 'tables', connectionId, normalizeScope(catalog), normalizeScope(schema)] as const;

export const getExplorerViewsQueryKey = (connectionId: string, catalog?: string, schema?: string) =>
  [EXPLORER_METADATA_QUERY_ROOT, 'views', connectionId, normalizeScope(catalog), normalizeScope(schema)] as const;

export const getExplorerFunctionsQueryKey = (connectionId: string, catalog?: string, schema?: string) =>
  [EXPLORER_METADATA_QUERY_ROOT, 'functions', connectionId, normalizeScope(catalog), normalizeScope(schema)] as const;

export const getExplorerProceduresQueryKey = (connectionId: string, catalog?: string, schema?: string) =>
  [EXPLORER_METADATA_QUERY_ROOT, 'procedures', connectionId, normalizeScope(catalog), normalizeScope(schema)] as const;

export const getExplorerTriggersQueryKey = (connectionId: string, catalog?: string, schema?: string) =>
  [EXPLORER_METADATA_QUERY_ROOT, 'triggers', connectionId, normalizeScope(catalog), normalizeScope(schema)] as const;

async function fetchCachedData<T>(
  queryClient: QueryClient,
  queryKey: readonly string[],
  queryFn: () => Promise<T>,
  options?: CachedListOptions
): Promise<T> {
  const { force = false, staleTime = EXPLORER_METADATA_STALE_TIME_MS } = options ?? {};
  return queryClient.fetchQuery({
    queryKey,
    queryFn,
    staleTime: force ? 0 : staleTime,
  });
}

export function getCachedExplorerDatabases(queryClient: QueryClient, connectionId: string): string[] | undefined {
  return queryClient.getQueryData<string[]>(getExplorerDatabasesQueryKey(connectionId));
}

export function getCachedExplorerSchemas(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string
): string[] | undefined {
  return queryClient.getQueryData<string[]>(getExplorerSchemasQueryKey(connectionId, catalog));
}

export function getCachedExplorerTables(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string
): string[] | undefined {
  return queryClient.getQueryData<string[]>(getExplorerTablesQueryKey(connectionId, catalog, schema));
}

export function getCachedExplorerViews(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string
): string[] | undefined {
  return queryClient.getQueryData<string[]>(getExplorerViewsQueryKey(connectionId, catalog, schema));
}

export function getCachedExplorerFunctions(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string
): FunctionMetadata[] | undefined {
  return queryClient.getQueryData<FunctionMetadata[]>(getExplorerFunctionsQueryKey(connectionId, catalog, schema));
}

export function getCachedExplorerProcedures(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string
): ProcedureMetadata[] | undefined {
  return queryClient.getQueryData<ProcedureMetadata[]>(getExplorerProceduresQueryKey(connectionId, catalog, schema));
}

export function getCachedExplorerTriggers(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string
): TriggerMetadata[] | undefined {
  return queryClient.getQueryData<TriggerMetadata[]>(getExplorerTriggersQueryKey(connectionId, catalog, schema));
}

export function fetchExplorerDatabases(
  queryClient: QueryClient,
  connectionId: string,
  options?: CachedListOptions
): Promise<string[]> {
  return fetchCachedData(
    queryClient,
    getExplorerDatabasesQueryKey(connectionId),
    () => databaseService.listDatabases(connectionId),
    options
  );
}

export function fetchExplorerSchemas(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  options?: CachedListOptions
): Promise<string[]> {
  return fetchCachedData(
    queryClient,
    getExplorerSchemasQueryKey(connectionId, catalog),
    () => schemaService.listSchemas(connectionId, catalog),
    options
  );
}

export function fetchExplorerTables(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string,
  options?: CachedListOptions
): Promise<string[]> {
  return fetchCachedData(
    queryClient,
    getExplorerTablesQueryKey(connectionId, catalog, schema),
    () => tableService.listTables(connectionId, catalog, schema),
    options
  );
}

export function fetchExplorerViews(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string,
  options?: CachedListOptions
): Promise<string[]> {
  return fetchCachedData(
    queryClient,
    getExplorerViewsQueryKey(connectionId, catalog, schema),
    () => viewService.listViews(connectionId, catalog, schema),
    options
  );
}

export function fetchExplorerFunctions(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string,
  options?: CachedListOptions
): Promise<FunctionMetadata[]> {
  return fetchCachedData(
    queryClient,
    getExplorerFunctionsQueryKey(connectionId, catalog, schema),
    () => functionService.listFunctions(connectionId, catalog, schema),
    options
  );
}

export function fetchExplorerProcedures(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string,
  options?: CachedListOptions
): Promise<ProcedureMetadata[]> {
  return fetchCachedData(
    queryClient,
    getExplorerProceduresQueryKey(connectionId, catalog, schema),
    () => procedureService.listProcedures(connectionId, catalog, schema),
    options
  );
}

export function fetchExplorerTriggers(
  queryClient: QueryClient,
  connectionId: string,
  catalog?: string,
  schema?: string,
  options?: CachedListOptions
): Promise<TriggerMetadata[]> {
  return fetchCachedData(
    queryClient,
    getExplorerTriggersQueryKey(connectionId, catalog, schema),
    () => triggerService.listTriggers(connectionId, catalog, schema),
    options
  );
}

export function clearExplorerMetadataForConnection(queryClient: QueryClient, connectionId: string) {
  queryClient.removeQueries({ queryKey: [EXPLORER_METADATA_QUERY_ROOT, 'databases', connectionId] });
  queryClient.removeQueries({ queryKey: [EXPLORER_METADATA_QUERY_ROOT, 'schemas', connectionId] });
  queryClient.removeQueries({ queryKey: [EXPLORER_METADATA_QUERY_ROOT, 'tables', connectionId] });
  queryClient.removeQueries({ queryKey: [EXPLORER_METADATA_QUERY_ROOT, 'views', connectionId] });
  queryClient.removeQueries({ queryKey: [EXPLORER_METADATA_QUERY_ROOT, 'functions', connectionId] });
  queryClient.removeQueries({ queryKey: [EXPLORER_METADATA_QUERY_ROOT, 'procedures', connectionId] });
  queryClient.removeQueries({ queryKey: [EXPLORER_METADATA_QUERY_ROOT, 'triggers', connectionId] });
}
