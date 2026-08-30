angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBasket', 20, {
        title: Ltsq('Update data in group (automatic)'),
        key: 'update',
        source: 'baskets',
        class: 'baskets',
        id: "datamanager-basket-of-stocks-update"
    });
});