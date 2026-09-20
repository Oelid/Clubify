import { HttpErrorResponse } from '@angular/common/http';

/**
 * Code stable porté par une erreur du backend (`ProblemDetail`, RFC 9457).
 *
 * <p>Le code est traduit par Transloco ; le message du backend ne sert que de
 * repli, jamais de libellé de référence (frontend/CLAUDE.md).
 */
export const codeDErreur = (echec: unknown): string => {
  if (echec instanceof HttpErrorResponse) {
    const probleme = echec.error as { code?: string } | null;
    if (probleme?.code) {
      return probleme.code;
    }
    if (echec.status === 0) {
      return 'network.unreachable';
    }
    return `http.${echec.status}`;
  }
  return 'error.unexpected';
};

/** Message du backend, à n'afficher que si aucune traduction n'existe. */
export const detailDErreur = (echec: unknown): string | null => {
  if (echec instanceof HttpErrorResponse) {
    const probleme = echec.error as { detail?: string } | null;
    return probleme?.detail ?? null;
  }
  return null;
};
