import vueParser from 'vue-eslint-parser';
import tsParser from '@typescript-eslint/parser';

const viewsRestrictedImports = [
  {
    group: ['@/views', '@/views/**'],
    message:
      'Do not import from @/views/. Use components/, composables/, or services/ instead.'
  },
  {
    group: ['@/api', '@/api/**'],
    message: 'Views must not import @/api. Use composables/services instead.'
  },
  {
    group: ['@/mock', '@/mock/**'],
    message: 'Views must not import @/mock. Use composables/services instead.'
  }
];

/** @type {import('eslint').Linter.Config[]} */
export default [
  {
    files: ['src/**/*.ts', 'src/**/*.vue'],
    languageOptions: {
      parser: vueParser,
      parserOptions: {
        parser: tsParser,
        ecmaVersion: 'latest',
        sourceType: 'module',
        extraFileExtensions: ['.vue']
      }
    },
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/views', '@/views/**'],
              message:
                'Do not import from @/views/. Use components/, composables/, or services/ instead.'
            }
          ]
        }
      ]
    }
  },
  {
    files: ['src/views/**/*.vue'],
    rules: {
      'no-restricted-imports': ['error', { patterns: viewsRestrictedImports }]
    }
  },
  {
    files: ['src/components/**/*.vue'],
    rules: {
      'no-restricted-imports': [
        'error',
        {
          patterns: [
            {
              group: ['@/api', '@/api/**'],
              message: 'Components must not import @/api. Use composables or services instead.'
            }
          ]
        }
      ]
    }
  }
];
