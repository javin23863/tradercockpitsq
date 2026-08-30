//product:SQUANT
angular.module('app.retester', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 30, {
        title: Ltsq('Retester'),
        appCode: 'RETESTER',
        url: '../RETESTER/index.html',
        iconClass: 'fa-filter',
        section: 0,
        iframeId: 'retester-frame',
        linkId: 'retester-link'
    });

});