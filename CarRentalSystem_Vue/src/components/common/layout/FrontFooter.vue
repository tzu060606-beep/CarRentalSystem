<script setup>
const footerGroups = [
  {
    title: '車款選擇',
    links: ['經濟型', '中型房車', '休旅型', '豪華型'],
  },
  {
    title: '租車方案',
    links: ['短租方案', '長租方案', '機場接送', '企業租車'],
  },
  {
    title: '會員服務',
    links: ['會員註冊', '會員專屬優惠', '紅利點數', '常見問題'],
  },
  {
    title: '關於 OneRent',
    links: ['品牌故事', '門市據點', '聯絡我們', '加入我們'],
  },
]
</script>

<template>
  <footer class="front-footer">
    <div class="container footer-container">
      <div class="footer-main">
        <div class="footer-brand">
          <div class="footer-logo">
            <span class="footer-mark"></span>
            <span class="footer-brand-name"><strong>One</strong>Rent</span>
          </div>
          <p class="footer-slogan">輕鬆租車・自在出行</p>
          <div class="footer-contact">
            <p><span>客服專線</span>0800-123-456</p>
            <p><span>服務時間</span>每日 08:00 - 22:00</p>
            <p><span>客服信箱</span>service@onerent.tw</p>
          </div>
        </div>

        <div v-for="group in footerGroups" :key="group.title" class="footer-group">
          <h6>{{ group.title }}</h6>
          <ul>
            <li v-for="link in group.links" :key="link">
              <a href="#">{{ link }}</a>
            </li>
          </ul>
        </div>
      </div>

      <div class="footer-bottom">
        <p>© 2026 OneRent Co., Ltd. 統一編號 12345678.</p>
        <div class="footer-legal">
          <a href="#">隱私權政策</a>
          <a href="#">租賃條款</a>
          <a href="#">網站地圖</a>
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.front-footer {
  position: relative;
  border-top: 0;
  background: var(--color-blue-900);
  color: var(--color-text-inverse);
  padding: 4rem 0 2.5rem;
}

.front-footer::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 5px;
  background: linear-gradient(
    90deg,
    var(--color-accent) 0%,
    var(--color-sandy-400) 18%,
    #f6a85a 35%,
    var(--color-primary) 52%,
    var(--color-primary) 100%
  );
}

.footer-container {
  max-width: 1240px;
}

.footer-main {
  display: grid;
  grid-template-columns: 2.2fr repeat(4, 1fr);
  gap: 4rem;
  padding-bottom: 3rem;
}

.footer-logo {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.25rem;
}

.footer-mark {
  position: relative;
  width: 38px;
  height: 38px;
  border: 0;
  border-radius: 50%;
  background: conic-gradient(
    from 196deg,
    var(--color-accent) 0deg,
    var(--color-sandy-400) 34deg,
    var(--color-gray-100) 66deg,
    var(--color-blue-400) 88deg,
    var(--color-primary) 108deg,
    var(--color-primary) 360deg
  );
  display: inline-block;
  flex: 0 0 auto;
}

.footer-mark::after {
  content: '';
  position: absolute;
  inset: 7px;
  border-radius: 50%;
  background: var(--color-blue-900);
}

.footer-brand-name {
  font-size: 1.65rem;
  color: var(--color-white);
}

.footer-slogan {
  color: var(--color-blue-200);
  font-weight: var(--font-semibold);
  margin-bottom: 1.5rem;
}

.footer-contact p {
  margin: 0.15rem 0;
  color: var(--color-blue-300);
  font-size: var(--text-sm);
}

.footer-contact span {
  display: inline-block;
  min-width: 4.4rem;
  color: var(--color-blue-200);
}

.footer-group h6 {
  color: var(--color-white);
  font-size: var(--text-sm);
  font-weight: var(--font-bold);
  margin-bottom: 1.5rem;
}

.footer-group ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 1rem;
}

.footer-group a,
.footer-legal a {
  color: var(--color-blue-300);
  font-size: var(--text-sm);
  font-weight: var(--font-semibold);
  text-decoration: none;
}

.footer-group a:hover,
.footer-legal a:hover {
  color: var(--color-white);
}

.footer-bottom {
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  padding-top: 1.5rem;
}

.footer-bottom p {
  margin: 0;
  color: var(--color-blue-400);
  font-size: var(--text-xs);
  font-weight: var(--font-semibold);
}

.footer-legal {
  display: flex;
  gap: 1.8rem;
}

@media (max-width: 1199.98px) {
  .footer-main {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 767.98px) {
  .footer-main {
    grid-template-columns: 1fr;
    gap: 2rem;
  }

  .footer-bottom {
    flex-direction: column;
  }

  .footer-legal {
    flex-wrap: wrap;
    gap: 1rem;
  }
}
</style>
