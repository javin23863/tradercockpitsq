//product:SQUANT
angular.module('app.algowizard', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("NavigationPlugin", 31, {
        title: Ltsq('AlgoWizard'),
        appCode: 'AlgoWizard',
        url: null, //loading in AppService dynamically
        iconClass: 'fa-magic',
        section: 1,
        iframeId: 'algowizard-frame',
        linkId: 'algowizard-frame-link'
    });

});