import { useMemo, useState } from 'react';
import { Database } from 'lucide-react';

interface DatabaseTypeIconProps {
  dbType?: string;
  className?: string;
  fallbackClassName?: string;
}

interface DbIconRule {
  matchers: string[];
  iconUrl: string;
}

const DB_ICON_RULES: DbIconRule[] = [
  { matchers: ['mysql'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mysql/mysql-original.svg' },
  { matchers: ['mariadb'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mariadb/mariadb-original.svg' },
  { matchers: ['postgres', 'postgresql'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/postgresql/postgresql-original.svg' },
  { matchers: ['sqlserver', 'mssql'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/microsoftsqlserver/microsoftsqlserver-plain.svg' },
  { matchers: ['oracle'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/oracle/oracle-original.svg' },
  { matchers: ['sqlite'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/sqlite/sqlite-original.svg' },
  { matchers: ['clickhouse'], iconUrl: 'https://cdn.simpleicons.org/clickhouse/FFCC01' },
  { matchers: ['tidb'], iconUrl: 'https://cdn.simpleicons.org/tidb/EA4E20' },
  { matchers: ['db2'], iconUrl: 'https://cdn.simpleicons.org/ibmdb2/052FAD' },
  { matchers: ['mongodb', 'mongo'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/mongodb/mongodb-original.svg' },
  { matchers: ['redis'], iconUrl: 'https://cdn.jsdelivr.net/gh/devicons/devicon/icons/redis/redis-original.svg' },
];

export function DatabaseTypeIcon({
  dbType,
  className = 'w-4 h-4',
  fallbackClassName = 'text-blue-400',
}: DatabaseTypeIconProps) {
  const [failed, setFailed] = useState(false);

  const iconSrc = useMemo(() => {
    const normalized = (dbType || '').toLowerCase();
    if (!normalized) return '';

    const matchedRule = DB_ICON_RULES.find((rule) =>
      rule.matchers.some((matcher) => normalized.includes(matcher))
    );
    return matchedRule?.iconUrl ?? '';
  }, [dbType]);

  if (!iconSrc || failed) {
    return <Database className={`${className} ${fallbackClassName}`.trim()} />;
  }

  return (
    <img
      src={iconSrc}
      alt={dbType ? `${dbType} icon` : 'database icon'}
      className={`${className} object-contain`}
      onError={() => setFailed(true)}
      loading="lazy"
    />
  );
}
