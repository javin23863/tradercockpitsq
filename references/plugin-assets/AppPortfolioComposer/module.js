//product:SQUANT
angular.module('app.portfoliocomposer', ['sqplugin']).config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 51, {
        title: Ltsq('Portfolio Composer'),
        appCode: 'PORTFOLIOCOMPOSER',
        url: '../PORTFOLIOCOMPOSER/index.html',
        iconClass: 'fa-pie-chart',
        section: 0,
        iframeId: 'portfoliocomposer-frame',
        linkId: 'portfoliocomposer-link'
    });

});