<template>
  <div>
    <a-spin :spinning="spinning">
      <a-card
        size="small"
        :body-style="{
          height: `calc(100vh - 45px)`
        }"
      >
        <template #title>
          <template v-if="sshData">
            <a-space>
              <div>
                {{ sshData.name }}
                <template v-if="sshData.host">({{ sshData.host }})</template>
              </div>

              <a-button size="small" type="primary" :disabled="!sshData.fileDirs" @click="handleFile()">文件</a-button>
            </a-space>
          </template>
          <template v-else>loading</template>
        </template>
        <template #extra>
          <a href="#"></a>
        </template>
        <terminal1 v-if="sshData" :ssh-id="sshData.id" />
        <template v-else>
          <a-result status="404" title="不能操作" sub-title="没有对应的SSH">
            <template #extra>
              <router-link :to="{ path: '/ssh', query: {} }">
                <a-button type="primary">返回首页</a-button>
              </router-link>
            </template>
          </a-result>
        </template>
      </a-card>
    </a-spin>
    <!-- 文件管理 -->
    <a-drawer v-if="sshData" destroy-on-close placement="right" width="90vw" :open="drawerVisible" @close="onClose">
      <template #title>
        {{ sshData.name }}<template v-if="sshData.host"> ({{ sshData.host }}) </template>文件管理
      </template>
      <ssh-file v-if="drawerVisible" :ssh-id="sshData.id" />
    </a-drawer>
  </div>
</template>

<script>
import terminal1 from './terminal'
import { getItem } from '@/api/ssh'
import SshFile from '@/pages/ssh/ssh-file'

export default {
  components: {
    terminal1,
    SshFile
  },

  data() {
    return {
      sshId: '',
      sshData: null,
      spinning: true,
      drawerVisible: false
    }
  },
  computed: {},
  mounted() {
    this.sshId = this.$route.query.id
    if (this.sshId) {
      this.loadItemData()
    }
  },
  beforeUnmount() {},
  methods: {
    loadItemData() {
      getItem({
        id: this.sshId
      }).then((res) => {
        this.spinning = false
        if (res.code === 200) {
          this.sshData = res.data
        }
        // console.log(this.sshData);
      })
    },
    handleFile() {
      this.drawerVisible = true
    },
    // 关闭抽屉层
    onClose() {
      this.drawerVisible = false
    }
  }
}
</script>
