import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests against the running stack (nginx with the Angular app, backend, users, PostgreSQL).
 * Start it first with `docker compose --profile app up --build --wait`; E2E_BASE_URL points elsewhere.
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  retries: 0,
  // The CI runner has 4 CPUs; the default (half of them) leaves the stack idle most of the time.
  workers: process.env.CI ? 4 : undefined,
  reporter: process.env.CI ? [['list'], ['html', { open: 'never' }]] : 'list',
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8080',
    locale: 'de-DE',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
