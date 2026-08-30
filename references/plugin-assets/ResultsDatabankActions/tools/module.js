angular.module('app.resultsdatabankactions.tools', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 70, {
        title: Ltsq("Tools")+":",
        id: "databank-action-tools-menu",
    });

});