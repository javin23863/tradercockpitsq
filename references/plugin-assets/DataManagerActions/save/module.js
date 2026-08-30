angular.module('app.data').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionTools', 40, {
        key: 'save',
        title: Ltsq('Save'),
        source: 'data,instruments,sessions,custom-da-ta,brokers,baskets',
        class: 'export',
        id: "datamanager-save",
        group: "data-source",
        group1: "instruments-sessions",
        group2: "custom-data",
        group3: 'baskets'
    });
});