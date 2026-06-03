(function () {
    const FALLBACK_IMAGE_PATH = 'assets/images/placeholder.svg';

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
    };

    window.normalizeImageUrl = normalizeImageUrl;
    window.normalizeStoredImageUrl = normalizeStoredImageUrl;
    window.fallbackImageUrl = fallbackImageUrl;
    window.handleImageError = handleImageError;
}());
