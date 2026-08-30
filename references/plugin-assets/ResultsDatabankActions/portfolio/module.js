angular.module('app.resultsdatabankactions.portfolio', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 60, {
        title: Ltsq("Portfolio") + ":",
        id: "databank-action-portfolio-menu"
    });
    
});
