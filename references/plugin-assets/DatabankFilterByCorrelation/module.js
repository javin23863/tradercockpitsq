angular.module('app.resultsdatabankactions.filterByCorrelation', ['sqplugin']).config(function(sqPluginProvider, $controllerProvider) {
    
    function callback(projectName, taskName, databankName, selectedStrategies){
        window.parent.filterByCorrelationData = {
            projectName,
            taskName,
            databankName
        };
        window.parent.showPopup("#filterByCorrelationButtonModal");
    }

    let buttonController = new CustomPluginController('app.resultsdatabankactions.filterByCorrelation', $controllerProvider, callback).init();

    sqPluginProvider.plugin("ResultsDatabankAction", 56, {
        title: Ltsq("Filter by correlation"),
        class: 'btn btn-normal btn-default',
        controller: buttonController,
        id: "databank-action-filterbycorrelation"
    });
    
    sqPluginProvider.addPopupWindow("plugins/DatabankFilterByCorrelation/databankFilterByCorrelationPopup.html", null, 'SQUANT');

});
