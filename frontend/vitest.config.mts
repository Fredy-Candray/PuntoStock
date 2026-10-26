import { defineConfig } from 'vitest/config';
import ts from 'typescript';
export default defineConfig({
  plugins: [
    {
      name: 'angular-decorators-tests',
      enforce: 'pre',
      transform(code, id) {
        if (!id.endsWith('.ts') || id.includes('node_modules')) return;
        const result = ts.transpileModule(code, {
          compilerOptions: {
            target: ts.ScriptTarget.ES2022,
            module: ts.ModuleKind.ESNext,
            experimentalDecorators: true,
            useDefineForClassFields: false,
            sourceMap: true,
          },
          fileName: id,
        });
        return {
          code: result.outputText,
          map: result.sourceMapText ? JSON.parse(result.sourceMapText) : null,
        };
      },
    },
  ],
  test: { include: ['tests/**/*.spec.ts'], setupFiles: ['tests/setup.ts'], environment: 'node' },
});
