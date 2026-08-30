angular.module('app.resultsdatabankactions.tools.runCa', ['sqplugin']).config(function(sqPluginProvider) {

    sqPluginProvider.plugin("ResultsDatabankAction", 500, {
        title: Ltsq("Tools")+":"+Ltsq("Run CA"),
        controller: 'RunCaCtrl',
        class: 'btn btn-normal btn-default',
        id: "databank-action-tools-compare",
        icon: "fa fa-list-ul",
    });

    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/tools/runCa/runCa.html", 'RunCaPopupCtrl', 'SQUANT');

});