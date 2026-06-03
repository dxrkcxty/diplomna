(function () {
    const FALLBACK_IMAGE_PATH = 'assets/images/placeholder.svg';
    const DEFAULT_IMAGE_NAMES = [
        'product-default.svg',
        'placeholder.svg',
    ];
    const PRODUCT_IMAGE_MAP = [
        { keys: ['\u0440\u044e\u043a\u0437\u0430\u043a'], path: 'https://cdn.blazimg.com/1800/product/1/0/100426101_b.jpg' },
        { keys: ['\u0441\u0443\u043c\u043a'], path: 'https://cdn.blazimg.com/1800/product/u/h/uhlsport_100121701_mag4344169_1.jpg' },
        { keys: ['\u0431\u0443\u0442\u0441', 'predator', 'mercurial', 'future'], path: 'https://cdn.blazimg.com/1800/product/p/1/p1ga243425.jpg' },
        { keys: ['\u0444\u0443\u0442\u0431\u043e\u043b\u043a', '\u0441\u0432\u0456\u0442\u0448\u043e\u0442'], path: 'https://cdn.blazimg.com/1800/product/5/0/50520201.jpg' },
        { keys: ['\u0448\u043e\u0440\u0442'], path: 'https://cdn.blazimg.com/1800/product/6/5/657249-06_0_puma__pum-657249-06__imagefront.jpg' },
        { keys: ['\u0433\u0435\u0442\u0440'], path: 'https://cdn.blazimg.com/1800/product/p/r/precision_708mr_royal_1.jpg' },
        { keys: ['\u0442\u0435\u0440\u043c\u043e\u0431\u0456\u043b\u0438\u0437\u043d', '\u043a\u043e\u0441\u0442\u044e\u043c'], path: 'https://cdn.blazimg.com/1800/product/9/3/9321_9221_802.jpg' },
        { keys: ['\u043a\u0443\u0440\u0442\u043a'], path: 'https://cdn.blazimg.com/1800/product/u/n/under-armour_1387162-002_0_0.jpg' },
        { keys: ['\u0440\u0443\u043a\u0430\u0432\u0438\u0446'], path: 'https://cdn.blazimg.com/1800/product/r/i/rinat_nkra1400_red-white_1.jpg' },
        { keys: ['\u0448\u0442\u0430\u043d\u0438'], path: 'https://cdn.blazimg.com/1800/product/3/0/303jv30_005_goalkeeper_pant_01.jpg' },
        { keys: ['\u0449\u0438\u0442\u043a', '\u0444\u0456\u043a\u0441\u0430\u0442\u043e\u0440'], path: 'https://cdn.blazimg.com/1800/product/u/h/uhlsport_100680901_0.jpg' },
        { keys: ['\u043f\u043b\u044f\u0448\u043a'], path: 'https://cdn.blazimg.com/1800/product/n/i/nike_n1007643-088_black-black-malachite_1.jpg' },
        { keys: ['\u043c\'\u044f\u0447', '\u043c\u2019\u044f\u0447', 'ball'], path: 'https://cdn.blazimg.com/1800/product/e/r/erima_7192403_0.jpg' },
        { keys: ['\u0456\u043d\u0432\u0435\u043d\u0442\u0430\u0440', '\u0434\u0440\u0430\u0431\u0438\u043d', '\u043a\u043e\u043d\u0443\u0441'], path: 'https://cdn.blazimg.com/1800/product/p/r/precision_trl233e_charcoal-black-grey_1.jpg' },
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
            return normalizeImageUrl(productImagePath(product));
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
