angular.module('app').config(function(sqPluginProvider) {
    
	sqPluginProvider.plugin("CodeEditorPlugin", 10, {
        title: Ltsq('Import/Export:Import extensions'),
        help: Ltsq('Import extensions'),
        iconClass: "fa fa-upload",
        controller: "CEImportCtrl",
        templateUrl: '../../../plugins/CodeEditorImportExport/import/views/overwriteConfirm.html'
    });
});