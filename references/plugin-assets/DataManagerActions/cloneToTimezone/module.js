angular.module('app.data').config(function(sqPluginProvider) {
      
    sqPluginProvider.plugin('DataManagerActionTools', 10, {
        title: Ltsq('Clone to timezone'),
        templateUrl: '../../../plugins/DataManagerActions/cloneToTimezone/cloneToTimezone.html',
        controller: 'CloneToTimezoneCtrl',
        source: 'data',
        class: 'clone',
        id: "datamanager-clone"
    });
});