//product:SQUANT
angular.module('app.portfoliomaster', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 50, {
        title: Ltsq('Portfolio Master'),
        appCode: 'PORTFOLIOMASTER',
        url: '../PORTFOLIOMASTER/index.html',
        iconClass: 'fa-area-chart',
        section: 0,
        iframeId: 'portfoliomaster-frame',
        linkId: 'portfoliomaster-link'
    });

});