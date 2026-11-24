import type { ReactNode } from 'react'

/** Ocho ilustraciones geométricas del design system para eventos sin imagen. Son arte, no estilos de interfaz. */
const INK = '#0B1410'
const MINT = '#6BF28B'
const BONE = '#F5F7F4'

export const POSTER_ARTS: ReactNode[] = [
  <>
    <rect width="300" height="190" fill={INK} />
    <circle cx="214" cy="82" r="92" fill="none" stroke={MINT} strokeOpacity=".28" strokeWidth="2" />
    <circle cx="214" cy="82" r="64" fill={MINT} />
    <circle cx="238" cy="64" r="58" fill={INK} />
    <rect x="28" y="140" width="120" height="10" rx="5" fill={BONE} fillOpacity=".9" />
    <rect x="28" y="158" width="76" height="10" rx="5" fill={BONE} fillOpacity=".4" />
  </>,
  <>
    <rect width="300" height="190" fill="#111C17" />
    <rect x="40" y="36" width="64" height="200" rx="32" fill="none" stroke={BONE} strokeWidth="2" />
    <rect x="118" y="20" width="64" height="220" rx="32" fill={BONE} />
    <rect x="196" y="36" width="64" height="200" rx="32" fill="none" stroke={BONE} strokeWidth="2" />
    <circle cx="150" cy="70" r="16" fill={MINT} />
  </>,
  <>
    <rect width="300" height="190" fill={INK} />
    <g fill="none" stroke={MINT} strokeWidth="2">
      <circle cx="150" cy="95" r="44" />
      <path d="M150 20v150" strokeOpacity=".6" />
      <path d="M0 95h300" strokeOpacity=".25" />
    </g>
    <circle cx="150" cy="95" r="12" fill={MINT} />
    <rect x="20" y="20" width="60" height="150" rx="30" fill={BONE} fillOpacity=".08" />
    <rect x="220" y="20" width="60" height="150" rx="30" fill={BONE} fillOpacity=".08" />
  </>,
  <>
    <rect width="300" height="190" fill={INK} />
    <g fill={BONE}>
      <rect x="34" y="70" width="14" height="90" rx="7" fillOpacity=".9" />
      <rect x="62" y="40" width="14" height="150" rx="7" fillOpacity=".5" />
      <rect x="90" y="90" width="14" height="70" rx="7" />
      <rect x="118" y="24" width="14" height="150" rx="7" fill={MINT} />
      <rect x="146" y="60" width="14" height="100" rx="7" fillOpacity=".7" />
      <rect x="174" y="84" width="14" height="80" rx="7" />
      <rect x="202" y="30" width="14" height="140" rx="7" fill={MINT} fillOpacity=".6" />
      <rect x="230" y="70" width="14" height="90" rx="7" fillOpacity=".5" />
    </g>
  </>,
  <>
    <rect width="300" height="190" fill={INK} />
    <g fill="none" strokeWidth="3" strokeLinecap="round">
      <path d="M-10 70c40-40 80 40 120 0s80-40 120 0 60 20 90-10" stroke={MINT} />
      <path d="M-10 105c40-40 80 40 120 0s80-40 120 0 60 20 90-10" stroke={BONE} strokeOpacity=".7" />
      <path d="M-10 140c40-40 80 40 120 0s80-40 120 0 60 20 90-10" stroke={BONE} strokeOpacity=".3" />
    </g>
  </>,
  <>
    <rect width="300" height="190" fill={BONE} />
    <path d="M0 190V110a110 110 0 0 1 220 0v80Z" fill={INK} />
    <path d="M60 190v-60a50 50 0 0 1 100 0v60Z" fill={MINT} />
    <circle cx="246" cy="52" r="26" fill={INK} />
  </>,
  <>
    <rect width="300" height="190" fill="#18261F" />
    <rect x="0" y="120" width="300" height="70" fill={INK} />
    <circle cx="90" cy="96" r="46" fill={BONE} />
    <circle cx="90" cy="96" r="46" fill="none" stroke={MINT} strokeWidth="3" strokeDasharray="6 8" />
    <circle cx="220" cy="70" r="26" fill={MINT} />
    <rect x="40" y="150" width="220" height="8" rx="4" fill={BONE} fillOpacity=".5" />
  </>,
  <>
    <rect width="300" height="190" fill={INK} />
    <circle cx="90" cy="95" r="52" fill="none" stroke={BONE} strokeWidth="2" />
    <circle cx="150" cy="95" r="52" fill="none" stroke={MINT} strokeWidth="2" />
    <circle cx="210" cy="95" r="52" fill="none" stroke={BONE} strokeOpacity=".6" strokeWidth="2" />
    <circle cx="150" cy="95" r="10" fill={MINT} />
  </>,
]
