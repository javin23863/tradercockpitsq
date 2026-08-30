//product:SQUANT
angular.module('app.gridtest', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("NavigationPlugin", 150, {
        title: Ltsq('Grid test'),
        appCode: 'GRIDTEST',
        url: '../GRIDTEST/index.html',
        iconClass: 'fa-home', /*'ico-home',*/
        section: 2,
        iframeId: 'gridtest-frame',
        linkId: 'gridtest-link',
        hidden: true
    });

});