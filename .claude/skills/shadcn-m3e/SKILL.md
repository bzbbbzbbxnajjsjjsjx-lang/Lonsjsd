---
name: shadcn-m3e
description: Build web UI with shadcn M3E (Crysta1221/shadcn-m3e), the shadcn/ui registry of Material 3 Expressive React components (namespace @m3e). Use when adding, styling or reviewing Material 3 / M3 Expressive components in a React 19 + Tailwind v4 web project, including this repo's Next.js app, or when the user mentions shadcn-m3e, @m3e/<name>, M3eProvider or the M3E registry. Not for Android Compose code (notlar-android, rss-android); those use androidx material3 directly.
---

# shadcn M3E

Material 3 Expressive components rebuilt on shadcn/ui: React 19, Tailwind CSS v4, Base UI. Distributed as a shadcn registry; the source is copied into the project.

- Docs: https://shadcn-m3e.crystaworld.dev
- Registry index: https://shadcn-m3e.crystaworld.dev/r/registry.json
- Item JSON: https://shadcn-m3e.crystaworld.dev/r/<name>.json
- Source: https://github.com/Crysta1221/shadcn-m3e (`packages/m3e/src/components`), MIT

Before writing a component, read its item JSON or source from the registry instead of guessing props. The API changes faster than training data.

## Setup in a project

1. The project needs shadcn initialised (`components.json`) and an `@/` alias. For a new one: `npx shadcn@latest init -b base -p maia`.
2. Register the namespace in `components.json`:
   ```json
   { "registries": { "@m3e": "https://shadcn-m3e.crystaworld.dev/r/{name}.json" } }
   ```
3. Install the base once: `npx shadcn@latest add @m3e/base` (styles, cn, theme, icon).
4. Import the styles in the main CSS after Tailwind:
   ```css
   @import "tailwindcss";
   @import "tw-animate-css";
   @import "shadcn/tailwind.css";
   @import "@fontsource-variable/roboto-flex";
   @import "@fontsource-variable/jetbrains-mono";
   @import "./lib/m3e/m3e.css";
   ```
5. Wrap the app in `<M3eProvider>` from `@/components/m3e/m3e-provider` (light/dark through `ColorModeProvider`, palette through `M3ThemeProvider`). Tooltips need `TooltipProvider`, snackbars need `Toaster`.
6. Add components: `npx shadcn@latest add @m3e/button @m3e/card`, or everything with `@m3e/all`. Image-based themes are opt-in: `@m3e/use-image-theme`.
7. Icons are bundled Material Symbols Rounded. Add `"gen:icons": "node scripts/icons.mjs"` and `"check:icons": "node scripts/icons.mjs --check"` to package.json and run `gen:icons` after using a new icon. Icon names built at runtime need a `// @icons a b c` comment.

Check it works: `<Button>` from `@/components/m3e/button` should morph its corners when pressed and show a ripple. If it looks flat, the CSS import is missing.

## Rules when using the components

- Import from `@/components/m3e/*`, never `@/components/ui/*`. `cn` comes from `@/lib/m3e/cn`.
- Colors only through role utilities (`bg-primary-container`, `text-on-surface-variant`). No hex values.
- Corners use the M3 scale `rounded-xs … rounded-4xl`. Type uses `text-<role>-<size>` (`text-body-medium`, `-emphasized` variants). Elevation uses `shadow-elevation-0…5`.
- Motion is spring based: `motion-spatial-fast|default|slow` for position, size and shape; `motion-effects-*` for color and opacity. Honour `prefers-reduced-motion`.
- Press state is the `data-press` attribute set by `<Ripple />`; do not style `:active`. Interactive controls get `<Ripple />`, `state-layer` and `focus-ring`.
- Selected toggles swap shape (round to square) and fill their icon (`<Icon fill="auto" />`).
- Base UI selected states are `data-pressed` / `data-selected`; use `data-[selected]:` for select and combobox items.
- When M3E values are in doubt, the Compose Material3 tokens (`androidx/androidx` → `compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/*.kt`) are the source of truth.

## Components in the registry

`styles`, `cn`, `shapes`, `shape`, `icon`, `theme`, `use-mobile`, `use-image-theme`, `accordion`, `alert-dialog`, `alert`, `app-bar`, `aspect-ratio`, `attachment`, `avatar`, `badge`, `breadcrumb`, `bubble`, `button-group`, `button`, `calendar`, `card`, `carousel`, `chart`, `checkbox`, `chip`, `circular-progress`, `collapsible`, `combobox`, `command`, `context-menu`, `date-picker`, `dialog`, `drawer`, `dropdown-menu`, `empty`, `expressive-carousel`, `fab-menu`, `fab`, `field`, `hover-card`, `input-group`, `input-otp`, `input`, `item`, `kbd`, `label`, `loading-indicator`, `marker`, `menubar`, `message-scroller`, `message`, `native-select`, `navigation-menu`, `navigation`, `pagination`, `popover`, `portal-container`, `progress`, `questionnaire`, `radio-group`, `resizable`, `ripple`, `scroll-area`, `search`, `select`, `separator`, `sheet`, `side-sheet`, `sidebar`, `skeleton`, `slider`, `sonner`, `spinner`, `split-button`, `switch`, `table`, `tabs`, `text-field`, `textarea`, `time-picker`, `toast`, `toggle-group`, `toggle`, `toolbar`, `tooltip`, `use-standard-group`, `base`, `all`

## In this repository

The root Next.js app (m3e-canvas) already uses React 19 and Tailwind v4. Read `AGENTS.md` and `node_modules/next/dist/docs/` first, since this Next.js version differs from older ones. `npx` works here; the bun-only rule applies inside the shadcn-m3e repository itself, not to projects that consume it.
