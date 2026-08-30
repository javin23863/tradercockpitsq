//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.save.sourceCode', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 40, {
        title: Ltsq("Save")+":"+Ltsq("Source code"),
        class: "source-code-menu",
        id: "databank-action-sourcecode-menu",
        icon: "fa fa-code",
    });
});
