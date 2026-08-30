//product:SQUANT
angular.module('app.builder', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("NavigationPlugin", 20, {
        title: Ltsq('Builder'),
        appCode: 'BUILDER',
        url: '../BUILDER/index.html',
        iconClass: 'fa-plus-square',
        section: 0,
        iframeId: 'builder-frame',
        linkId: 'builder-link'
    });

});