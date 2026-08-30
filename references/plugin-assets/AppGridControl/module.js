//product:SQUANT
angular.module('app.gridControl', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("NavigationPlugin", 100, {
        title: Ltsq('Grid Control'),
        appCode: 'GRIDCONTROL',
        url: '../GRIDCONTROL/index.html',
        iconClass: 'fa fa-th-large',
        section: 1,
        iframeId: 'gridcontrol-frame',
        linkId: 'gridcontrol-link',
        menu: 'top'
    });

});