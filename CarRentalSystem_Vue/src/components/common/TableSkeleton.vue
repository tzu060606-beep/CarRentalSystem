<script setup>
// TableSkeleton.vue - 表格骨架屏（資料載入時的佔位動畫）
// 使用方式：
// <TableSkeleton :rows="5" :cols="4" />
// <TableSkeleton :rows="3" :cols="6" v-if="isLoading" />

const props = defineProps({
  rows: { type: Number, default: 5 },   // 骨架列數
  cols: { type: Number, default: 4 }    // 骨架欄數
})
</script>

<template>
  <div class="bg-white border rounded-4 shadow-sm overflow-hidden">
    <table class="table mb-0">
      <thead>
        <tr>
          <th v-for="col in cols" :key="col">
            <div class="placeholder-glow">
              <span class="placeholder col-8 rounded"></span>
            </div>
          </th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="row in rows" :key="row">
          <td v-for="col in cols" :key="col">
            <div class="placeholder-glow">
              <!-- 每欄寬度隨機變化，讓骨架看起來更自然 -->
              <span :class="`placeholder col-${[6, 7, 8, 9, 10][Math.floor(Math.random() * 5)]} rounded`"></span>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
