# Ledgerly Design System

## Direction

A calm, premium personal-finance interface: warm white surfaces, soft neutral canvas, deep navy typography, and restrained blue/violet accents. Information is spacious, readable, and private-feeling.

## Foundations

Tokens and responsive styles are centralized in `frontend/src/styles.css`.

- Canvas: soft cool neutral (`#f7f8fb`)
- Surfaces: white with subtle cool-gray borders
- Primary text: deep navy (`#17243b`)
- Interactive accent: blue (`#4667e8`)
- Secondary accent: violet (`#785ce8`)
- Positive state: restrained mint green
- Display typography: Manrope; interface typography: DM Sans
- Controls: 8–9px corner radius; cards: 10–15px
- Motion respects `prefers-reduced-motion`

## Components and layouts

Shared controls live under `frontend/src/components/ui`. Navigation components live under `frontend/src/components/navigation`. Landing sections, authentication, and dashboard patterns use the same type, color, spacing, and focus styles.

The public landing page uses a paper-to-digital visual story and responsive sections. The signed-in shell is an individual account experience, with a mobile navigation drawer and real-data-safe empty states.

## Accessibility

Use semantic landmarks, labels, keyboard-accessible controls, visible focus rings, adequate text contrast, and responsive layouts without horizontal overflow. Marketing receipt art is illustrative and must never be treated as saved customer data.

## Data integrity

Empty-state illustrations are visual guidance only. Do not present sample amounts, charts, vendors, or activity as authenticated customer records. Dashboard metrics and reports must be backed by that customer's saved data.
