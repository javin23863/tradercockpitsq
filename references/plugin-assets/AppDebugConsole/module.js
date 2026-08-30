//product:SQUANT
angular.module('app.debugConsole', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("NavigationPlugin", 30, {
        title: Ltsq('Debug Console'),
        appCode: 'DEBUGCONSOLE',
        url: '../DEBUGCONSOLE/index.html',
        iconClass: 'fa fa-bug',
        section: 1,
        iframeId: 'debug-console-frame',
        linkId: 'debug-console-link',
        menu: 'top',
        onlyInPro: true
    });

});