angular.module('app.datasource.darwinex', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin(
        "DataSourceDarwinex",
        10, {
            title: Ltsq('Darwinex Tick Data') + ':',
            class: "darwinex",
            source: 'data',
            id: "datamanager-datasource-darwinex-menu"
        }
    );
});