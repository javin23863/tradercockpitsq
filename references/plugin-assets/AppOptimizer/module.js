//product:SQUANT
angular.module('app.optimizer', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 40, {
        title: Ltsq('Optimizer'),
        appCode: 'OPTIMIZER',
        url: '../OPTIMIZER/index.html',
        iconClass: 'fa-line-chart',
        section: 0,
        iframeId: 'optimizer-frame',
        linkId: 'optimizer-link',
        onlyInPro: true
    });

});