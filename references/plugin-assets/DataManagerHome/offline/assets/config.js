// Offline Page Configuration
// Edit this file to customize content, links, and FAQ







const config = {
    pageTitle: "QDM Offline - Documentation",

    logo: {
        src: "./img/qdm-logo.png",
        alt: "QuantDataManager"
    },

    header: {
        title: "You are offline - cannot load QDM Home page",
        subtitle: "Access documentation and FAQ below"
    },

    docLinks: [
        {
            id: 1,
            icon: "download",
            title: "Import History Data MT4",
            description: "Learn how to import historical data to MetaTrader 4",
            url: "https://strategyquant.com/doc/quantdatamanager/import-history-data-metatrader-4/"
        },
        {
            id: 2,
            icon: "speed",
            title: "Test Strategy MT4",
            description: "Test your strategy in MT4 with tick precision",
            url: "https://strategyquant.com/doc/quantdatamanager/test-strategy-metatrader-4-tick-precision/"
        },
        {
            id: 3,
            icon: "input",
            title: "Import Data to MT5",
            description: "How to import data to MetaTrader 5",
            url: "https://strategyquant.com/doc/quantdatamanager/how-to-import-data-to-metatrader-5/"
        },
        {
            id: 4,
            icon: "output",
            title: "Export Data from MT5",
            description: "How to export data from MetaTrader 5",
            url: "https://strategyquant.com/doc/quantdatamanager/how-to-export-data-from-metatrader-5/"
        },
        {
            id: 5,
            icon: "terminal",
            title: "Introduction to CLI",
            description: "What is CLI and how to use it",
            url: "https://strategyquant.com/doc/cli-command-line/introduction-to-cli/"
        },
        {
            id: 6,
            icon: "menu_book",
            title: "Full Documentation",
            description: "Browse complete QDM documentation",
            url: "https://strategyquant.com/doc/quantdatamanager/"
        }
    ],

    faq: {
        title: "Frequently Asked Questions",
        subtitle: "Have a different question about Quant Data Manager? Get in touch with one of our specialists.",
        items: [
            {
                id: 1,
                question: "Is the data provided for free? What is the data source?",
                answer: "High quality tick data for forex and CFDs are provided for free by Dukascopy (available directly in app) and by several other providers.\n\nQuantDataManager downloads these data more conveniently, and – in case of Pro version – also much faster.\nWith Pro version you are not paying for the data (they are downloadable for free), only for more convenient or faster way of downloading them."
            },
            {
                id: 2,
                question: "What does Verified downloads * mean for paid version?",
                answer: "Verified downloads mean that the data downloaded by QDM Pro are verified - they are exactly the same as provided by the data provider - for example Dukascopy. So you will be not affected by unstable Internet connection, source server down, etc.\n\nNote - it does NOT mean that the data will not have gaps - if the data source has gaps, so will the downloaded data. But they will not have additional gaps caused by unstable connection while data download."
            },
            {
                id: 3,
                question: "Can QuantDataManager download data for stocks, commodities, crypto?",
                answer: "QuantDataManager supports multiple data sources, including the ones for stocks and crypto. Commodity data are paid, there is no free source available."
            },
            {
                id: 4,
                question: "Can I get a refund of my order?",
                answer: "Sorry, you can try QuantDataManager in a free version, so no refunds are given."
            },
            {
                id: 5,
                question: "Can I use QuantDataManager on multiple computers?",
                answer: "QDM licenses can't work on more computers simultaneously, however you can change computers anytime after a license reset: either at the Dashboard of your client area after logging on our website (limit of 5 resets) or then by contacting us.\n\nIf you need to use the app on more computers, you need to buy more licenses."
            },
            {
                id: 6,
                question: "How can I activate Pro version?",
                answer: "You will get your license by email after purchase.\n\nThen you need to open QuantDataManger and click the link in footer -> Update your license.\n\nInsert your license code and update."
            },
            {
                id: 7,
                question: "How I can transfer tick data from QuantDataManager to MetaTrader?",
                answer: "QuantDataManager now allows you to export special FXT & HST files for MetaTrader 4 that allow you to test your strategies in MT4 with the highest possible modeling quality.\n\nPlease check the documentation for QDM at: https://strategyquant.com/doc/quantdatamanager/"
            }
        ]
    },

    footer: {
        text: "Need help? Check our",
        documentationUrl: "https://strategyquant.com/doc/quantdatamanager/",
        contactUrl: "https://strategyquant.com/contactus/"
    }
};


// Make config globally available
if (typeof window !== "undefined") {
    window.QDM_OFFLINE_CONFIG = config;
}
