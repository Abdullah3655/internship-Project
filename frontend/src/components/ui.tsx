import type {
  CSSProperties,
  ButtonHTMLAttributes,
  InputHTMLAttributes,
  ReactNode,
  SelectHTMLAttributes,
  TextareaHTMLAttributes,
} from 'react';
import { useEffect } from 'react';
import { createPortal } from 'react-dom';

export function Button({
  variant = 'primary',
  size = 'md',
  className = '',
  loading = false,
  children,
  disabled,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: 'primary' | 'secondary' | 'ghost' | 'danger';
  size?: 'sm' | 'md';
  loading?: boolean;
}) {
  return (
    <button
      className={`ui-btn ui-btn-${variant} ui-btn-${size} ${className}`}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...props}
    >
      {loading ? <span className="ui-btn-spinner" aria-hidden /> : null}
      {children}
    </button>
  );
}

export function Input(props: InputHTMLAttributes<HTMLInputElement>) {
  return <input className="ui-input" {...props} />;
}

export function Select(props: SelectHTMLAttributes<HTMLSelectElement>) {
  return <select className="ui-input" {...props} />;
}

export function TextArea(props: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className="ui-input ui-textarea" {...props} />;
}

export function Field({
  label,
  hint,
  children,
}: {
  label: string;
  hint?: string;
  children: ReactNode;
}) {
  return (
    <label className="ui-field">
      <span className="ui-label">{label}</span>
      {children}
      {hint ? <span className="ui-hint">{hint}</span> : null}
    </label>
  );
}

export function Badge({
  tone = 'neutral',
  children,
}: {
  tone?: 'neutral' | 'accent' | 'info' | 'warn' | 'danger' | 'success';
  children: ReactNode;
}) {
  return <span className={`ui-badge ui-badge-${tone}`}>{children}</span>;
}

export function Panel({
  children,
  className = '',
  style,
}: {
  children: ReactNode;
  className?: string;
  style?: CSSProperties;
}) {
  return (
    <section className={`ui-panel ${className}`} style={style}>
      {children}
    </section>
  );
}

export function EmptyState({
  title,
  body,
  action,
}: {
  title: string;
  body: string;
  action?: ReactNode;
}) {
  return (
    <div className="ui-empty fade-up">
      <h3>{title}</h3>
      <p>{body}</p>
      {action}
    </div>
  );
}

export function LoadingBlock({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="ui-loading" role="status">
      <span className="ui-spinner" />
      {label}
    </div>
  );
}

export function RefreshHint({ show }: { show: boolean }) {
  if (!show) return null;
  return <span className="ui-refresh-hint fade-in">Updating…</span>;
}

export function ErrorBanner({ message }: { message: string }) {
  return <div className="ui-error fade-in">{message}</div>;
}

export function InfoBanner({ message }: { message: string }) {
  return <div className="ui-info fade-in">{message}</div>;
}

export function Avatar({
  label,
  size = 36,
}: {
  label: string;
  size?: number;
}) {
  return (
    <span
      className="ui-avatar"
      style={{ width: size, height: size, fontSize: size * 0.34 }}
      aria-hidden
    >
      {label}
    </span>
  );
}

export function Modal({
  title,
  children,
  onClose,
  wide,
}: {
  title: string;
  children: ReactNode;
  onClose: () => void;
  wide?: boolean;
}) {
  useEffect(() => {
    const previous = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    return () => {
      document.body.style.overflow = previous;
    };
  }, []);

  return createPortal(
    <div className="ui-modal-backdrop fade-in" onClick={onClose}>
      <div
        className={`ui-modal ${wide ? 'ui-modal-wide' : ''} fade-up`}
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal
        aria-labelledby="modal-title"
      >
        <header className="ui-modal-head">
          <h2 id="modal-title">{title}</h2>
          <button type="button" className="ui-icon-btn" onClick={onClose} aria-label="Close">
            ×
          </button>
        </header>
        <div className="ui-modal-body">{children}</div>
      </div>
    </div>,
    document.body,
  );
}

export function ConfirmModal({
  title,
  message,
  confirmLabel = 'Confirm',
  danger = false,
  loading = false,
  onConfirm,
  onClose,
}: {
  title: string;
  message: string;
  confirmLabel?: string;
  danger?: boolean;
  loading?: boolean;
  onConfirm: () => void | Promise<void>;
  onClose: () => void;
}) {
  return (
    <Modal
      title={title}
      onClose={() => {
        if (!loading) onClose();
      }}
    >
      <p style={{ marginBottom: '1rem' }}>{message}</p>
      <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end' }}>
        <Button variant="ghost" disabled={loading} onClick={onClose}>
          Cancel
        </Button>
        <Button
          variant={danger ? 'danger' : 'primary'}
          loading={loading}
          onClick={() => void onConfirm()}
        >
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
