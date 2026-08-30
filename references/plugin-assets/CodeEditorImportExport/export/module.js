angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CodeEditorPlugin", 20, {
        title: Ltsq('Import/Export:Export extensions'),
        help: Ltsq('Export extensions'),
        iconClass: "fa fa-download",
        popupId: '#CEExportPopup',
        templateUrl: '../../../plugins/CodeEditorImportExport/export/views/exportPopup.html',
        controller: "CEExportPopupCtrl"
    });
});