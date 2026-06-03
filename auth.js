const API_BASE_URL = window.location.hostname.endsWith('vercel.app')
    ? 'https://diplomna-kpfv.onrender.com'
    : '';

function apiUrl(path) {
    return `${API_BASE_URL}${path}`;
}

class AuthManager {
    constructor() {
        this.token = localStorage.getItem('token');
        const userStr = localStorage.getItem('user');
        this.user = userStr ? JSON.parse(userStr) : null;
        this._toastTimer = null;
        this.applySavedTheme();
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                this.updateUI();
                this.refreshMe().finally(() => this.updateUI());
                this.ensureThemeToggle();
            });
        } else {
            this.updateUI();
            setTimeout(() => this.refreshMe().finally(() => this.updateUI()), 0);
            setTimeout(() => this.ensureThemeToggle(), 0);
        }
    }

    async refreshMe() {
        if (!this.isAuthenticated()) return;
        try {
            const res = await fetch(apiUrl('/api/users/me'), { headers: { ...this.getAuthHeader() } });
            if (!res.ok) {
                if (res.status === 401 || res.status === 403) {
                    this.token = null;
                    this.user = null;
                    localStorage.removeItem('token');
                    localStorage.removeItem('user');
                }
                return;
            }
            const me = await res.json();
            if (!me || !me.email) return;
            const nextUser = {
                ...(this.user || {}),
                email: me.email,
                role: me.role || (this.user ? this.user.role : undefined),
                firstName: (typeof me.firstName === 'string' ? me.firstName : (this.user ? (this.user.firstName || '') : '')).trim(),
                lastName: (typeof me.lastName === 'string' ? me.lastName : (this.user ? (this.user.lastName || '') : '')).trim(),
                phone: (typeof me.phone === 'string' ? me.phone : (this.user ? (this.user.phone || '') : '')).trim(),
                bonusBalance: typeof me.bonusBalance === 'number' ? me.bonusBalance : (this.user ? (this.user.bonusBalance || 0) : 0),
                bonusRate: (me.bonusRate === null || typeof me.bonusRate === 'undefined') ? (this.user ? (this.user.bonusRate ?? null) : null) : me.bonusRate,
                spinCredits: typeof me.spinCredits === 'number' ? me.spinCredits : (this.user ? (this.user.spinCredits || 0) : 0)
            };
            this.user = nextUser;
            localStorage.setItem('user', JSON.stringify(nextUser));
            this.renderProfileBanner();
        } catch (e) {
        }
    }

    hasValidSession() {
        return !!(this.token && this.user && this.user.email);
    }

    getCurrentPageName() {
        const pathname = (window.location && window.location.pathname) ? window.location.pathname : '';
        if (!pathname || pathname === '/' || pathname.endsWith('/')) {
            return 'index.html';
        }
        const parts = pathname.split('/');
        return parts[parts.length - 1] || 'index.html';
    }

    isProfileComplete() {
        return !!(this.hasValidSession() && String(this.user.firstName || '').trim() && String(this.user.lastName || '').trim());
    }

    ensureProfileComplete() {
        if (!this.hasValidSession()) return false;
        if (this.isProfileComplete()) return true;
        const current = this.getCurrentPageName();
        if (current !== 'complete-profile.html') {
            window.location.href = 'complete-profile.html';
        }
        return false;
    }

    renderProfileBanner() {
        const current = this.getCurrentPageName();
        const shouldShow = this.hasValidSession() && !this.isProfileComplete() && current !== 'index.html' && current !== 'complete-profile.html';
        let banner = document.getElementById('profileCompleteBanner');
        if (!shouldShow) {
            if (banner) banner.remove();
            return;
        }
        if (!banner) {
            banner = document.createElement('div');
            banner.id = 'profileCompleteBanner';
            banner.style.cssText = 'position:sticky;top:0;z-index:999;background:rgba(250,204,21,0.22);border-bottom:1px solid rgba(250,204,21,0.55);backdrop-filter:blur(10px);padding:0.75rem 1rem;';
            banner.innerHTML = `
                <div style="max-width:1400px;margin:0 auto;display:flex;align-items:center;justify-content:space-between;gap:1rem;flex-wrap:wrap;">
                    <div style="font-weight:800;color:var(--text-dark);">Завершіть реєстрацію: додайте ім'я та прізвище, щоб продовжити.</div>
                    <a class="btn" href="complete-profile.html" style="text-decoration:none;padding:0.6rem 1rem;">Завершити реєстрацію</a>
                </div>
            `;
            document.body.prepend(banner);
        }
    }

    async login(email, password) {
        try {
            const formData = `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`;
            const response = await fetch(apiUrl('/api/users/login'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            });

            if (response.ok) {
                const data = await response.json();
                this.token = data.token;
                this.user = {
                    email: data.email,
                    role: data.role,
                    firstName: (data.firstName || '').trim(),
                    lastName: (data.lastName || '').trim(),
                    phone: (data.phone || '').trim(),
                    bonusBalance: typeof data.bonusBalance === 'number' ? data.bonusBalance : 0,
                    bonusRate: (typeof data.bonusRate === 'number') ? data.bonusRate : null,
                    spinCredits: typeof data.spinCredits === 'number' ? data.spinCredits : 0
                };
                localStorage.setItem('token', this.token);
                localStorage.setItem('user', JSON.stringify(this.user));
                await this.refreshMe();
                this.updateUI();
                return { success: true };
            } else {
                const raw = await response.text();
                let message = 'Помилка авторизації';
                try {
                    const error = JSON.parse(raw);
                    message = error.message || message;
                } catch (e) {
                    if (raw && raw.trim()) message = raw;
                }
                return { success: false, message };
            }
        } catch (error) {
            return { success: false, message: `Помилка з'єднання з сервером: ${error?.message || 'невідома помилка'}` };
        }
    }

    async register(email, password, role = 'USER') {
        try {
            const firstName = (document.getElementById('registerFirstName')?.value || '').trim();
            const lastName = (document.getElementById('registerLastName')?.value || '').trim();
            const formData = `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}&role=${encodeURIComponent(role)}&firstName=${encodeURIComponent(firstName)}&lastName=${encodeURIComponent(lastName)}`;
            const response = await fetch(apiUrl('/api/users'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: formData
            });

            if (response.ok || response.status === 201) {
                const data = await response.json();
                return await this.login(email, password);
            } else {
                const raw = await response.text();
                let message = 'Помилка реєстрації';
                try {
                    const error = JSON.parse(raw);
                    message = error.message || message;
                } catch (e) {
                    if (raw && raw.trim()) message = raw;
                }
                if (message.includes('Email already exists')) {
                    message = 'Цей email вже зареєстровано. Спробуйте інший email або увійдіть в систему.';
                }
                return { success: false, message: message };
            }
        } catch (error) {
            return { success: false, message: `Помилка з'єднання з сервером: ${error?.message || 'невідома помилка'}` };
        }
    }

    logout() {
        this.token = null;
        this.user = null;
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        this.updateUI();
        const menu = document.getElementById('userMenu');
        if (menu) {
            menu.style.opacity = '0';
            menu.style.visibility = 'hidden';
            menu.style.transform = 'translateY(-10px)';
        }
        window.location.href = 'index.html';
    }

    isAuthenticated() {
        return this.token !== null && this.token !== '' && this.token !== 'null';
    }

    isAdmin() {
        return this.user && this.user.role === 'ADMIN';
    }

    getAuthHeader() {
        return this.token ? { 'Authorization': `Bearer ${this.token}` } : {};
    }

    updateUI() {
        const loginBtn = document.getElementById('loginBtn');
        const registerBtn = document.getElementById('registerBtn');
        const logoutBtn = document.getElementById('logoutBtn');
        const userInfo = document.getElementById('userInfo');
        const addProductBtn = document.getElementById('addProductBtn');
        const favoritesBtn = document.getElementById('favoritesBtn');
        const cartBtn = document.getElementById('cartBtn');
        const ordersBtn = document.getElementById('ordersBtn');
        const usersBtn = document.getElementById('usersBtn');
        const bonusesBtn = document.getElementById('bonusesBtn');

        if (this.isAuthenticated()) {
            if (loginBtn) loginBtn.style.display = 'none';
            if (registerBtn) registerBtn.style.display = 'none';
            if (logoutBtn) logoutBtn.style.display = 'none';
            
            if (userInfo) {
                const isAdmin = this.isAdmin();
                const avatarColor = isAdmin ? '#ef4444' : '#6366f1';
                const avatarIcon = isAdmin ? '🛡️' : '👤';
                const profileTitle = isAdmin ? 'Адміністратор' : 'Профіль';
                const profileSubtitle = isAdmin ? 'Керування магазином' : 'Особистий кабінет';
                const fullName = `${String(this.user.firstName || '').trim()} ${String(this.user.lastName || '').trim()}`.trim();
                const phone = String(this.user.phone || '').trim();
                const emailMasked = this.maskEmail(this.user.email || '');
                
                userInfo.innerHTML = `
                    <div class="user-avatar-container">
                        <button type="button" class="sidebar-user-trigger" aria-label="Профіль">
                            <span class="user-avatar user-avatar--emoji" style="background: ${avatarColor};">
                                ${avatarIcon}
                            </span>
                            <span class="sidebar-user-meta">
                                <strong>${profileTitle}</strong>
                                <small>${profileSubtitle}</small>
                            </span>
                        </button>
                        <div class="user-menu" id="userMenu">
                            <div style="padding: 0.85rem 1rem; border-bottom: 1px solid var(--border-color);">
                                <div style="font-weight:900; color: var(--text-dark);">${fullName || 'Без імені'}</div>
                                <div style="margin-top:0.2rem; color: var(--text-light); font-size:0.9rem;">${this.escapeHtml(emailMasked)}</div>
                                <div style="margin-top:0.35rem; color: var(--text-light); font-size:0.9rem;">
                                    Телефон: <span style="color: var(--text-dark); font-weight:800;">${this.escapeHtml(phone || 'не вказано')}</span>
                                </div>
                            </div>
                            <div style="display:flex; gap:0.5rem; padding: 0.75rem 1rem;">
                                <button type="button" class="btn" style="flex:1; padding:0.6rem 0.8rem;" onclick="authManager.openProfileModal()">Профіль</button>
                                <button type="button" class="nav-btn" style="flex:1; padding:0.6rem 0.8rem;" onclick="authManager.logout()">Вийти</button>
                            </div>
                        </div>
                    </div>
                `;
                userInfo.style.display = 'inline-block';
            }
            
            if (addProductBtn) {
                addProductBtn.style.display = this.isAdmin() ? 'inline-block' : 'none';
            }
            
            const discountsBtn = document.getElementById('discountsBtn');
            const manageDiscountsBtn = document.getElementById('manageDiscountsBtn');
            const reviewsBtn = document.getElementById('reviewsBtn');
            
            if (this.isAdmin()) {
                if (favoritesBtn) favoritesBtn.style.display = 'none';
                if (cartBtn) cartBtn.style.display = 'none';
                if (discountsBtn) discountsBtn.style.display = 'none';
                if (reviewsBtn) reviewsBtn.style.display = 'inline-block';
                if (ordersBtn) ordersBtn.style.display = 'inline-block';
                if (manageDiscountsBtn) manageDiscountsBtn.style.display = 'inline-block';
                if (usersBtn) usersBtn.style.display = 'inline-block';
                if (bonusesBtn) bonusesBtn.style.display = 'none';
            } else {
                if (favoritesBtn) favoritesBtn.style.display = 'inline-block';
                if (cartBtn) cartBtn.style.display = 'inline-block';
                if (discountsBtn) discountsBtn.style.display = 'inline-block';
                if (reviewsBtn) reviewsBtn.style.display = 'inline-block';
                if (ordersBtn) ordersBtn.style.display = 'inline-block';
                if (manageDiscountsBtn) manageDiscountsBtn.style.display = 'none';
                if (usersBtn) usersBtn.style.display = 'none';
                if (bonusesBtn) bonusesBtn.style.display = 'inline-block';
            }
            
            if (typeof cartManager !== 'undefined' && !this.isAdmin()) {
                cartManager.updateCartBadge();
                cartManager.updateFavoritesBadge();
            }
            this.renderProfileBanner();
            this.maybeShowWelcomeToast();
        } else {
            if (loginBtn) loginBtn.style.display = 'inline-block';
            if (registerBtn) registerBtn.style.display = 'inline-block';
            if (logoutBtn) logoutBtn.style.display = 'none';
            if (userInfo) userInfo.style.display = 'none';
            if (addProductBtn) addProductBtn.style.display = 'none';
            if (favoritesBtn) favoritesBtn.style.display = 'none';
            if (cartBtn) cartBtn.style.display = 'none';
            if (ordersBtn) ordersBtn.style.display = 'none';
            if (usersBtn) usersBtn.style.display = 'none';
            if (bonusesBtn) bonusesBtn.style.display = 'none';
            this.renderProfileBanner();
        }
    }

    getWelcomeTextForPage(page) {
        const map = {
            'products.html': {
                title: 'Вітаю у каталозі!',
                text: 'Тут можна сортувати товари, відкривати деталі та додавати в кошик або обране.'
            },
            'cart.html': {
                title: 'Це ваш кошик',
                text: 'Перевіряйте товари та оформлюйте замовлення. Під час оформлення можна обрати доставку й оплату.'
            },
            'favorites.html': {
                title: 'Обране',
                text: 'Зберігайте товари, щоб швидко повернутися до них пізніше.'
            },
            'discounts.html': {
                title: 'Знижки',
                text: 'Тут показані товари зі знижками — інколи найвигідніші пропозиції саме тут.'
            },
            'reviews.html': {
                title: 'Відгуки',
                text: 'Поділись враженнями про покупку та допоможи іншим з вибором.'
            },
            'orders.html': {
                title: 'Замовлення',
                text: 'Тут зберігаються ваші замовлення. Можна переглянути деталі та написати в чат по замовленню.'
            },
            'bonuses.html': {
                title: 'Бонуси',
                text: 'Крути колесо (2–5%) і накопичуй бонуси з покупок. У кошику зможеш списати їх як додаткову знижку.'
            },
            'users.html': {
                title: 'Користувачі',
                text: 'Цей розділ доступний адміну: тут можна змінювати ролі користувачів.'
            },
            'manage-discounts.html': {
                title: 'Знижки (адмін)',
                text: 'Адмін-розділ: встановлюй/знімай знижки для товарів.'
            }
        };
        return map[page] || null;
    }

    maybeShowWelcomeToast() {
        if (!this.hasValidSession()) return;
        const page = this.getCurrentPageName();
        if (page === 'index.html' || page === 'complete-profile.html') return;
        const msg = this.getWelcomeTextForPage(page);
        if (!msg) return;
        const key = `welcomeSeen:${String(this.user.email || '').toLowerCase()}:${page}`;
        if (localStorage.getItem(key) === '1') return;
        localStorage.setItem(key, '1');
        this.showToast(msg.title, msg.text);
    }

    showToast(title, text) {
        try {
            if (this._toastTimer) {
                clearTimeout(this._toastTimer);
                this._toastTimer = null;
            }
            const existing = document.getElementById('welcomeToast');
            if (existing) existing.remove();

            const toast = document.createElement('div');
            toast.id = 'welcomeToast';
            toast.className = 'toast';
            toast.innerHTML = `
                <div class="toast__title">${title}</div>
                <div class="toast__text">${text}</div>
                <div class="toast__hint">Натисни, щоб закрити (або зачекай 8 секунд)</div>
            `;
            toast.addEventListener('click', () => toast.remove());
            document.body.appendChild(toast);
            this._toastTimer = setTimeout(() => {
                const el = document.getElementById('welcomeToast');
                if (el) el.remove();
            }, 8000);
        } catch (e) {
        }
    }

    applySavedTheme() {
        try {
            const mode = localStorage.getItem('themeMode') || 'light';
            if (mode === 'dark') document.body.classList.add('theme-dark');
            else document.body.classList.remove('theme-dark');
        } catch (e) {
        }
    }

    ensureThemeToggle() {
        try {
            if (document.getElementById('themeToggleBtn')) return;
            const btn = document.createElement('button');
            btn.id = 'themeToggleBtn';
            btn.type = 'button';
            btn.className = 'theme-toggle';
            const icon = document.createElement('span');
            icon.className = 'theme-toggle__icon';
            const renderIcon = () => {
                const isDark = document.body.classList.contains('theme-dark');
                icon.textContent = isDark ? '☀' : '🌙';
                btn.title = isDark ? 'Світла тема' : 'Темна тема';
            };
            renderIcon();
            btn.appendChild(icon);
            btn.addEventListener('click', () => {
                const isDark = document.body.classList.toggle('theme-dark');
                localStorage.setItem('themeMode', isDark ? 'dark' : 'light');
                renderIcon();
            });
            document.body.appendChild(btn);
        } catch (e) {
        }
    }

    escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#039;');
    }

    maskEmail(email) {
        const e = String(email || '').trim();
        const at = e.indexOf('@');
        if (at <= 1) return '***';
        const name = e.slice(0, at);
        const domain = e.slice(at);
        const visible = name.slice(0, 2);
        return `${visible}***${domain}`;
    }

    ensureProfileModal() {
        if (document.getElementById('profileModal')) return;
        const modal = document.createElement('div');
        modal.id = 'profileModal';
        modal.className = 'modal';
        modal.style.display = 'none';
        modal.innerHTML = `
            <div class="modal-content" style="max-width: 720px;">
                <span class="close" onclick="authManager.closeProfileModal()">&times;</span>
                <h2>Мій профіль</h2>
                <div style="display:flex; gap:0.5rem; margin: 0.5rem 0 1rem; flex-wrap:wrap;">
                    <button type="button" class="btn" id="profileTabDataBtn" style="padding:0.55rem 0.9rem;">Дані</button>
                    <button type="button" class="nav-btn" id="profileTabSecurityBtn" style="padding:0.55rem 0.9rem;">Безпека</button>
                </div>

                <div id="profileTabData">
                    <form id="profileDataForm">
                        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:0.75rem;">
                            <div class="form-group">
                                <label for="profileFirstName">Ім'я</label>
                                <input id="profileFirstName" type="text" required>
                            </div>
                            <div class="form-group">
                                <label for="profileLastName">Прізвище</label>
                                <input id="profileLastName" type="text" required>
                            </div>
                        </div>
                        <div style="display:grid; grid-template-columns: 1fr 1fr; gap:0.75rem;">
                            <div class="form-group">
                                <label for="profilePhone">Телефон (необов'язково)</label>
                                <input id="profilePhone" type="text" placeholder="+380XXXXXXXXX">
                                <div style="margin-top:0.35rem; color: var(--text-light); font-size:0.85rem;">Формат: +380 і 9 цифр.</div>
                            </div>
                            <div class="form-group">
                                <label>Email</label>
                                <input id="profileEmailReadonly" type="text" disabled>
                                <div style="margin-top:0.35rem; color: var(--text-light); font-size:0.85rem;">Змінюється у вкладці “Безпека”.</div>
                            </div>
                        </div>
                        <div class="modal-buttons">
                            <button type="button" class="btn btn-danger" onclick="authManager.closeProfileModal()">Закрити</button>
                            <button type="submit" class="btn" id="saveProfileBtn">Зберегти дані</button>
                        </div>
                    </form>
                </div>

                <div id="profileTabSecurity" style="display:none;">
                    <div style="background: var(--bg-light); border:1px solid var(--border-color); border-radius: 14px; padding: 0.9rem; margin-bottom: 0.9rem;">
                        <div style="font-weight:900; margin-bottom:0.5rem;">Зміна email</div>
                        <form id="profileEmailForm">
                            <div class="form-group">
                                <label for="profileNewEmail">Новий email</label>
                                <input id="profileNewEmail" type="email" placeholder="new@gmail.com" required>
                            </div>
                            <div class="form-group">
                                <label for="profileEmailPassword">Поточний пароль</label>
                                <input id="profileEmailPassword" type="password" required>
                            </div>
                            <button type="submit" class="btn" id="saveEmailBtn">Змінити email</button>
                        </form>
                    </div>

                    <div style="background: var(--bg-light); border:1px solid var(--border-color); border-radius: 14px; padding: 0.9rem;">
                        <div style="font-weight:900; margin-bottom:0.5rem;">Зміна пароля</div>
                        <form id="profilePasswordForm">
                            <div class="form-group">
                                <label for="profileCurrentPassword">Поточний пароль</label>
                                <input id="profileCurrentPassword" type="password" required>
                            </div>
                            <div class="form-group">
                                <label for="profileNewPassword">Новий пароль</label>
                                <input id="profileNewPassword" type="password" placeholder="мінімум 6 символів" required>
                            </div>
                            <div style="display:flex; gap:0.5rem; flex-wrap:wrap;">
                                <button type="submit" class="btn" id="savePasswordBtn">Змінити пароль</button>
                                <button type="button" class="nav-btn" onclick="authManager.openForgotPasswordFromProfile()">Забув пароль</button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        `;
        document.body.appendChild(modal);
        modal.addEventListener('click', (e) => {
            if (e.target === modal) this.closeProfileModal();
        });

        modal.querySelector('#profileTabDataBtn')?.addEventListener('click', () => this.setProfileTab('data'));
        modal.querySelector('#profileTabSecurityBtn')?.addEventListener('click', () => this.setProfileTab('security'));
        modal.querySelector('#profileDataForm')?.addEventListener('submit', async (e) => { e.preventDefault(); await this.saveProfileFromModal(); });
        modal.querySelector('#profilePasswordForm')?.addEventListener('submit', async (e) => { e.preventDefault(); await this.changePasswordFromModal(); });
        modal.querySelector('#profileEmailForm')?.addEventListener('submit', async (e) => { e.preventDefault(); await this.changeEmailFromModal(); });
    }

    setProfileTab(tab) {
        const data = document.getElementById('profileTabData');
        const sec = document.getElementById('profileTabSecurity');
        const b1 = document.getElementById('profileTabDataBtn');
        const b2 = document.getElementById('profileTabSecurityBtn');
        if (!data || !sec || !b1 || !b2) return;
        const isData = tab === 'data';
        data.style.display = isData ? 'block' : 'none';
        sec.style.display = isData ? 'none' : 'block';
        b1.className = isData ? 'btn' : 'nav-btn';
        b2.className = isData ? 'nav-btn' : 'btn';
    }

    openProfileModal() {
        if (!this.hasValidSession()) return;
        this.ensureProfileModal();
        const modal = document.getElementById('profileModal');
        if (!modal) return;
        const fn = document.getElementById('profileFirstName');
        const ln = document.getElementById('profileLastName');
        const ph = document.getElementById('profilePhone');
        const em = document.getElementById('profileEmailReadonly');
        const ne = document.getElementById('profileNewEmail');
        if (fn) fn.value = String(this.user.firstName || '');
        if (ln) ln.value = String(this.user.lastName || '');
        if (ph) ph.value = String(this.user.phone || '');
        if (em) em.value = String(this.user.email || '');
        if (ne) ne.value = '';
        this.setProfileTab('data');
        modal.style.display = 'block';
    }

    closeProfileModal() {
        const modal = document.getElementById('profileModal');
        if (modal) modal.style.display = 'none';
    }

    async saveProfileFromModal() {
        if (!this.hasValidSession()) return;
        const fn = String(document.getElementById('profileFirstName')?.value || '').trim();
        const ln = String(document.getElementById('profileLastName')?.value || '').trim();
        const phRaw = String(document.getElementById('profilePhone')?.value || '').trim();
        const ph = phRaw.replaceAll(' ', '');
        if (!fn || !ln) {
            alert("Вкажіть ім'я та прізвище");
            return;
        }
        if (ph && !/^\+380\d{9}$/.test(ph)) {
            alert('Номер телефону має бути у форматі +380XXXXXXXXX');
            return;
        }
        const btn = document.getElementById('saveProfileBtn');
        if (btn) btn.disabled = true;
        try {
            const body = `firstName=${encodeURIComponent(fn)}&lastName=${encodeURIComponent(ln)}&phone=${encodeURIComponent(ph)}`;
            const res = await fetch(apiUrl('/api/users/me'), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', ...this.getAuthHeader() },
                body
            });
            if (!res.ok) {
                const text = await res.text();
                alert(text || 'Не вдалося зберегти профіль');
                return;
            }
            const me = await res.json();
            this.user = {
                ...(this.user || {}),
                firstName: (me.firstName || fn).trim(),
                lastName: (me.lastName || ln).trim(),
                phone: (me.phone || ph).trim(),
            };
            localStorage.setItem('user', JSON.stringify(this.user));
            this.closeProfileModal();
            await this.refreshMe();
            this.updateUI();
        } catch (e) {
            alert('Помилка з’єднання з сервером');
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    async changePasswordFromModal() {
        if (!this.hasValidSession()) return;
        const currentPassword = String(document.getElementById('profileCurrentPassword')?.value || '');
        const newPassword = String(document.getElementById('profileNewPassword')?.value || '');
        if (!currentPassword || !newPassword) return;
        if (newPassword.trim().length < 6) {
            alert('Новий пароль має бути мінімум 6 символів');
            return;
        }
        const btn = document.getElementById('savePasswordBtn');
        if (btn) btn.disabled = true;
        try {
            const body = `currentPassword=${encodeURIComponent(currentPassword)}&newPassword=${encodeURIComponent(newPassword)}`;
            const res = await fetch(apiUrl('/api/users/me/password'), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', ...this.getAuthHeader() },
                body
            });
            if (!res.ok) {
                const text = await res.text();
                alert(text || 'Не вдалося змінити пароль');
                return;
            }
            alert('Пароль змінено');
            const c = document.getElementById('profileCurrentPassword'); if (c) c.value = '';
            const n = document.getElementById('profileNewPassword'); if (n) n.value = '';
        } catch (e) {
            alert('Помилка з’єднання з сервером');
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    async changeEmailFromModal() {
        if (!this.hasValidSession()) return;
        const email = String(document.getElementById('profileNewEmail')?.value || '').trim();
        const password = String(document.getElementById('profileEmailPassword')?.value || '');
        if (!email || !password) return;
        const btn = document.getElementById('saveEmailBtn');
        if (btn) btn.disabled = true;
        try {
            const body = `email=${encodeURIComponent(email)}&password=${encodeURIComponent(password)}`;
            const res = await fetch(apiUrl('/api/users/me/email'), {
                method: 'PUT',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded', ...this.getAuthHeader() },
                body
            });
            const raw = await res.text();
            if (!res.ok) {
                alert(raw || 'Не вдалося змінити email');
                return;
            }
            const data = JSON.parse(raw);
            if (data?.token) {
                this.token = data.token;
                localStorage.setItem('token', this.token);
            }
            this.user = {
                ...(this.user || {}),
                email: data.email || email,
                role: data.role || this.user.role,
                firstName: (data.firstName || this.user.firstName || '').trim(),
                lastName: (data.lastName || this.user.lastName || '').trim(),
                phone: (data.phone || this.user.phone || '').trim(),
                bonusBalance: typeof data.bonusBalance === 'number' ? data.bonusBalance : (this.user.bonusBalance || 0),
                bonusRate: (typeof data.bonusRate === 'number') ? data.bonusRate : (this.user.bonusRate ?? null),
                spinCredits: typeof data.spinCredits === 'number' ? data.spinCredits : (this.user.spinCredits || 0)
            };
            localStorage.setItem('user', JSON.stringify(this.user));
            alert('Email змінено');
            await this.refreshMe();
            this.updateUI();
            const em = document.getElementById('profileEmailReadonly'); if (em) em.value = this.user.email || '';
            const ne = document.getElementById('profileNewEmail'); if (ne) ne.value = '';
            const pw = document.getElementById('profileEmailPassword'); if (pw) pw.value = '';
        } catch (e) {
            alert('Помилка з’єднання з сервером');
        } finally {
            if (btn) btn.disabled = false;
        }
    }

    openForgotPasswordFromProfile() {
        alert('Відновлення пароля доступне на головній сторінці: натисни “Забув пароль” у вікні входу.');
    }
}

const authManager = new AuthManager();

