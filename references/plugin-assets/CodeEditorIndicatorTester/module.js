angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CodeEditorPlugin", 10, {
        title: Ltsq('Test indicators'),
        help: Ltsq('Test indicator values between platforms'),
        iconClass: "icon ico-find",
        popupId: '#indicatorTesterPopup',
        templateUrl: '../../../plugins/CodeEditorIndicatorTester/views/indicatorTesterPopup.html',
    });
});