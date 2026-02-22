import { useState } from 'react'

const API_BASE = '/v1/payments'

const initialForm = { firstName: '', lastName: '', zipCode: '', cardNumber: '' }
const initialErrors = { firstName: '', lastName: '', zipCode: '', cardNumber: '' }

export default function PaymentForm() {
  const [form, setForm]       = useState(initialForm)
  const [errors, setErrors]   = useState(initialErrors)
  const [loading, setLoading] = useState(false)
  const [response, setResponse] = useState(null)
  const [toast, setToast]     = useState(null)

  function showToast(msg, type = 'success') {
    setToast({ msg, type })
    setTimeout(() => setToast(null), 3500)
  }

  function handleChange(e) {
    const { name, value } = e.target
    let val = value
    if (name === 'cardNumber') {
      val = value.replace(/\D/g, '').substring(0, 16).replace(/(.{4})/g, '$1 ').trim()
    }
    setForm(f => ({ ...f, [name]: val }))
    setErrors(err => ({ ...err, [name]: '' }))
  }

  function validate() {
    const newErrors = { ...initialErrors }
    let valid = true
    if (!form.firstName.trim())  { newErrors.firstName  = 'Required'; valid = false }
    if (!form.lastName.trim())   { newErrors.lastName   = 'Required'; valid = false }
    if (!form.zipCode.trim())    { newErrors.zipCode    = 'Required'; valid = false }
    if (!form.cardNumber.trim()) { newErrors.cardNumber = 'Required'; valid = false }
    else if (form.cardNumber.replace(/\s/g, '').length < 7) {
      newErrors.cardNumber = 'Card number too short'; valid = false
    }
    setErrors(newErrors)
    return valid
  }

  async function handleSubmit(e) {
    e.preventDefault()
    if (!validate()) return
    setLoading(true)
    setResponse(null)

    const payload = {
      firstName:  form.firstName.trim(),
      lastName:   form.lastName.trim(),
      zipCode:    form.zipCode.trim(),
      cardNumber: form.cardNumber.replace(/\s/g, ''),
    }

    try {
      const res = await fetch(API_BASE, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      })
      const ct   = res.headers.get('content-type') || ''
      const body = ct.includes('application/json') ? await res.json() : await res.text()

      setResponse({ status: res.status, body, ok: res.ok })
      res.ok ? showToast('Payment submitted ✓') : showToast(`Error ${res.status}`, 'error')
    } catch (err) {
      setResponse({ status: '—', body: { error: err.message, hint: 'Is Spring Boot running on :8080?' }, ok: false })
      showToast('Could not reach API', 'error')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={s.wrapper}>
      <p style={s.labelTop}>// payment service</p>
      <h1 style={s.h1}>Submit Payment</h1>
      <p style={s.subtitle}>All fields are required.</p>

      <form style={s.card} onSubmit={handleSubmit} noValidate>
        <p style={s.cardTitle}>Cardholder Details</p>

        <div style={s.row}>
          <Field label="First Name" name="firstName" value={form.firstName}
            placeholder="put your firstName here" error={errors.firstName} onChange={handleChange} />
          <Field label="Last Name"  name="lastName"  value={form.lastName}
            placeholder="put your lastName here"      error={errors.lastName}  onChange={handleChange} />
        </div>

        <Field label="ZIP Code" name="zipCode" value={form.zipCode}
          placeholder="put your zipCode here" error={errors.zipCode} onChange={handleChange} />

        <div style={s.divider} />
        <p style={s.cardTitle}>Card Details</p>

        <Field label="Card Number" name="cardNumber" value={form.cardNumber}
          placeholder="put your card number here" error={errors.cardNumber}
          onChange={handleChange} suffix="💳" />

        <button style={{ ...s.btn, opacity: loading ? 0.5 : 1 }} type="submit" disabled={loading}>
          {loading && <span style={s.spinner} />}
          {loading ? 'Processing…' : 'Submit Payment'}
        </button>
      </form>

      {response && (
        <div style={s.responsePanel}>
          <p style={s.responseLabel}>API Response</p>
          <span style={{ ...s.badge, ...(response.ok ? s.badgeOk : s.badgeErr) }}>
            <span style={s.dot} />
            HTTP {response.status}
          </span>
          <pre style={s.responseBody}>
            {typeof response.body === 'object'
              ? JSON.stringify(response.body, null, 2)
              : response.body}
          </pre>
        </div>
      )}

      {toast && (
        <div style={{ ...s.toast, ...(toast.type === 'error' ? s.toastErr : s.toastOk) }}>
          {toast.msg}
        </div>
      )}
    </div>
  )
}

function Field({ label, name, value, placeholder, error, onChange, suffix }) {
  return (
    <div style={s.field}>
      <label style={s.label}>{label}</label>
      <div style={{ position: 'relative' }}>
        <input
          style={{ ...s.input, ...(error ? s.inputErr : {}) }}
          name={name} value={value} placeholder={placeholder}
          onChange={onChange} autoComplete="off"
        />
        {suffix && <span style={s.suffix}>{suffix}</span>}
      </div>
      <span style={s.fieldError}>{error || ''}</span>
    </div>
  )
}

// ─── Styles ──────────────────────────────────────────────────────────────────
const s = {
  wrapper:       { width: '100%', maxWidth: 460, animation: 'fadeUp .5s ease both' },
  labelTop:      { fontFamily: 'var(--mono)', fontSize: 11, letterSpacing: '0.18em', color: 'var(--accent)', textTransform: 'uppercase', marginBottom: 10 },
  h1:            { fontSize: 28, fontWeight: 700, letterSpacing: '-0.03em', marginBottom: 6 },
  subtitle:      { fontSize: 13, color: 'var(--muted)', marginBottom: 32, fontWeight: 300 },
  card:          { background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 16, padding: 28, marginBottom: 16 },
  cardTitle:     { fontFamily: 'var(--mono)', fontSize: 10, letterSpacing: '0.15em', color: 'var(--muted)', textTransform: 'uppercase', marginBottom: 20 },
  row:           { display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 },
  field:         { display: 'flex', flexDirection: 'column', gap: 6, marginBottom: 14 },
  label:         { fontSize: 11, fontWeight: 600, color: 'var(--muted)', letterSpacing: '0.06em', textTransform: 'uppercase' },
  input:         { background: 'var(--bg)', border: '1px solid var(--border)', borderRadius: 8, padding: '11px 14px', fontFamily: 'var(--mono)', fontSize: 14, color: 'var(--text)', outline: 'none', width: '100%', transition: 'border-color .2s' },
  inputErr:      { borderColor: 'var(--error)' },
  suffix:        { position: 'absolute', right: 12, top: '50%', transform: 'translateY(-50%)', fontSize: 18, pointerEvents: 'none' },
  fieldError:    { fontSize: 11, color: 'var(--error)', fontFamily: 'var(--mono)', minHeight: 14 },
  divider:       { height: 1, background: 'var(--border)', margin: '20px 0' },
  btn:           { width: '100%', padding: 14, background: 'var(--accent)', color: '#000', border: 'none', borderRadius: 10, fontFamily: 'var(--sans)', fontSize: 14, fontWeight: 700, letterSpacing: '0.04em', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, marginTop: 20, transition: 'opacity .2s' },
  spinner:       { width: 16, height: 16, borderRadius: '50%', border: '2px solid rgba(0,0,0,.3)', borderTopColor: '#000', animation: 'spin .7s linear infinite', display: 'inline-block' },
  responsePanel: { background: 'var(--card)', border: '1px solid var(--border)', borderRadius: 12, padding: '16px 18px' },
  responseLabel: { fontFamily: 'var(--mono)', fontSize: 10, color: 'var(--muted)', letterSpacing: '0.12em', textTransform: 'uppercase', marginBottom: 8 },
  responseBody:  { fontFamily: 'var(--mono)', fontSize: 12, color: 'var(--accent)', whiteSpace: 'pre-wrap', wordBreak: 'break-all', lineHeight: 1.7 },
  badge:         { display: 'inline-flex', alignItems: 'center', gap: 5, fontFamily: 'var(--mono)', fontSize: 11, padding: '3px 10px', borderRadius: 20, marginBottom: 10 },
  badgeOk:       { background: 'var(--accent-dim)', color: 'var(--accent)' },
  badgeErr:      { background: 'rgba(255,92,110,.12)', color: 'var(--error)' },
  dot:           { width: 6, height: 6, borderRadius: '50%', background: 'currentColor' },
  toast:         { position: 'fixed', bottom: 28, left: '50%', transform: 'translateX(-50%)', padding: '12px 20px', borderRadius: 10, fontSize: 13, fontWeight: 500, fontFamily: 'var(--mono)', whiteSpace: 'nowrap', zIndex: 999 },
  toastOk:       { background: 'var(--accent)', color: '#000' },
  toastErr:      { background: 'var(--error)', color: '#fff' },
}