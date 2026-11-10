import type { InputHTMLAttributes, ReactNode } from 'react'

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  icon?: ReactNode
}

export function Field({ label, error, icon, id, className = '', ...props }: FieldProps) {
  const inputId = id || props.name
  return (
    <label className={`field ${className}`} htmlFor={inputId}>
      <span>{label}</span>
      <span className="field-control">
        {icon}
        <input id={inputId} aria-invalid={Boolean(error)} {...props} />
      </span>
      {error ? <small className="field-error">{error}</small> : null}
    </label>
  )
}
