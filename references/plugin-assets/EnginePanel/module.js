angular.module('app.engine', ['sqplugin']).config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("MainPanel", 40, {
        templateUrl: '../../../plugins/EnginePanel/engine.html',
        controller: 'EngineCtrl',
        class: { 'sqn-engine' : true },
        name: 'engine'
    });
});