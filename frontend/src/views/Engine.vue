<template>
  <section class="card p-5 flex flex-col gap-4">
    <CardHeader title="缠论引擎" subtitle="规则参数与级别同步。">
      <template #actions>
        <div class="flex gap-2">
          <button class="button-ghost" @click="resetParams">重置</button>
          <button class="button" :disabled="saving" @click="saveParams">
            {{ saving ? '保存中...' : '保存参数' }}
          </button>
        </div>
      </template>
    </CardHeader>

    <template v-if="loading">
      <Skeleton variant="card" height="200px" />
      <Skeleton variant="card" height="100px" />
    </template>

    <template v-else>
      <CardSection title="核心参数" subtitle="调整缠论计算规则" layout="grid-3" gap="md">
        <div v-for="param in engineParams" :key="param.label" class="param p-4">
          <div class="flex items-center justify-between mb-2">
            <label>{{ param.label }}</label>
            <span class="mono text-sm text-accent">{{ param.value }}</span>
          </div>
          <input
            type="range"
            :min="param.min"
            :max="param.max"
            v-model.number="param.value"
            class="w-full"
          />
          <div class="flex justify-between text-xs text-muted mt-1">
            <span>{{ param.min }}</span>
            <span>{{ param.max }}</span>
          </div>
        </div>
      </CardSection>

      <CardSection title="说明" bordered>
        <div class="mini-card p-4">
          <div class="flex items-start gap-3">
            <div class="w-8 h-8 rounded-lg bg-[rgba(59,130,246,0.15)] flex items-center justify-center flex-shrink-0">
              <svg class="w-4 h-4 text-accent" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="12" cy="12" r="10" />
                <path d="M12 16v-4M12 8h.01" />
              </svg>
            </div>
            <div>
              <div class="text-sm font-medium mb-1">参数生效规则</div>
              <div class="text-muted text-sm leading-relaxed">
                参数保存后将立即应用于实时计算与历史回放。修改包含关系参数可能影响已有的笔段划分结果。
              </div>
            </div>
          </div>
        </div>
      </CardSection>
    </template>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import CardHeader from '../components/common/CardHeader.vue';
import CardSection from '../components/common/CardSection.vue';
import Skeleton from '../components/common/Skeleton.vue';

const loading = ref(true);
const saving = ref(false);

const defaultParams = [
  { label: '包含关系', value: 1, min: 0, max: 2 },
  { label: '分型最少K数', value: 3, min: 2, max: 6 },
  { label: '线段最少笔数', value: 3, min: 3, max: 6 },
];

const engineParams = ref(defaultParams.map((p) => ({ ...p })));

const saveParams = async () => {
  saving.value = true;
  await new Promise((resolve) => setTimeout(resolve, 1000));
  saving.value = false;
};

const resetParams = () => {
  engineParams.value = defaultParams.map((p) => ({ ...p }));
};

// Simulate data loading
onMounted(async () => {
  await new Promise((resolve) => setTimeout(resolve, 700));
  loading.value = false;
});
</script>

<style scoped>
.text-accent {
  color: var(--accent);
}
</style>
