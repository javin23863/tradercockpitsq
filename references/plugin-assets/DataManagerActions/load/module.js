angular.module('app.data').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionTools', 50, {
        key: 'load',
        title: Ltsq('Load'),
        source: 'data,instruments,sessions,custom-da-ta,brokers,baskets',
        class: 'load',
        id: "datamanager-load",
        group: "data-source",
        group1: "instruments-sessions",
        group2: "custom-data",
        group3: 'baskets'
    });
});