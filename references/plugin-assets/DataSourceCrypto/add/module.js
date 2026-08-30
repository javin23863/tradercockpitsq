angular.module('app.datasource.crypto').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceCrypto',
        10,
        {
            title: Ltsq('Crypto') + ':' + Ltsq('Add crypto symbol'),
            templateUrl: '../../../plugins/DataSourceCrypto/add/addPopup.html',
            controller: 'cryptoAddPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-crypto-add",
            items: [
                {id: 'Binance', name: 'Binance', title: Ltsq('Add Binance symbol(s)')},
                {id: 'BinanceCoinM', name: 'BinanceCoinM', title: Ltsq('Add Binance Coin-M symbol(s)')},         
                {id: 'BinanceUsdtM', name: 'BinanceUsdtM', title: Ltsq('Add Binance USDT-M symbol(s)')},         
                {id: 'Bitfinex', name: 'Bitfinex', title: Ltsq('Add Bitfinex symbol(s)')},
                {id: 'Poloniex', name: 'Poloniex', title: Ltsq('Add Poloniex symbol(s)')},
                {id: 'Coinbase', name: 'Coinbase Pro', title: Ltsq('Add Coinbase Pro symbol(s)')}    
            ]
        }
    );
});