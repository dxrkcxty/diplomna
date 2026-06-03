class ProductManager {
    constructor() {
        this.products = [];
        this.currentPage = 1;
        this.productsPerPage = 4;
        this.searchTerm = '';
        this.sortBy = '';
        this.groupFilter = '';
        this.categoriesCache = [];
        this.typeRules = this.createTypeRules();
        this.selectedDetailProductId = null;
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                this.setupEventListeners();
                Promise.all([this.loadCategories(), this.loadProducts()]);
            });
        } else {
            this.setupEventListeners();
            Promise.all([this.loadCategories(), this.loadProducts()]);
        }
    }

    async loadCategories() {
        try {
            const response = await fetch(apiUrl('/api/categories'));
            if (!response.ok) return;
            const categories = await response.json();
            if (!Array.isArray(categories)) return;
            this.categoriesCache = categories;
            this.renderCategorySelects();
        } catch (e) {
            console.error('Помилка завантаження категорій:', e);
        }
    }

    renderCategorySelects() {
        const filterSelect = document.getElementById('groupFilterSelect');
        const productCategory = document.getElementById('productCategory');
        if (filterSelect) {
            const current = filterSelect.value;
            filterSelect.innerHTML = '<option value="">Всі товари</option>';
            this.categoriesCache.forEach((cat) => {
                const opt = document.createElement('option');
                opt.value = String(cat.id);
                opt.textContent = cat.name;
                filterSelect.appendChild(opt);
            });
            if (current) filterSelect.value = current;
        }
        if (productCategory) {
            const current = productCategory.value;
            productCategory.innerHTML = '<option value="">Оберіть категорію</option>';
            this.categoriesCache.forEach((cat) => {
                const opt = document.createElement('option');
                opt.value = String(cat.id);
                opt.textContent = cat.name;
                productCategory.appendChild(opt);
            });
            if (current) productCategory.value = current;
        }
    }

    resolveCategoryNameForType(type) {
        const t = String(type || '').toLowerCase().trim();
        if (!t) return 'Аксесуари';
        if (t.includes('бутси') || t === 'щитки' || t === 'фіксатор') return 'Екіпірування гравця';
        if (t.includes('воротар')) return 'Воротарська екіпіровка';
        if (t.includes('тренувальний інвентар')) return 'Тренувальний інвентар';
        if (t.includes("м'яч") || t.includes('рюкзак') || t.includes('сумка') || t.includes('пляшка')) return 'Аксесуари';
        if (t.includes('футболка') || t.includes('шорти') || t.includes('гетри') || t.includes('термобілизна') || t.includes('костюм') || t.includes('куртка')) {
            return 'Одяг для футболу';
        }
        return 'Аксесуари';
    }

    resolveCategoryIdForType(type) {
        const name = this.resolveCategoryNameForType(type);
        const cat = this.categoriesCache.find((c) => c.name === name);
        return cat ? String(cat.id) : '';
    }

    async loadProducts() {
        try {
            const response = await fetch(apiUrl('/api/products'));
            const contentType = response.headers.get('content-type');
            
            if (response.ok) {
                const text = await response.text();
                
                try {
                    const products = JSON.parse(text);
                    if (Array.isArray(products)) {
                        this.products = products;
                        window.allProducts = this.products;
                        this.renderProducts();
                    } else {
                        console.error('Отримано не масив:', products);
                        this.products = [];
                        window.allProducts = [];
                        this.renderProducts();
                    }
                } catch (parseError) {
                    console.error('Помилка парсингу JSON:', parseError);
                    console.error('Текст, який не вдалося розпарсити:', text);
                    this.products = [];
                    window.allProducts = [];
                    this.renderProducts();
                }
            } else {
                const errorText = await response.text();
                console.error('Помилка завантаження товарів:', response.status, errorText);
                this.products = [];
                window.allProducts = [];
                this.renderProducts();
            }
        } catch (error) {
            console.error('Помилка завантаження товарів:', error);
            this.products = [];
            window.allProducts = [];
            this.renderProducts();
        }
    }

    async addProduct(product) {
        if (typeof authManager === 'undefined' || !(authManager.isAdmin())) {
            alert('Тільки персонал магазину може додавати товари');
            return;
        }

        try {
            const categoryId = product.categoryId || this.resolveCategoryIdForType(product.type) || 0;
            const formData = `name=${encodeURIComponent(product.name || '')}&price=${encodeURIComponent(product.price || 0)}&categoryId=${encodeURIComponent(categoryId)}&type=${encodeURIComponent(product.type || '')}&size=${encodeURIComponent(product.size || '')}&gender=${encodeURIComponent(product.gender || '')}&imageUrl=${encodeURIComponent(product.imageUrl || '')}`;
            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded',
                ...(typeof authManager !== 'undefined' ? authManager.getAuthHeader() : {})
            };


            const response = await fetch(apiUrl('/api/products'), {
                method: 'POST',
                headers: headers,
                body: formData
            });


            if (response.ok || response.status === 201) {
                try {
                    const createdProduct = await response.json();
                } catch (e) {
                }
                await this.loadProducts();
                this.currentPage = 1;
                this.renderProducts();
                return { success: true };
            } else {
                let errorMessage = 'Помилка додавання товару';
                try {
                    const error = await response.json();
                    errorMessage = error.message || error.error || errorMessage;
                    console.error('Помилка від сервера:', error);
                } catch (e) {
                    const text = await response.text();
                    console.error('Помилка парсингу відповіді:', text);
                    errorMessage = text || errorMessage;
                }
                return { success: false, message: errorMessage };
            }
        } catch (error) {
            console.error('Помилка з\'єднання:', error);
            return { success: false, message: 'Помилка з\'єднання з сервером: ' + error.message };
        }
    }

    async updateProduct(id, updatedProduct) {
        if (typeof authManager === 'undefined' || !(authManager.isAdmin())) {
            alert('Тільки персонал магазину може редагувати товари');
            return;
        }

        try {
            const categoryId = updatedProduct.categoryId || this.resolveCategoryIdForType(updatedProduct.type) || 0;
            const formData = `id=${id}&name=${encodeURIComponent(updatedProduct.name || '')}&price=${encodeURIComponent(updatedProduct.price || 0)}&categoryId=${encodeURIComponent(categoryId)}&type=${encodeURIComponent(updatedProduct.type || '')}&size=${encodeURIComponent(updatedProduct.size || '')}&gender=${encodeURIComponent(updatedProduct.gender || '')}&imageUrl=${encodeURIComponent(updatedProduct.imageUrl || '')}`;
            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded',
                ...(typeof authManager !== 'undefined' ? authManager.getAuthHeader() : {})
            };

            const response = await fetch(apiUrl(`/api/products/${id}`), {
                method: 'PUT',
                headers: headers,
                body: formData
            });

            if (response.ok) {
                await this.loadProducts();
                return { success: true };
            } else {
                const error = await response.json();
                return { success: false, message: error.message || 'Помилка оновлення товару' };
            }
        } catch (error) {
            return { success: false, message: 'Помилка з\'єднання з сервером' };
        }
    }

    async deleteProduct(id) {
        if (typeof authManager === 'undefined' || !(authManager.isAdmin())) {
            alert('Тільки персонал магазину може видаляти товари');
            return;
        }

        try {
            const headers = {
                ...authManager.getAuthHeader()
            };

            const response = await fetch(apiUrl(`/api/products/${id}`), {
                method: 'DELETE',
                headers: headers
            });

            if (response.ok || response.status === 204) {
                await this.loadProducts();
                return { success: true };
            } else {
                const error = await response.json();
                return { success: false, message: error.message || 'Помилка видалення товару' };
            }
        } catch (error) {
            return { success: false, message: 'Помилка з\'єднання з сервером' };
        }
    }

    filterProducts(searchTerm) {
        if (!searchTerm) return this.products;
        return this.products.filter(product => 
            product.name && product.name.toLowerCase().includes(searchTerm.toLowerCase())
        );
    }

    filterByCategory(products, categoryId) {
        if (!categoryId) return products;
        const id = Number(categoryId);
        return products.filter((product) => Number(product.categoryId) === id);
    }

    applyCurrentFilters() {
        let filtered = this.filterProducts(this.searchTerm);
        filtered = this.filterByCategory(filtered, this.groupFilter);
        if (this.sortBy) {
            filtered = this.sortProducts(filtered, this.sortBy);
        }
        this.renderProducts(filtered);
    }

    sortProducts(products, sortBy) {
        return [...products].sort((a, b) => {
            if (sortBy === 'price_asc' || sortBy === 'price') {
                return Number(a.price || 0) - Number(b.price || 0);
            }
            if (sortBy === 'price_desc') {
                return Number(b.price || 0) - Number(a.price || 0);
            }
            if (sortBy === 'name_asc' || sortBy === 'name') {
                return (a.name || '').localeCompare(b.name || '');
            }
            if (sortBy === 'name_desc') {
                return (b.name || '').localeCompare(a.name || '');
            }
            return 0;
        });
    }

    renderProducts(products = this.products) {
        const container = document.getElementById('productsContainer');
        if (!container) {
            console.error('productsContainer не знайдено!');
            return;
        }
        
        container.innerHTML = '';

        if (products.length === 0) {
            container.innerHTML = '<p style="text-align: center; padding: 2rem;">Товари не знайдено</p>';
            return;
        }

        const isAdmin = typeof authManager !== 'undefined' && authManager.isAdmin();
        const isAuthenticated = typeof authManager !== 'undefined' && authManager.isAuthenticated();
        const isFavorite = (productId) => typeof cartManager !== 'undefined' && cartManager.isFavorite(productId);

        const pageProducts = this.getProductsForPage(products);
        pageProducts.forEach(product => {
            const card = document.createElement('div');
            card.className = 'product-card';
            card.style.cursor = 'pointer';
            
            const productName = product.name || 'Товар #' + product.id;
            const displayPrice = product.price ? product.price.toFixed(2) : '0.00';
            const favorite = isFavorite(product.id);
            
            const imageSrc = this.escapeHtml(normalizeImageUrl(product.imageUrl));
            const productImage = `<img src="${imageSrc}" alt="${this.escapeHtml(productName)}" style="width: 100%; height: 200px; object-fit: cover; border-radius: 5px; margin-bottom: 10px;" onerror="handleImageError(this)">`;
            const productCategory = product.categoryName
                ? `<p><strong>Категорія:</strong> ${this.escapeHtml(product.categoryName)}</p>`
                : '';
            const productType = product.type ? `<p><strong>Тип:</strong> ${this.escapeHtml(product.type)}</p>` : '';
            const productSize = product.size ? `<p><strong>Розмір:</strong> ${this.escapeHtml(product.size)}</p>` : '';
            const productGender = product.gender ? `<p><strong>Стать:</strong> ${this.escapeHtml(product.gender)}</p>` : '';
            
            card.innerHTML = `
                ${productImage}
                <div class="product-info">
                    <h3>${this.escapeHtml(productName)}</h3>
                    ${productCategory}
                    ${productType}
                    ${productSize}
                    ${productGender}
                    ${this.renderPrice(product)}
                </div>
                ${isAdmin ? `
                <div class="product-actions">
                    <button class="btn" onclick="productManager.editProduct(${product.id})">Редагувати</button>
                    <button class="btn btn-danger" onclick="productManager.confirmDelete(${product.id})">Видалити</button>
                </div>
                ` : isAuthenticated ? `
                <div class="product-actions">
                    <button class="btn" onclick="addToCartHandler(${product.id})">Додати в корзину</button>
                    <button class="btn ${favorite ? 'btn-favorite' : ''}" onclick="toggleFavorite(${product.id})" data-product-id="${product.id}">
                        ${favorite ? '❤️ В обраному' : '🤍 Додати в обране'}
                    </button>
                </div>
                ` : '<p style="text-align: center; color: #999; padding: 1rem;">Увійдіть, щоб додати в корзину</p>'}
            `;
            card.addEventListener('click', () => {
                if (isAdmin) return;
                this.openProductDetailModal(product.id);
            });
            card.querySelectorAll('button').forEach((btn) => {
                btn.addEventListener('click', (e) => e.stopPropagation());
            });
            container.appendChild(card);
        });
        this.renderPagination(products);
    }

    escapeHtml(text) {
        const div = document.createElement('div');
        div.textContent = text;
        return div.innerHTML;
    }

    renderPrice(product) {
        const originalPrice = product.price || 0;
        const discountPercent = product.discountPercent || 0;
        const discountAmount = product.discountAmount || 0;
        let finalPrice = originalPrice;
        let discountText = '';
        
        if (discountPercent > 0) {
            finalPrice = originalPrice * (1 - discountPercent / 100);
            discountText = `-${discountPercent.toFixed(0)}%`;
        } else if (discountAmount > 0) {
            finalPrice = Math.max(0, originalPrice - discountAmount);
            discountText = `-${discountAmount.toFixed(0)} грн`;
        }
        
        if (discountText) {
            return `
                <p style="margin-top: 1rem;">
                    <span style="text-decoration: line-through; color: var(--text-light); font-size: 0.9rem; margin-right: 0.5rem;">${originalPrice.toFixed(2)} грн</span>
                    <span style="font-size: 1.25rem; font-weight: 700; color: var(--danger-color);">${finalPrice.toFixed(2)} грн</span>
                    <span style="background: var(--danger-color); color: white; padding: 0.25rem 0.5rem; border-radius: 6px; font-size: 0.85rem; font-weight: 600; margin-left: 0.5rem;">${discountText}</span>
                </p>
            `;
        } else {
            return `<p style="margin-top: 1rem; font-size: 1.25rem; font-weight: 700; color: var(--primary-color);"><strong>Ціна:</strong> ${originalPrice.toFixed(2)} грн</p>`;
        }
    }

    getProductsForPage(products) {
        const start = (this.currentPage - 1) * this.productsPerPage;
        return products.slice(start, start + this.productsPerPage);
    }

    renderPagination(products) {
        let pagination = document.getElementById('pagination');
        if (!pagination) {
            const container = document.getElementById('productsContainer');
            if (!container) return;
            pagination = document.createElement('div');
            pagination.id = 'pagination';
            pagination.className = 'pagination';
            container.parentNode.appendChild(pagination);
        }
        pagination.innerHTML = '';
        const totalPages = Math.ceil(products.length / this.productsPerPage);
        if (totalPages <= 1) {
            pagination.style.display = 'none';
            return;
        }
        pagination.style.display = 'flex';
        const prevBtn = document.createElement('button');
        prevBtn.textContent = '<';
        prevBtn.disabled = this.currentPage === 1;
        prevBtn.className = 'pagination-btn';
        prevBtn.onclick = () => {
            this.currentPage--;
            this.renderProducts(products);
        };
        pagination.appendChild(prevBtn);
        for (let i = 1; i <= totalPages; i++) {
            const pageBtn = document.createElement('button');
            pageBtn.textContent = i;
            pageBtn.className = 'pagination-btn' + (i === this.currentPage ? ' active' : '');
            pageBtn.onclick = () => {
                this.currentPage = i;
                this.renderProducts(products);
            };
            pagination.appendChild(pageBtn);
        }
        const nextBtn = document.createElement('button');
        nextBtn.textContent = '>';
        nextBtn.disabled = this.currentPage === totalPages;
        nextBtn.className = 'pagination-btn';
        nextBtn.onclick = () => {
            this.currentPage++;
            this.renderProducts(products);
        };
        pagination.appendChild(nextBtn);
    }

    setupEventListeners() {
        const searchInput = document.getElementById('searchInput');
        const sortSelect = document.getElementById('sortSelect');
        const groupFilterSelect = document.getElementById('groupFilterSelect');
        const addProductBtn = document.getElementById('addProductBtn');
        const productForm = document.getElementById('productForm');
        const productTypeSelect = document.getElementById('productType');
        const confirmDelete = document.getElementById('confirmDelete');
        const cancelDelete = document.getElementById('cancelDelete');
        const detailSize = document.getElementById('detailSize');
        const detailColor = document.getElementById('detailColor');
        const detailCloseBtn = document.getElementById('detailCloseBtn');
        const closeDetailModal = document.getElementById('closeDetailModal');
        const detailAddToCartBtn = document.getElementById('detailAddToCartBtn');

        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.currentPage = 1;
                this.searchTerm = e.target.value || '';
                this.applyCurrentFilters();
            });
        }

        if (sortSelect) {
            sortSelect.addEventListener('change', (e) => {
                this.currentPage = 1;
                this.sortBy = e.target.value || '';
                this.applyCurrentFilters();
            });
        }

        if (groupFilterSelect) {
            groupFilterSelect.addEventListener('change', (e) => {
                this.currentPage = 1;
                this.groupFilter = e.target.value || '';
                this.applyCurrentFilters();
            });
        }

        document.querySelectorAll('[data-group-filter]').forEach((btn) => {
            btn.addEventListener('click', () => {
                const nextFilter = btn.getAttribute('data-group-filter') || '';
                this.groupFilter = nextFilter;
                this.currentPage = 1;
                if (groupFilterSelect) {
                    groupFilterSelect.value = nextFilter;
                }
                this.applyCurrentFilters();
            });
        });
            
        if (addProductBtn) {
            addProductBtn.addEventListener('click', () => {
                if (typeof authManager === 'undefined' || !(authManager.isAdmin())) {
                    alert('Тільки персонал магазину може додавати товари');
                    return;
                }
                this.openProductModal();
            });
        }

        document.querySelectorAll('.close').forEach(closeBtn => {
            closeBtn.addEventListener('click', () => {
                const productModal = document.getElementById('productModal');
                const deleteModal = document.getElementById('deleteModal');
                const detailModal = document.getElementById('productDetailModal');
                if (productModal) productModal.style.display = 'none';
                if (deleteModal) deleteModal.style.display = 'none';
                if (detailModal) detailModal.style.display = 'none';
            });
        });

        if (productForm) {
            productForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const form = e.target;
                const productData = this.collectFormProductData(form);

                let result;
                if (form.productId.value) {
                    result = await this.updateProduct(form.productId.value, productData);
                } else {
                    result = await this.addProduct(productData);
                }

                if (result && result.success) {
                    document.getElementById('productModal').style.display = 'none';
                    form.reset();
                    const imagePreview = document.getElementById('imagePreview');
                    if (imagePreview) imagePreview.style.display = 'none';
                    setTimeout(() => {
                        if (this.products.length === 0) {
                            this.loadProducts();
                        } else {
                            this.currentPage = 1;
                            this.renderProducts();
                        }
                    }, 200);
                } else {
                    console.error('Помилка збереження товару:', result);
                    alert(result?.message || 'Помилка збереження товару');
                }
            });

            const imageUrlInput = document.getElementById('imageUrl');
            if (imageUrlInput) {
                imageUrlInput.addEventListener('input', (e) => {
                    const url = e.target.value.trim();
                    const preview = document.getElementById('imagePreview');
                    const previewImg = document.getElementById('previewImg');
                    if (url) {
                        previewImg.src = normalizeImageUrl(url);
                        previewImg.onerror = () => handleImageError(previewImg);
                        preview.style.display = 'block';
                    } else {
                        preview.style.display = 'none';
                    }
                });
            }
        }

        if (productTypeSelect) {
            productTypeSelect.addEventListener('change', () => {
                this.applyProductTypeRules();
                const catSelect = document.getElementById('productCategory');
                const catId = this.resolveCategoryIdForType(productTypeSelect.value);
                if (catSelect && catId) catSelect.value = catId;
            });
        }

        if (confirmDelete) {
            confirmDelete.addEventListener('click', async () => {
                const id = confirmDelete.dataset.id;
                if (id) {
                    const result = await this.deleteProduct(id);
                    if (result && result.success) {
                        document.getElementById('deleteModal').style.display = 'none';
                    } else {
                        alert(result?.message || 'Помилка видалення товару');
                    }
                }
            });
        }

        if (cancelDelete) {
            cancelDelete.addEventListener('click', () => {
                document.getElementById('deleteModal').style.display = 'none';
            });
        }

        if (detailSize) {
            detailSize.addEventListener('change', () => {
                this.updateDetailPriceDisplay();
                this.refreshDetailAvailability();
            });
        }
        if (detailColor) {
            detailColor.addEventListener('change', () => this.refreshDetailAvailability());
        }
        if (detailCloseBtn) {
            detailCloseBtn.addEventListener('click', () => this.closeProductDetailModal());
        }
        if (closeDetailModal) {
            closeDetailModal.addEventListener('click', () => this.closeProductDetailModal());
        }
        if (detailAddToCartBtn) {
            detailAddToCartBtn.addEventListener('click', () => this.addSelectedDetailToCart());
        }
    }

    openProductModal(product = null) {
        const modal = document.getElementById('productModal');
        const form = document.getElementById('productForm');
        const title = document.getElementById('modalTitle');
        const imagePreview = document.getElementById('imagePreview');

        if (!modal || !form || !title) return;

        if (product) {
            title.textContent = 'Редагувати товар';
            form.productId.value = product.id;
            form.productName.value = product.name || '';
            form.productType.value = product.type || '';
            const catSelect = document.getElementById('productCategory');
            if (catSelect) {
                catSelect.value = product.categoryId ? String(product.categoryId) : this.resolveCategoryIdForType(product.type);
            }
            form.price.value = product.price || '';
            form.imageUrl.value = product.imageUrl || '';
            this.applyProductTypeRules({ size: product.size || '', gender: product.gender || '' });
            
            if (product.imageUrl) {
                const previewImg = document.getElementById('previewImg');
                previewImg.src = normalizeImageUrl(product.imageUrl);
                previewImg.onerror = () => handleImageError(previewImg);
                imagePreview.style.display = 'block';
            } else {
                imagePreview.style.display = 'none';
            }
        } else {
            title.textContent = 'Додати новий товар';
            form.reset();
            form.productId.value = '';
            this.applyProductTypeRules();
            if (imagePreview) imagePreview.style.display = 'none';
        }

        modal.style.display = 'block';
    }

    async editProduct(id) {
        const product = window.allProducts.find(p => p.id == id);
        if (product) {
            this.openProductModal(product);
        }
    }

    confirmDelete(id) {
        const modal = document.getElementById('deleteModal');
        const confirmBtn = document.getElementById('confirmDelete');
        if (modal && confirmBtn) {
            confirmBtn.dataset.id = id;
            modal.style.display = 'block';
        }
    }

    createTypeRules() {
        const apparelSizes = ['XS', 'S', 'M', 'L', 'XL', 'XXL'];
        const bootSizes = ['36', '37', '38', '39', '40', '41', '42', '43', '44', '45'];
        const gloveSizes = ['7', '8', '9', '10', '11'];
        const protectionSizes = ['S', 'M', 'L', 'XL'];
        const oneSize = ['one size'];
        const ballSizes = ['3', '4', '5'];
        return {
            'бутси': { showSize: true, sizeOptions: bootSizes, showGender: true, requireSize: true, requireGender: true },
            'ігрова футболка': { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true },
            'ігрові шорти': { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true },
            'гетри': { showSize: true, sizeOptions: apparelSizes, showGender: false, requireSize: true, requireGender: false },
            'термобілизна': { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true },
            'воротарський світшот': { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true },
            'воротарські штани': { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true },
            'тренувальний костюм': { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true },
            'куртка для тренувань': { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true },
            'воротарські рукавиці': { showSize: true, sizeOptions: gloveSizes, showGender: false, requireSize: true, requireGender: false },
            'щитки': { showSize: true, sizeOptions: protectionSizes, showGender: false, requireSize: true, requireGender: false },
            'фіксатор': { showSize: true, sizeOptions: protectionSizes, showGender: false, requireSize: true, requireGender: false },
            "м'яч": { showSize: true, sizeOptions: ballSizes, showGender: false, requireSize: true, requireGender: false },
            'пляшка': { showSize: false, sizeOptions: oneSize, showGender: false, requireSize: false, requireGender: false },
            'рюкзак': { showSize: false, sizeOptions: oneSize, showGender: false, requireSize: false, requireGender: false },
            'сумка': { showSize: false, sizeOptions: oneSize, showGender: false, requireSize: false, requireGender: false },
            'тренувальний інвентар': { showSize: false, sizeOptions: oneSize, showGender: false, requireSize: false, requireGender: false },
            default: { showSize: true, sizeOptions: apparelSizes, showGender: true, requireSize: true, requireGender: true }
        };
    }

    getTypeRule(typeValue) {
        if (!typeValue) return this.typeRules.default;
        return this.typeRules[typeValue] || this.typeRules.default;
    }

    applyProductTypeRules(prefill = {}) {
        const typeSelect = document.getElementById('productType');
        const sizeSelect = document.getElementById('productSize');
        const genderSelect = document.getElementById('productGender');
        const sizeGroup = document.getElementById('productSizeGroup');
        const genderGroup = document.getElementById('productGenderGroup');
        if (!typeSelect || !sizeSelect || !genderSelect || !sizeGroup || !genderGroup) return;

        const rule = this.getTypeRule(typeSelect.value);
        const nextSizeOptions = Array.isArray(rule.sizeOptions) && rule.sizeOptions.length ? rule.sizeOptions : ['one size'];
        const previousSize = prefill.size != null ? String(prefill.size) : String(sizeSelect.value || '');
        const previousGender = prefill.gender != null ? String(prefill.gender) : String(genderSelect.value || '');

        sizeSelect.innerHTML = '';
        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = 'Оберіть розмір';
        sizeSelect.appendChild(placeholder);
        nextSizeOptions.forEach((size) => {
            const opt = document.createElement('option');
            opt.value = String(size);
            opt.textContent = String(size);
            sizeSelect.appendChild(opt);
        });
        if (nextSizeOptions.includes(previousSize)) {
            sizeSelect.value = previousSize;
        } else {
            sizeSelect.value = '';
        }

        sizeGroup.style.display = rule.showSize ? 'block' : 'none';
        genderGroup.style.display = rule.showGender ? 'block' : 'none';
        sizeSelect.required = !!rule.requireSize && rule.showSize;
        genderSelect.required = !!rule.requireGender && rule.showGender;

        if (!rule.showSize) {
            sizeSelect.value = '';
        }
        if (!rule.showGender) {
            genderSelect.value = '';
        } else if (['чоловічий', 'жіночий', 'унісекс'].includes(previousGender)) {
            genderSelect.value = previousGender;
        }
    }

    collectFormProductData(form) {
        const type = form.productType.value;
        const rule = this.getTypeRule(type);
        const sizeValue = rule.showSize ? (form.productSize.value || '') : '';
        const genderValue = rule.showGender ? (form.productGender.value || '') : '';
        const categoryId = Number(document.getElementById('productCategory')?.value || 0)
            || Number(this.resolveCategoryIdForType(type) || 0);
        return {
            id: form.productId.value || null,
            name: form.productName.value,
            price: parseFloat(form.price.value),
            categoryId: categoryId,
            type: type,
            size: sizeValue,
            gender: genderValue,
            imageUrl: normalizeStoredImageUrl(form.imageUrl.value || '')
        };
    }

    getDetailColorsByType(typeValue) {
        const type = String(typeValue || '').toLowerCase();
        if (type.includes('бутс')) return ['Чорний', 'Білий', 'Синій', 'Червоний'];
        if (type.includes('рукави')) return ['Чорний', 'Білий', 'Салатовий'];
        if (type.includes('м\'яч')) return ['Білий/Чорний', 'Жовтий/Синій'];
        if (type.includes('рюкзак') || type.includes('сумка')) return ['Чорний', 'Синій', 'Сірий'];
        if (type.includes('пляшка')) return ['Чорний', 'Синій', 'Прозорий'];
        return ['Чорний', 'Білий', 'Синій'];
    }

    parseSizeNumber(size) {
        if (size == null || size === '') return null;
        const match = String(size).match(/(\d+(?:\.\d+)?)/);
        return match ? Number(match[1]) : null;
    }

    getSizeReferenceIndex(sizeOptions, productSize) {
        const options = (sizeOptions || []).map(String);
        if (!options.length) return 0;
        const fromProduct = String(productSize || '');
        let idx = options.indexOf(fromProduct);
        if (idx >= 0) return idx;

        const productNum = this.parseSizeNumber(fromProduct);
        if (productNum != null) {
            let bestIdx = 0;
            let bestDiff = Infinity;
            options.forEach((opt, i) => {
                const n = this.parseSizeNumber(opt);
                if (n == null) return;
                const diff = Math.abs(n - productNum);
                if (diff < bestDiff) {
                    bestDiff = diff;
                    bestIdx = i;
                }
            });
            return bestIdx;
        }
        return Math.floor(options.length / 2);
    }

    getSizeAdjustedBasePrice(product, selectedSize) {
        const basePrice = Number(product?.price || 0);
        if (!basePrice) return 0;
        if (!selectedSize) return basePrice;

        const rule = this.getTypeRule(product.type);
        const options = Array.isArray(rule.sizeOptions) ? rule.sizeOptions.map(String) : [];
        if (!rule.showSize || !options.length) return basePrice;

        const selectedIdx = options.indexOf(String(selectedSize));
        if (selectedIdx < 0) return basePrice;

        const refIdx = this.getSizeReferenceIndex(options, product.size);
        const steps = selectedIdx - refIdx;
        const perStep = 0.015;
        const adjusted = basePrice * (1 + steps * perStep);
        const minPrice = basePrice * 0.82;
        const maxPrice = basePrice * 1.12;
        return Number(Math.max(minPrice, Math.min(maxPrice, adjusted)).toFixed(2));
    }

    formatDetailPrice(product, selectedSize) {
        const adjustedBase = this.getSizeAdjustedBasePrice(product, selectedSize);
        const discountPercent = Number(product?.discountPercent || 0);
        const discountAmount = Number(product?.discountAmount || 0);
        let finalPrice = adjustedBase;
        if (discountPercent > 0) {
            finalPrice = adjustedBase * (1 - discountPercent / 100);
        } else if (discountAmount > 0) {
            finalPrice = Math.max(0, adjustedBase - discountAmount);
        }

        const sizeNote = selectedSize
            ? ` <span style="color:var(--text-light);font-weight:500;font-size:0.9rem;">(розмір ${this.escapeHtml(selectedSize)})</span>`
            : '';

        if (discountPercent > 0 || discountAmount > 0) {
            return `<span style="text-decoration:line-through;color:var(--text-light);margin-right:0.35rem;">${adjustedBase.toFixed(2)} грн</span><span style="font-weight:700;color:var(--danger-color);">${finalPrice.toFixed(2)} грн</span>${sizeNote}`;
        }
        return `<span style="font-weight:700;color:var(--primary-color);">${adjustedBase.toFixed(2)} грн</span>${sizeNote}`;
    }

    updateDetailPriceDisplay() {
        const priceEl = document.getElementById('detailPrice');
        const product = this.products.find((p) => Number(p.id) === Number(this.selectedDetailProductId));
        if (!priceEl || !product) return;

        const sizeSelect = document.getElementById('detailSize');
        const sizeVisible = sizeSelect && sizeSelect.parentElement && sizeSelect.parentElement.style.display !== 'none';
        const selectedSize = sizeVisible ? sizeSelect.value : '';
        priceEl.innerHTML = `<p style="margin-bottom:0.5rem;"><strong>Ціна:</strong> ${this.formatDetailPrice(product, selectedSize)}</p>`;
    }

    getStockForVariant(productId, sizeValue, colorValue) {
        const raw = `${productId}|${sizeValue || ''}|${colorValue || ''}`;
        let hash = 0;
        for (let i = 0; i < raw.length; i += 1) {
            hash = ((hash << 5) - hash) + raw.charCodeAt(i);
            hash |= 0;
        }
        const qty = Math.abs(hash % 8);
        return qty;
    }

    openProductDetailModal(productId) {
        const modal = document.getElementById('productDetailModal');
        const title = document.getElementById('detailTitle');
        const body = document.getElementById('detailBody');
        const sizeGroup = document.getElementById('detailSizeGroup');
        const sizeSelect = document.getElementById('detailSize');
        const colorSelect = document.getElementById('detailColor');
        const addBtn = document.getElementById('detailAddToCartBtn');
        if (!modal || !title || !body || !sizeGroup || !sizeSelect || !colorSelect || !addBtn) return;

        const product = this.products.find((p) => Number(p.id) === Number(productId));
        if (!product) return;
        this.selectedDetailProductId = Number(productId);

        const productName = product.name || `Товар #${product.id}`;
        title.textContent = productName;
        body.innerHTML = `
            <p style="margin-bottom:0.5rem;"><strong>Категорія:</strong> ${this.escapeHtml(product.categoryName || '—')}</p>
            <p style="margin-bottom:0.5rem;"><strong>Тип:</strong> ${this.escapeHtml(product.type || '—')}</p>`;
        const priceHost = document.getElementById('detailPrice');
        if (priceHost) priceHost.innerHTML = '';

        const rule = this.getTypeRule(product.type);
        const sizes = rule.showSize && Array.isArray(rule.sizeOptions) ? rule.sizeOptions : [];
        sizeSelect.innerHTML = '';
        if (sizes.length) {
            sizes.forEach((size) => {
                const opt = document.createElement('option');
                opt.value = String(size);
                opt.textContent = String(size);
                sizeSelect.appendChild(opt);
            });
            const defaultSize = sizes.map(String).includes(String(product.size || ''))
                ? String(product.size)
                : String(sizes[0]);
            sizeSelect.value = defaultSize;
            sizeGroup.style.display = 'block';
        } else {
            sizeGroup.style.display = 'none';
        }

        const colors = this.getDetailColorsByType(product.type);
        colorSelect.innerHTML = '';
        colors.forEach((color) => {
            const opt = document.createElement('option');
            opt.value = color;
            opt.textContent = color;
            colorSelect.appendChild(opt);
        });

        addBtn.style.display = (typeof authManager !== 'undefined' && authManager.isAuthenticated()) ? 'inline-block' : 'none';
        this.updateDetailPriceDisplay();
        this.refreshDetailAvailability();
        modal.style.display = 'block';
    }

    refreshDetailAvailability() {
        const availability = document.getElementById('detailAvailability');
        const addBtn = document.getElementById('detailAddToCartBtn');
        const sizeSelect = document.getElementById('detailSize');
        const colorSelect = document.getElementById('detailColor');
        if (!availability || !addBtn) return;

        const sizeValue = sizeSelect && sizeSelect.parentElement.style.display !== 'none' ? sizeSelect.value : '';
        const colorValue = colorSelect ? colorSelect.value : '';
        const qty = this.getStockForVariant(this.selectedDetailProductId || 0, sizeValue, colorValue);
        if (qty > 0) {
            availability.textContent = `В наявності: ${qty} шт.`;
            availability.style.color = 'var(--success-color)';
            addBtn.disabled = false;
        } else {
            availability.textContent = 'Немає в наявності';
            availability.style.color = 'var(--danger-color)';
            addBtn.disabled = true;
        }
    }

    addSelectedDetailToCart() {
        const product = this.products.find((p) => Number(p.id) === Number(this.selectedDetailProductId));
        if (!product) return;
        if (typeof authManager === 'undefined' || !authManager.isAuthenticated()) {
            alert('Увійдіть, щоб додати товар в кошик');
            return;
        }
        if (typeof cartManager === 'undefined') return;

        const sizeSelect = document.getElementById('detailSize');
        const colorSelect = document.getElementById('detailColor');
        const selectedSize = sizeSelect && sizeSelect.parentElement.style.display !== 'none' ? sizeSelect.value : '';
        const selectedColor = colorSelect ? colorSelect.value : '';
        const qty = this.getStockForVariant(this.selectedDetailProductId || 0, selectedSize, selectedColor);
        if (qty <= 0) {
            alert('Ця комбінація зараз недоступна');
            return;
        }

        const adjustedBase = this.getSizeAdjustedBasePrice(product, selectedSize);
        const customProduct = {
            ...product,
            price: adjustedBase,
            size: selectedSize || product.size || '',
            color: selectedColor || ''
        };
        cartManager.addToCart(customProduct);
        if (cartManager.isFavorite(product.id)) {
            cartManager.removeFromFavorites(product.id);
        }
        alert('Товар додано в кошик');
        this.closeProductDetailModal();
    }

    closeProductDetailModal() {
        const modal = document.getElementById('productDetailModal');
        if (modal) modal.style.display = 'none';
        this.selectedDetailProductId = null;
    }
}

if (!window.productManager) {
    window.productManager = new ProductManager();
}

function addToCartHandler(productId) {
    if (typeof cartManager === 'undefined') return;
    const product = window.allProducts.find(p => p.id === productId);
    if (product) {
        cartManager.addToCart(product);
        if (cartManager.isFavorite(productId)) {
            cartManager.removeFromFavorites(productId);
        }
        alert('Товар додано в корзину!');
    }
}

function toggleFavorite(productId) {
    if (typeof cartManager === 'undefined') return;
    const product = window.allProducts.find(p => p.id === productId);
    if (!product) return;
    
    if (cartManager.isFavorite(productId)) {
        cartManager.removeFromFavorites(productId);
    } else {
        cartManager.addToFavorites(product);
    }
    window.productManager.renderProducts();
}
