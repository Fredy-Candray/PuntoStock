import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: 'e2e',
  timeout: 45000,
  globalTimeout: 300000,
  fullyParallel: false,
  use: { baseURL: 'http://127.0.0.1:14300', headless: true, trace: 'off', actionTimeout: 15000 },
  webServer: {
    command: `${process.execPath} node_modules/@angular/cli/bin/ng.js serve --host 127.0.0.1 --port 14300`,
    url: 'http://127.0.0.1:14300',
    timeout: 180000,
    reuseExistingServer: !process.env['CI'],
  },
  reporter: [['list'], ['html', { outputFolder: '../.cache/playwright-report', open: 'never' }]],
  outputDir: '../.cache/playwright-results',
});
