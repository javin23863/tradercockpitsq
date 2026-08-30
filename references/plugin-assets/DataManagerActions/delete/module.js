angular.module('app.data').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionTools', 30, {
        key: 'delete',
        title: Ltsq('Mass delete'),
        source: 'data,instruments,sessions,custom-da-ta',
        class: 'delete',
        id: "datamanager-massdelete",
        group: "data-source",
        group1: "instruments-sessions",
        group2: "custom-data",
        group3: "baskets",
    });
});