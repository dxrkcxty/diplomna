(function () {
    const FALLBACK_IMAGE_PATH = 'assets/images/placeholder.svg';
    const DEFAULT_IMAGE_NAMES = [
        'product-default.svg',
        'placeholder.svg',
    ];
    const PRODUCT_IMAGE_MAP = [
        { keys: ['рюкзак'], path: 'assets/images/products/backpack.svg' },
        { keys: ['сумк'], path: 'assets/images/products/bag.svg' },
        { keys: ['бутс', 'predator', 'mercurial', 'future'], path: 'assets/images/products/boots.svg' },
        { keys: ['футболк', 'світшот'], path: 'assets/images/products/jersey.svg' },
        { keys: ['шорт'], path: 'assets/images/products/shorts.svg' },
        { keys: ['гетр'], path: 'assets/images/products/socks.svg' },
        { keys: ['термобілизн', 'костюм'], path: 'assets/images/products/training-suit.svg' },
        { keys: ['куртк'], path: 'assets/images/products/jacket.svg' },
        { keys: ['рукавиц'], path: 'assets/images/products/gloves.svg' },
        { keys: ['штани'], path: 'assets/images/products/pants.svg' },
        { keys: ['щитк', 'фіксатор'], path: 'assets/images/products/protection.svg' },
        { keys: ['пляшк'], path: 'assets/images/products/bottle.svg' },
        { keys: ['м\'яч', 'м’яч', 'ball'], path: 'assets/images/products/ball.svg' },
        { keys: ['інвентар', 'драбин', 'конус'], path: 'assets/images/products/training-gear.svg' },
    ];

    function localAssetUrl(path) {
        const cleanPath = String(path || FALLBACK_IMAGE_PATH)
            .trim()
            .replace(/\\/g, '/')
            .replace(/^\.?\//, '');

        try {
            return new URL(cleanPath || FALLBACK_IMAGE_PATH, document.baseURI).href;
        } catch (e) {
            return cleanPath || FALLBACK_IMAGE_PATH;
        }
    }

    function normalizeImageUrl(value) {
        let url = String(value || '').trim();
        if (!url) return localAssetUrl(FALLBACK_IMAGE_PATH);

        url = url.replace(/\\/g, '/');

        if (/^data:image\//i.test(url) || /^blob:/i.test(url)) {
            return url;
        }

        if (/^\/?assets\//i.test(url) || /^\.\/assets\//i.test(url)) {
            return localAssetUrl(url.replace(/^\//, ''));
        }

        if (/^\/\//.test(url)) {
            return `${window.location.protocol === 'http:' ? 'http:' : 'https:'}${url}`;
        }

        if (/^www\./i.test(url)) {
            return `https://${url}`;
        }

        if (/^http:\/\//i.test(url) && !/^http:\/\/(localhost|127\.0\.0\.1|0\.0\.0\.0)(:|\/|$)/i.test(url)) {
            return `https://${url.slice(7)}`;
        }

        try {
            return new URL(url, document.baseURI).href;
        } catch (e) {
            return localAssetUrl(FALLBACK_IMAGE_PATH);
        }
    }

    function isDefaultImageUrl(value) {
        const url = String(value || '').trim().toLowerCase().replace(/\\/g, '/');
        if (!url) return true;
        return DEFAULT_IMAGE_NAMES.some((name) => (
            url.endsWith(`/assets/images/${name}`)
            || url.endsWith(`assets/images/${name}`)
            || url.endsWith(name)
        ));
    }

    function productImagePath(product) {
        const haystack = `${product?.type || ''} ${product?.name || ''} ${product?.categoryName || ''}`.toLowerCase();
        const match = PRODUCT_IMAGE_MAP.find((item) => item.keys.some((key) => haystack.includes(key)));
        return match ? match.path : FALLBACK_IMAGE_PATH;
    }

    function productImageUrl(product) {
        const current = product?.imageUrl || '';
        if (isDefaultImageUrl(current)) {
            return localAssetUrl(productImagePath(product));
        }
        return normalizeImageUrl(current);
    }

    function productStoredImageUrl(product) {
        const current = product?.imageUrl || '';
        if (isDefaultImageUrl(current)) {
            return productImagePath(product);
        }
        return normalizeStoredImageUrl(current);
    }

    function normalizeStoredImageUrl(value) {
        let url = String(value || '').trim();
        if (!url) return '';

        url = url.replace(/\\/g, '/');

        if (/^data:image\//i.test(url) || /^blob:/i.test(url)) {
            return url;
        }

        if (/^\/?assets\//i.test(url) || /^\.\/assets\//i.test(url)) {
            return url.replace(/^\.?\//, '');
        }

        if (/^\/\//.test(url)) {
            return `https:${url}`;
        }

        if (/^www\./i.test(url)) {
            return `https://${url}`;
        }

        if (/^http:\/\//i.test(url) && !/^http:\/\/(localhost|127\.0\.0\.1|0\.0\.0\.0)(:|\/|$)/i.test(url)) {
            return `https://${url.slice(7)}`;
        }

        return url;
    }

    function fallbackImageUrl() {
        return localAssetUrl(FALLBACK_IMAGE_PATH);
    }

    function handleImageError(img) {
        if (!img) return;
        const fallback = fallbackImageUrl();
        if (img.src !== fallback) {
            img.onerror = null;
            img.src = fallback;
        }
    }

    window.imageUtils = {
        fallbackImageUrl,
        handleImageError,
        normalizeImageUrl,
        normalizeStoredImageUrl,
        productStoredImageUrl,
        productImageUrl,
    };

    window.normalizeImageUrl = normalizeImageUrl;
    window.normalizeStoredImageUrl = normalizeStoredImageUrl;
    window.productStoredImageUrl = productStoredImageUrl;
    window.productImageUrl = productImageUrl;
    window.fallbackImageUrl = fallbackImageUrl;
    window.handleImageError = handleImageError;
}());
