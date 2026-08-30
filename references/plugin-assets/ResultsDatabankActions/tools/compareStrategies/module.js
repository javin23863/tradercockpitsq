angular.module('app.resultsdatabankactions.tools.compareStrategies', ['sqplugin']).config(function(sqPluginProvider) {

    sqPluginProvider.plugin("ResultsDatabankAction", 400, {
        title: Ltsq("Tools:Compare"),
        controller: 'CompareStrategiesCtrl',
        class: 'btn btn-normal btn-default',
        id: "databank-action-tools-compare",
        icon: "fa fa-clone",
    });

    sqPluginProvider.addPopupWindow("../../../plugins/ResultsDatabankActions/tools/compareStrategies/compareStrategiesPopup.html", 'CompareStrategiesPopupCtrl', 'SQUANT');

});