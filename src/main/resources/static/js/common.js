/* 公共 UI 组件：导航、提示、格式化、分页 */
(function (global) {
  const NAV = [
    { href: 'index.html', text: '首页' },
    { href: 'products.html', text: '全部商品' },
    { href: 'publish.html', text: '发布闲置' },
    { href: 'user.html', text: '个人中心' }
  ];

  function escapeHtml(text) {
    if (text === undefined || text === null) return '';
    return String(text)
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
  }

  function money(value) {
    const num = Number(value || 0);
    return num % 1 === 0 ? String(num) : num.toFixed(2);
  }

  function timeText(value) {
    if (!value) return '';
    const date = new Date(String(value).replace(/-/g, '/'));
    if (isNaN(date.getTime())) return value;
    const diff = (Date.now() - date.getTime()) / 1000;
    if (diff < 60) return '刚刚';
    if (diff < 3600) return Math.floor(diff / 60) + ' 分钟前';
    if (diff < 86400) return Math.floor(diff / 3600) + ' 小时前';
    if (diff < 86400 * 7) return Math.floor(diff / 86400) + ' 天前';
    return value.slice(0, 10);
  }

  const STATUS_CHIP = {
    0: ['待审核', 'chip-orange'],
    1: ['在售', 'chip-green'],
    2: ['已下架', 'chip-gray'],
    3: ['已售出', 'chip-blue'],
    4: ['审核驳回', 'chip-red'],
    5: ['违规下架', 'chip-red']
  };

  function statusChip(status) {
    const item = STATUS_CHIP[status] || ['未知', 'chip-gray'];
    return '<span class="chip ' + item[1] + '">' + item[0] + '</span>';
  }

  function toast(message, isError) {
    let wrap = document.querySelector('.toast-wrap');
    if (!wrap) {
      wrap = document.createElement('div');
      wrap.className = 'toast-wrap';
      document.body.appendChild(wrap);
    }
    const el = document.createElement('div');
    el.className = 'toast' + (isError ? ' err' : '');
    el.textContent = message;
    wrap.appendChild(el);
    setTimeout(function () { el.remove(); }, 2600);
  }

  function renderHeader() {
    const host = document.getElementById('app-header');
    if (!host) return;
    const page = location.pathname.split('/').pop() || 'index.html';
    const navHtml = NAV.map(function (item) {
      return '<a href="' + item.href + '" class="' + (item.href === page ? 'active' : '') + '">' + item.text + '</a>';
    }).join('');

    let actions;
    if (API.isLogin()) {
      const user = API.getUser() || {};
      actions =
        '<a class="btn btn-ghost btn-sm" href="user.html#messages">消息<span id="unread-badge"></span></a>' +
        (API.isAdmin() ? '<a class="btn btn-soft btn-sm" href="admin.html">管理后台</a>' : '') +
        '<a href="user.html" class="header-user" title="个人中心">' +
          '<img class="avatar avatar-sm" src="' + escapeHtml(user.avatar || '/images/avatar/default.png') + '" alt="头像">' +
          '<span style="font-weight:600">' + escapeHtml(user.nickname || user.name || user.account) + '</span>' +
        '</a>' +
        '<button class="btn btn-ghost btn-sm" id="btn-logout">退出</button>';
    } else {
      actions = '<a class="btn btn-ghost btn-sm" href="login.html">登录</a>' +
                '<a class="btn btn-sm" href="login.html?tab=register">注册</a>';
    }

    host.innerHTML =
      '<header class="header"><div class="header-inner">' +
        '<a class="logo" href="index.html"><span class="logo-mark">♻</span><span>校园二手集市</span></a>' +
        '<nav class="nav">' + navHtml + '</nav>' +
        '<div class="header-actions">' + actions + '</div>' +
      '</div></header>';

    const logoutBtn = document.getElementById('btn-logout');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', async function () {
        try { await API.post('/api/auth/logout'); } catch (e) { /* 忽略 */ }
        API.clearAuth();
        toast('已退出登录');
        setTimeout(function () { location.href = 'index.html'; }, 500);
      });
    }
    refreshUnread();
  }

  async function refreshUnread() {
    if (!API.isLogin()) return;
    try {
      const count = await API.get('/api/messages/unread-count');
      const badge = document.getElementById('unread-badge');
      if (badge && count > 0) {
        badge.className = 'chip chip-red';
        badge.style.marginLeft = '6px';
        badge.textContent = count > 99 ? '99+' : count;
      }
    } catch (e) { /* 忽略 */ }
  }

  function productCard(product) {
    const cover = (product.images && product.images[0]) || product.cover || '/images/products/p1.jpg';
    return '' +
      '<a class="product-card" href="product.html?id=' + product.id + '">' +
        '<img class="product-thumb" src="' + escapeHtml(cover) + '" alt="' + escapeHtml(product.title) + '" loading="lazy">' +
        '<div class="product-body">' +
          '<div class="product-name">' + escapeHtml(product.title) + '</div>' +
          '<div class="product-meta">' +
            '<span class="price">' + money(product.price) + '</span>' +
            (product.originalPrice ? '<span class="original">¥' + money(product.originalPrice) + '</span>' : '') +
            (product.negotiable === 1 ? '<span class="chip chip-orange">可议价</span>' : '') +
          '</div>' +
          '<div class="product-foot">' +
            '<span>' + escapeHtml(product.sellerName || '同学') + '</span>' +
            '<span>👁 ' + (product.viewCount || 0) + '</span>' +
          '</div>' +
        '</div>' +
      '</a>';
  }

  function renderPagination(host, page, pages, onChange) {
    if (!host) return;
    if (!pages || pages <= 1) { host.innerHTML = ''; return; }
    const items = [];
    items.push('<button data-page="' + (page - 1) + '" ' + (page <= 1 ? 'disabled' : '') + '>上一页</button>');
    const start = Math.max(1, page - 2);
    const end = Math.min(pages, start + 4);
    for (let i = start; i <= end; i++) {
      items.push('<button data-page="' + i + '" class="' + (i === page ? 'on' : '') + '">' + i + '</button>');
    }
    items.push('<button data-page="' + (page + 1) + '" ' + (page >= pages ? 'disabled' : '') + '>下一页</button>');
    host.innerHTML = items.join('');
    host.querySelectorAll('button').forEach(function (btn) {
      btn.addEventListener('click', function () {
        const target = Number(btn.dataset.page);
        if (target >= 1 && target <= pages && target !== page) onChange(target);
      });
    });
  }

  function empty(text) {
    return '<div class="empty"><div class="icon">🍃</div><div>' + escapeHtml(text || '这里还什么都没有') + '</div></div>';
  }

  function requireLogin() {
    if (!API.isLogin()) {
      const next = encodeURIComponent(location.pathname + location.search);
      location.href = 'login.html?next=' + next;
      return false;
    }
    return true;
  }

  function query(name) {
    return new URLSearchParams(location.search).get(name);
  }

  global.UI = {
    escapeHtml: escapeHtml,
    money: money,
    timeText: timeText,
    statusChip: statusChip,
    toast: toast,
    renderHeader: renderHeader,
    productCard: productCard,
    renderPagination: renderPagination,
    empty: empty,
    requireLogin: requireLogin,
    query: query,
    refreshUnread: refreshUnread
  };
})(window);
