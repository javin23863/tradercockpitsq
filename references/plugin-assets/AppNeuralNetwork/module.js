//product:SQUANT
angular.module('app.neuralNetwork', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("NavigationPlugin", 50, {
        title: Ltsq('Neural Network Trainer'),
        appCode: 'NEURALNETWORK',
        url: '../NEURALNETWORK/index.html',
        iconClass: 'fa-home',
        section: 0,
        iframeId: 'nnt-frame',
        linkId: 'nnt-link',
        hidden: true
    });

});