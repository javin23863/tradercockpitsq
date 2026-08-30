// Theme Switcher for QDM Onboarding
(function () {
    'use strict';

    const THEME_ATTR = 'data-theme';

    /**
     * Apply theme to document root
     */
    function applyTheme(theme) {
        document.documentElement.setAttribute(THEME_ATTR, theme);
        console.log(`✓ Theme switched to: ${theme}`);
    }

    window.openLink = function(link) {
        window.parent.postMessage({ type: "OPEN_LINK", link }, "*");
    }
    
    window.addEventListener("message", (e) => {
        if (e.data?.type === "SET_THEME") {
            applyTheme(e.data.theme);
        }
    });
})();
