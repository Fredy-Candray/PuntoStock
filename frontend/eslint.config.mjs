import tseslint from 'typescript-eslint';
export default [
  { ignores: ['dist/**', 'node_modules/**', '.angular/**'] },
  {
    files: ['src/**/*.ts'],
    languageOptions: { parser: tseslint.parser },
    rules: {
      'no-debugger': 'error',
      'no-eval': 'error',
      eqeqeq: ['error', 'always', { null: 'ignore' }],
    },
  },
];
