/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_AUTH_URL?: string;
  readonly VITE_CANDIDATE_URL?: string;
  readonly VITE_APPLICATION_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
