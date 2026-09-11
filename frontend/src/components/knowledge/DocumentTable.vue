<template>
  <el-table :data="documents" stripe>
    <el-table-column prop="fileName" label="文件名" />
    <el-table-column prop="fileType" label="类型" width="100" />
    <el-table-column prop="parseStatus" label="解析状态" width="120" />
    <el-table-column label="操作" width="180">
      <template #default="{ row }">
        <el-button link type="primary" @click="$emit('parse', row.id)">解析</el-button>
        <el-button link type="danger" @click="$emit('delete', row.id)">删除</el-button>
      </template>
    </el-table-column>
  </el-table>
</template>

<script setup lang="ts">
import { KBDocument } from '@/types/knowledge/document';

defineProps<{ documents: KBDocument[] }>();
defineEmits<{ parse: [id: number]; delete: [id: number] }>();
</script>
