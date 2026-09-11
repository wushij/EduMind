<template>
  <div
    class="color-icon"
    :class="[`color-icon--${size}`, `color-icon--rounded-${rounded}`]"
    :style="{ background: backgroundStyle }"
  >
    <el-icon class="color-icon__glyph">
      <component :is="resolvedIcon" />
    </el-icon>
  </div>
</template>

<script setup lang="ts">
import { computed, type Component } from 'vue';
import * as ElementPlusIcons from '@element-plus/icons-vue';
import { MagicStick } from '@element-plus/icons-vue';

export type IconTheme =
  | 'blue'
  | 'cyan'
  | 'emerald'
  | 'amber'
  | 'rose'
  | 'purple'
  | 'indigo'
  | 'teal';

const THEME_GRADIENTS: Record<IconTheme, string> = {
  blue: 'linear-gradient(135deg, #3B82F6 0%, #1D4ED8 100%)',
  cyan: 'linear-gradient(135deg, #06B6D4 0%, #0891B2 100%)',
  emerald: 'linear-gradient(135deg, #10B981 0%, #059669 100%)',
  amber: 'linear-gradient(135deg, #F59E0B 0%, #D97706 100%)',
  rose: 'linear-gradient(135deg, #EC4899 0%, #BE185D 100%)',
  purple: 'linear-gradient(135deg, #8B5CF6 0%, #6D28D9 100%)',
  indigo: 'linear-gradient(135deg, #6366F1 0%, #4338CA 100%)',
  teal: 'linear-gradient(135deg, #14B8A6 0%, #0D9488 100%)'
};

const props = withDefaults(
  defineProps<{
    icon?: Component;
    name?: string;
    theme?: IconTheme;
    customBg?: string;
    size?: 'sm' | 'md' | 'lg' | 'xl';
    rounded?: 'md' | 'lg' | 'xl';
  }>(),
  {
    theme: 'blue',
    size: 'md',
    rounded: 'lg'
  }
);

const iconRegistry = ElementPlusIcons as Record<string, Component>;

function resolveIconByName(name: string): Component {
  return iconRegistry[name] ?? MagicStick;
}

const resolvedIcon = computed<Component>(() => {
  if (props.icon) return props.icon;
  if (props.name) return resolveIconByName(props.name);
  return MagicStick;
});

const backgroundStyle = computed(() => {
  if (props.customBg) return props.customBg;
  return THEME_GRADIENTS[props.theme];
});
</script>

<style scoped lang="scss">
.color-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #ffffff;
  box-shadow: 0 6px 16px rgba(30, 80, 150, 0.12);

  &--rounded-md {
    border-radius: 10px;
  }

  &--rounded-lg {
    border-radius: 12px;
  }

  &--rounded-xl {
    border-radius: 16px;
  }

  &--sm {
    width: 36px;
    height: 36px;

    .color-icon__glyph {
      font-size: 18px;
    }
  }

  &--md {
    width: 44px;
    height: 44px;

    .color-icon__glyph {
      font-size: 22px;
    }
  }

  &--lg {
    width: 52px;
    height: 52px;

    .color-icon__glyph {
      font-size: 24px;
    }
  }

  &--xl {
    width: 60px;
    height: 60px;

    .color-icon__glyph {
      font-size: 28px;
    }
  }
}
</style>
