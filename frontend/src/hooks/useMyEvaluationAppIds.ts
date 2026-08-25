import { useEffect, useState } from 'react';
import { applicationsApi } from '../api/applications';
import type { Application } from '../types';

export function useMyEvaluationAppIds(applications: Application[], userId: string | undefined) {
  const [evaluatedAppIds, setEvaluatedAppIds] = useState<Set<string>>(new Set());
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (!userId || applications.length === 0) {
      setEvaluatedAppIds(new Set());
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    (async () => {
      const results = await Promise.all(
        applications.map(async (app) => {
          try {
            const evals = await applicationsApi.listEvaluations(app.id);
            return evals.items.some((ev) => ev.interviewerUserId === userId) ? app.id : null;
          } catch {
            return null;
          }
        }),
      );
      if (!cancelled) {
        setEvaluatedAppIds(new Set(results.filter((id): id is string => id !== null)));
        setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [applications, userId]);

  return { evaluatedAppIds, loading };
}
