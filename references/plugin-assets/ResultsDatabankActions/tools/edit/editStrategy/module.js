//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.tools.edit.strategy', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("ResultsDatabankAction", 51, {
        title: Ltsq("Tools")+":"+Ltsq("Edit")+":"+Ltsq("Strategy"),
        class: 'btn btn-normal btn-default',
        controller: 'EditStrategyCtrl',
        id: "databank-action-tools-edit-str",
        icon: "fa fa-line-chart",
    });
});
