// import './assets/main.css'
import "bootstrap/dist/css/bootstrap.min.css";
import "./assets/css/all.css";
import "bootstrap/dist/js/bootstrap.bundle.js";
import { createApp } from "vue";
import { createPinia } from "pinia";
import piniaPluginPersistedstate from "pinia-plugin-persistedstate";
import App from "./App.vue";
import router from "./router";
import * as bootstrap from "bootstrap";
window.bootstrap = bootstrap;

// === Font Awesome 核心引入 ===
import { library } from "@fortawesome/fontawesome-svg-core";
import { FontAwesomeIcon } from "@fortawesome/vue-fontawesome";
import { fas } from "@fortawesome/free-solid-svg-icons";
// 將所有 Solid 圖示加入庫中，方便大家直接用
library.add(fas);

const app = createApp(App);
// === 註冊全域組件 ===
app.component("font-awesome-icon", FontAwesomeIcon);

import vue3GoogleLogin from 'vue3-google-login';

const pinia = createPinia();
pinia.use(piniaPluginPersistedstate);

app.use(pinia);
app.use(router);

// 註冊 Google Login 套件
app.use(vue3GoogleLogin, {
  clientId: '821295713640-uk2d070093p8htt12dqe2mfpc6v7cr1e.apps.googleusercontent.com'
});

app.mount("#app");
