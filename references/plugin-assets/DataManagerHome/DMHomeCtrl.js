angular.module("app.dmhome").controller("DMHomeCtrl", function($scope, SQEvents, DMHomeService, SkinService, $timeout, AppService) {
           
    $scope.homeLink = "";

    DMHomeService.link(function(response) {
        document.getElementById("qdm-home-iframe").src = response.link;

        $timeout(function () {
            updateSkin();
        }, 500);
        
    });

    function updateSkin() {
        let theme = SkinService.selectedSkin.name == "Dark skin" ? 'dark' : 'light';
        console.log(`✓ Updating Home iframe theme to: ${theme}`);

        const iframe = document.getElementById("qdm-home-iframe");
        iframe.contentWindow.postMessage({ type: "SET_THEME", theme }, "*");
    }

    window.addEventListener("message", (e) => {
        if (e.data?.type === "OPEN_LINK") {
            AppService.openBrowser(e.data.link);
        }
    });

    window.parent.addListener("EquityChart", [SQEvents.get("SKIN_CHANGED")], updateSkin);
});