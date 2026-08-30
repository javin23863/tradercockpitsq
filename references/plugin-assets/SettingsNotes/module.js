//product:BUILDER
//product:OPTIMIZER
//product:RETESTER
//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 100, {
        title: Ltsq('Notes'),
        help: Ltsq('Save some notes about this configuration'),
        helpURL: 'notes',
        task: 'Build,Optimize,Retest,NeuralNetworkTrainer',
        configElemName: 'Notes',
        dataItem: 'settings-notes',
        templateUrl: '../../../plugins/SettingsNotes/notes.html',
        controller: 'NotesCtrl',
        getSettingsDescription : function(settingsElement, injector){
            var L = injector.get("L");
            var settingsText = L.tsq("Empty");
		    var maxLength = 32;
        
            var notesContent = settingsElement.childNodes[0] ? settingsElement.childNodes[0].nodeValue : L.tsq("Empty");
			return notesContent.length > maxLength ? (notesContent.substr(0, maxLength) + "...") : notesContent;
        }
    });
});