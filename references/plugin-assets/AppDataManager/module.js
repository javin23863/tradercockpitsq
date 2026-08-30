angular.module('app.dataManager', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("NavigationPlugin", 10, {
        title: Ltsq('Data Manager'),
        appCode: 'SQMANAGER',
        url: '../SQMANAGER/index.html',
        iconClass: 'fa-database',
        section: 1,
        iframeId: 'data-manager-frame',
        linkId: 'data-manager-link'
    });

});