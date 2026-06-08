class CartManager {
    constructor() {
        this.cart = JSON.parse(localStorage.getItem('cart')) || [];
        this.favorites = JSON.parse(localStorage.getItem('favorites')) || [];
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => {
                this.updateCartBadge();
                this.updateFavoritesBadge();
            });
        } else {
            this.updateCartBadge();
            this.updateFavoritesBadge();
        }
    }

    addToCart(product) {
        const normalized = this.normalizeProductForStorage(product);
        const existingItem = this.cart.find(item => item.id === product.id);
        if (existingItem) {
            existingItem.quantity = (existingItem.quantity || 1) + 1;
        } else {
            this.cart.push({ ...normalized, quantity: 1 });
        }
        this.saveCart();
        this.updateCartBadge();
    }

    removeFromCart(productId) {
        this.cart = this.cart.filter(item => item.id !== productId);
        this.saveCart();
        this.updateCartBadge();
    }

    getCartTotal() {
        return this.cart.reduce((total, item) => total + (item.price * (item.quantity || 1)), 0);
    }

    saveCart() {
        localStorage.setItem('cart', JSON.stringify(this.cart));
    }

    addToFavorites(product) {
        const normalized = this.normalizeProductForStorage(product);
        if (!this.favorites.find(item => item.id === product.id)) {
            this.favorites.push(normalized);
            this.saveFavorites();
            this.updateFavoritesBadge();
        }
    }

    removeFromFavorites(productId) {
        this.favorites = this.favorites.filter(item => item.id !== productId);
        this.saveFavorites();
        this.updateFavoritesBadge();
    }

    isFavorite(productId) {
        return this.favorites.some(item => item.id === productId);
    }

    saveFavorites() {
        localStorage.setItem('favorites', JSON.stringify(this.favorites));
    }

    updateCartBadge() {
        const badge = document.getElementById('cartBadge');
        if (badge) {
            const count = this.cart.reduce((sum, item) => sum + (item.quantity || 1), 0);
            badge.textContent = count;
            badge.style.display = count > 0 ? 'inline-block' : 'none';
        }
    }

    updateFavoritesBadge() {
        const badge = document.getElementById('favoritesBadge');
        if (badge) {
            badge.textContent = this.favorites.length;
            badge.style.display = this.favorites.length > 0 ? 'inline-block' : 'none';
        }
    }

    normalizeProductForStorage(product) {
        const originalPrice = Number(product?.originalPrice ?? product?.price ?? 0);
        const discountPercent = Number(product?.discountPercent || 0);
        const discountAmount = Number(product?.discountAmount || 0);

        let finalPrice = originalPrice;
        if (discountPercent > 0) {
            finalPrice = originalPrice * (1 - discountPercent / 100);
        } else if (discountAmount > 0) {
            finalPrice = Math.max(0, originalPrice - discountAmount);
        }

        return {
            ...product,
            originalPrice: originalPrice,
            price: Number(finalPrice.toFixed(2)),
            discountPercent: discountPercent > 0 ? discountPercent : null,
            discountAmount: discountAmount > 0 ? discountAmount : null,
            imageUrl: productStoredImageUrl(product),
            size: product.size || '',
            color: product.color || ''
        };
    }
}

const cartManager = new CartManager();

