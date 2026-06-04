class ReviewManager {
    constructor() {
        this.reviews = [];
        this.products = [];
        this.orderedProductIds = [];
        this.init();
    }

    async init() {
        await Promise.all([
            this.loadProductsForSubject(),
            this.loadReviews()
        ]);
        this.setupEventListeners();
        this.updateAddReviewSection();
    }

    async loadProductsForSubject() {
        try {
            const token = localStorage.getItem('token');
            if (!token) {
                this.products = [];
                this.orderedProductIds = [];
                this.renderProductSelect();
                return;
            }
            const orderedRes = await fetch(apiUrl('/api/reviews?orderedProducts=1'), {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (orderedRes.ok) {
                this.products = await orderedRes.json();
                this.orderedProductIds = (this.products || []).map(p => Number(p.id));
            } else {
                this.products = [];
                this.orderedProductIds = [];
            }
        } catch (e) {
            this.products = [];
            this.orderedProductIds = [];
        }

        this.renderProductSelect();
    }

    renderProductSelect() {
        const select = document.getElementById('reviewProductSelect');
        if (!select) return;

        const allowed = new Set((this.orderedProductIds || []).map(x => Number(x)));
        const list = (this.products || []).filter(p => allowed.size === 0 || allowed.has(Number(p.id)));

        select.innerHTML = '<option value="">Оберіть товар…</option>';
        list.forEach(p => {
            const opt = document.createElement('option');
            opt.value = String(p.id);
            opt.textContent = p.name || `Товар #${p.id}`;
            select.appendChild(opt);
        });

        const productRadio = document.querySelector('input[name="subjectType"][value="PRODUCT"]');
        if (productRadio) {
            productRadio.disabled = list.length === 0;
        }
    }

    async loadReviews() {
        try {
            const response = await fetch(apiUrl('/api/reviews'));
            if (response.ok) {
                this.reviews = await response.json();
                this.renderReviews();
            } else {
                console.error('Помилка завантаження відгуків:', response.status);
            }
        } catch (error) {
            console.error('Помилка завантаження відгуків:', error);
        }
    }

    renderReviews() {
        const container = document.getElementById('reviewsList');
        if (!container) return;
        const isAdmin = typeof authManager !== 'undefined' && authManager.isAdmin();

        if (this.reviews.length === 0) {
            container.innerHTML = '<p class="no-reviews">Поки що немає відгуків. Станьте першим!</p>';
            return;
        }

        container.innerHTML = '';
        this.reviews.forEach(review => {
            const reviewCard = document.createElement('div');
            reviewCard.className = 'review-card';
            
            const stars = '⭐'.repeat(review.rating) + '☆'.repeat(5 - review.rating);
            const isProduct = Number(review.productId || 0) > 0 || String(review.subjectType || '').toUpperCase() === 'PRODUCT';
            const subjectLabel = isProduct
                ? `Товар: ${this.escapeHtml(review.productName || ('#' + (review.productId || '')))}`
                : 'Відгук про сайт';
            
            reviewCard.innerHTML = `
                <div class="review-header">
                    <div class="review-user">
                        <strong>${this.escapeHtml(this.maskEmail(review.userEmail || '')) || 'Користувач'}</strong>
                    </div>
                    <div class="review-rating">${stars}</div>
                </div>
                <div style="margin:0.35rem 0 0.6rem; color: var(--text-light); font-weight:800; font-size:0.95rem;">
                    ${subjectLabel}
                </div>
                <div class="review-comment">${this.escapeHtml(review.comment)}</div>
                ${isAdmin ? `
                    <div style="margin-top:0.75rem; display:flex; justify-content:flex-end;">
                        <button type="button" class="btn btn-danger" data-delete-review-id="${review.id}" style="padding:0.5rem 0.9rem; font-size:0.9rem;">
                            Видалити
                        </button>
                    </div>
                ` : ''}
            `;
            container.appendChild(reviewCard);
        });

        if (isAdmin) {
            container.querySelectorAll('[data-delete-review-id]').forEach(btn => {
                btn.addEventListener('click', async () => {
                    const id = Number(btn.getAttribute('data-delete-review-id'));
                    if (!id) return;
                    await this.deleteReview(id);
                });
            });
        }
    }

    async submitReview(rating, comment) {
        const token = localStorage.getItem('token');
        if (!token) {
            alert('Будь ласка, увійдіть в систему для додавання відгуку');
            return;
        }
        if (typeof authManager !== 'undefined' && authManager.ensureProfileComplete && !authManager.ensureProfileComplete()) {
            alert("Завершіть реєстрацію (ім'я та прізвище) перед додаванням відгуку.");
            return;
        }

        try {
            const userStr = localStorage.getItem('user');
const email = userStr ? JSON.parse(userStr).email : "";
            const subjectType = document.querySelector('input[name="subjectType"]:checked')?.value || 'SITE';
            const productIdRaw = document.getElementById('reviewProductSelect')?.value || '';
            const productId = (subjectType === 'PRODUCT') ? Number(productIdRaw || 0) : 0;
            if (subjectType === 'PRODUCT' && !productId) {
                alert('Оберіть товар');
                return;
            }

const formData = `productId=${encodeURIComponent(String(productId))}&rating=${encodeURIComponent(rating)}&comment=${encodeURIComponent(comment)}&userEmail=${encodeURIComponent(email)}`;
            const response = await fetch(apiUrl('/api/reviews'), {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    'Authorization': `Bearer ${token}`
                },
                body: formData
            });

            if (response.ok) {
                alert('Відгук успішно додано!');
                document.getElementById('reviewForm').reset();
                document.querySelectorAll('input[name="rating"]').forEach(radio => radio.checked = false);
                await this.loadReviews();
            } else {
                const error = await response.json();
                alert('Помилка: ' + (error.message || 'Не вдалося додати відгук'));
            }
        } catch (error) {
            console.error('Помилка відправки відгуку:', error);
            alert('Помилка з\'єднання з сервером');
        }
    }

    async deleteReview(reviewId) {
        const token = localStorage.getItem('token');
        if (!token) {
            alert('Потрібно увійти як адміністратор');
            return;
        }
        if (!(typeof authManager !== 'undefined' && authManager.isAdmin())) {
            alert('Видаляти відгуки може лише персонал магазину');
            return;
        }
        if (!confirm('Видалити цей відгук?')) return;

        try {
            const response = await fetch(apiUrl(`/api/reviews/${reviewId}`), {
                method: 'DELETE',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });
            if (response.ok || response.status === 204) {
                await this.loadReviews();
                alert('Відгук видалено');
            } else {
                const raw = await response.text();
                let msg = raw || 'Не вдалося видалити відгук';
                try { msg = (JSON.parse(raw).message || msg); } catch (e) {}
                alert(`Помилка: ${msg}`);
            }
        } catch (error) {
            alert(`Помилка з'єднання з сервером: ${error.message || error}`);
        }
    }

    setupEventListeners() {
        const reviewForm = document.getElementById('reviewForm');
        if (reviewForm) {
            reviewForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const rating = document.querySelector('input[name="rating"]:checked')?.value;
                const comment = document.getElementById('reviewComment').value.trim();

                if (!rating) {
                    alert('Оберіть оцінку');
                    return;
                }
                if (!comment) {
                    alert('Введіть коментар');
                    return;
                }

                await this.submitReview(parseInt(rating), comment);
            });
        }

        const subjectRadios = document.querySelectorAll('input[name="subjectType"]');
        const productSelect = document.getElementById('reviewProductSelect');
        const comment = document.getElementById('reviewComment');
        const syncSubjectUI = () => {
            const subject = document.querySelector('input[name="subjectType"]:checked')?.value || 'SITE';
            const isProduct = subject === 'PRODUCT';
            if (productSelect) productSelect.style.display = isProduct ? 'inline-block' : 'none';
            if (comment) comment.placeholder = isProduct ? 'Напишіть ваш відгук про товар тут...' : 'Напишіть ваш відгук про сайт тут...';
        };
        subjectRadios.forEach(r => r.addEventListener('change', syncSubjectUI));
        syncSubjectUI();
        
        if (typeof authManager !== 'undefined') {
            const originalUpdateUI = authManager.updateUI.bind(authManager);
            authManager.updateUI = function() {
                originalUpdateUI();
                if (reviewManager) {
                    reviewManager.updateAddReviewSection();
                }
            };
        }
    }

    updateAddReviewSection() {
        const addReviewSection = document.getElementById('addReviewSection');
        const isAuthenticated = !!localStorage.getItem('token');
        const isAdmin = typeof authManager !== 'undefined' && authManager.isAdmin();

        if (addReviewSection) {
            if (isAuthenticated && !isAdmin) {
                addReviewSection.style.display = 'block';
            } else {
                addReviewSection.style.display = 'none';
            }
        }
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    maskEmail(email) {
        const e = String(email || '').trim();
        const at = e.indexOf('@');
        if (at <= 1) return '';
        const name = e.slice(0, at);
        const domain = e.slice(at);
        const visible = name.slice(0, 2);
        return `${visible}***${domain}`;
    }
}

let reviewManager;
document.addEventListener('DOMContentLoaded', () => {
    reviewManager = new ReviewManager();
});

function openLoginModal() {
    document.getElementById('loginModal').style.display = 'block';
}

function closeLoginModal() {
    document.getElementById('loginModal').style.display = 'none';
}

function openRegisterModal() {
    document.getElementById('registerModal').style.display = 'block';
}

function closeRegisterModal() {
    document.getElementById('registerModal').style.display = 'none';
}
