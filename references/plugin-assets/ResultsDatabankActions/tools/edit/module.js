angular.module('app.resultsdatabankactions.tools.edit', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 51, {
        title: Ltsq("Tools")+":"+Ltsq("Edit")+":",
        id: "databank-action-edit-menu",
        class: 'normal-submenu',
    });
    
});