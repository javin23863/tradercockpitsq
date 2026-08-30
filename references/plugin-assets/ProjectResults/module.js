angular.module('app.results', [
    'sqplugin',
    'app.resultstabs.resultsplugins',
    'app.customresultspluginactions.rename',
    'app.customresultspluginactions.delete'
]).config(function(SQEventsProvider, sqPluginProvider) {
	
    SQEventsProvider.add("STRATEGY_SELECTED","databank-strategies-selected");           //called after clicking the strategy in databank grid 
    // SQEventsProvider.add("RESULTS_SPLITPANE_RESIZE","results-splitpane-resize");        //called after the change of splitpane dimensions 
    SQEventsProvider.add("DATABANK_UPDATED","results-databank-updated");                //called after the databank contents change
    SQEventsProvider.add("DATABANK_CHANGED","results-databank-changed");                //called after databank tab is selected
    SQEventsProvider.add("RELOAD_DATABANKS","results-reload-databanks");                //called to reload all databanks from the project's xml config
    SQEventsProvider.add("DATABANK_ACTIONS_MODIFIED",'databank-actions-modified');      //called after the modification of a databank action (action's xml config is included in args)
    SQEventsProvider.add("DATABANK_ACTIONS_RELOAD",'databank-actions-reload');          //called to reload all databank actions
    SQEventsProvider.add("MODIFY_CONDITION",'modify-condition');                        //called by DatabankActionTask to open condition modification dialog
    SQEventsProvider.add("PROJECT_STATUS_CHANGED",'project-status-changed');

    SQEventsProvider.add('APP_PROJECT_CHANGED', 'app-project-changed');

    sqPluginProvider.plugin("AppPanelResults", 40, {
        dataItem: 'results',
        templateUrl: '../../../plugins/ProjectResults/results.html',
        controller: 'ResultsCtrl',
    });
});