//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.databanks', ['sqplugin']).config(function(SQEventsProvider) {

    SQEventsProvider.add('SETTINGS_RANKING_CHANGED', 'settings-ranking-changed'); 
    SQEventsProvider.add('DATABANK_DISPLAY', 'databank-display'); 
    SQEventsProvider.add('DATABANKS_LOADED', 'databanks-loaded'); 
    SQEventsProvider.add('DATABANK_SET_VIEW', 'databank-set-view'); 
    SQEventsProvider.add('DATABANK_VIEW_SELECTED', 'databank-view-selected'); 
    SQEventsProvider.add('CUSTOM_DATABANK_ACTION', 'custom-databank-action'); 
    
});