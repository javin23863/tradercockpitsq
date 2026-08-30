//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.tools.edit.parameters', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin("ResultsDatabankAction", 51, {
        title: Ltsq("Tools")+":"+Ltsq("Edit")+":"+Ltsq("Parameters"),
        controller: 'EditParametersCtrl',
        id: "databank-action-tools-edit-params",
        icon: "fa fa-list-ul",
    });

    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/tools/edit/editParameters/EditParametersPopup.html", 'EditParamsPopupCtrl', 'SQUANT');

});
