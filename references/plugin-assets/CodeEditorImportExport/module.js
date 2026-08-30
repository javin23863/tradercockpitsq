angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CodeEditorPlugin", 24, {
        title: Ltsq('Import/Export:'),
        help: Ltsq('Import/Export extensions'),
        iconClass: "icon ico-find"
    });
});