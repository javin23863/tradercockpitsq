//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.resultsdatabankactions.load', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("ResultsDatabankAction", 10, {
        title: Ltsq("Load"),
        class: 'btn btn-normal btn-default',
        controller: 'loadBtnCtrl',
        id: "databank-action-load"
    });
    
    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/load/loadPopup.html", 'LoadPopupCtrl', 'SQUANT');

});
