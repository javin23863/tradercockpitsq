//product:SQUANT
angular.module('app.resultsapp', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 200, {
        title: Ltsq('Results'),
        appCode: 'RESULTS',
        url: '../RESULTS/index.html',
        iconClass: 'fa-home',
        section: 2,
        iframeId: 'results-frame',
        linkId: 'results-link',
        hidden: true
    });

});