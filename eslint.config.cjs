const ionic = require('@ionic/eslint-config/recommended');

module.exports = [
  {
    ignores: [
      // eslint . --ext ts linted TypeScript only
      '**/*.js',
      '**/*.mjs',
      '**/*.cjs',
      '**/build/**',
      '.build/**',
      '**/dist/**',
      '**/example-app/**',
    ],
  },
  ...ionic,
];
