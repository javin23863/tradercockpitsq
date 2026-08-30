//product:SQUANT
angular.module('app.sqxbusiness', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 50, {
        title: Ltsq('SQ 4 Business'),
        appCode: 'SQXBUSINESS',
        url: '../SQXBUSINESS/dist/index.html',
        iconClass: 'fa-briefcase',
        section: 1,
        iframeId: 'sqxbusiness-frame',
        linkId: 'sqxbusiness-frame-link',
        hidden: true
    });

});