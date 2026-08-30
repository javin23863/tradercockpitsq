//product:SQUANT
angular.module('app.sqxhome', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 0, {
        title: Ltsq('Getting started'),
        appCode: 'SQXHOME',
        url: '../SQXHOME/index.html',
        iconClass: 'fa-home',
        section: 0,
        iframeId: 'sqxhome-frame',
        linkId: 'sqxhome-link'
    });

});