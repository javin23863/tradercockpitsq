//product:SQUANT
angular.module('app.codeEditor', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("NavigationPlugin", 40, {
        title: Ltsq('Code Editor'),
        appCode: 'SQEDITOR',
        url: '../SQEDITOR/index.html',
        iconClass: 'fa fa-code',
        section: 1,
        iframeId: 'code-editor-frame',
        linkId: 'code-editor-link',
        menu: 'top',
        onlyInPro: true
    });

});