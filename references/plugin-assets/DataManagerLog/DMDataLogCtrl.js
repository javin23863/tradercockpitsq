angular.module('app.dataLog').controller('DMDataLogCtrl', function ($scope, $rootScope, $q, $injector, $timeout, SQEvents, sqPlugin, SQConstants, SQWebSocketService, L) {

	  $scope.clearLog = function () {
        $scope.totalLog = "";
    }

	function onWebSocketData(args) {
		if (args.DMDataProgresses) {
			var progresses = args.DMDataProgresses;
			for (var i = 0; i < progresses.length; i++) {
				var progress = progresses[i];

				refreshQueue(progress);

				var now = new Date();
				var month = now.getMonth() + 1;
				var t = now.getFullYear() + "." + month + "." + now.getDate() + " " + now.toLocaleTimeString('en-US', { hour12: false, hour: "numeric", minute: "numeric" });

				if (progress.logStr) {
					$scope.totalLog = (t + " " + progress.key + " " + progress.logStr + "<br>" + $scope.totalLog).substring(0, 10000);
				}				
			}

			try { $scope.$digest(); } catch (err) { }
		}
	}

	function refreshQueue(data) {
		var item = getItem($scope.queue, 'key', data.key);
        
		if (data.pct == 100 || data.pct < 0) {
			if (item) {
				removeItem($scope.queue, 'key', data.key)
			}

			return;
		}

		if (!item) {
			item = data;

			$scope.queue.push(item);
		}

		if (item.refresh) item.refresh(data);
	}

	$scope.totalLog = "";

	var listenerId = "DMDataLogCtrl-" + window.appConfig.appCode;
	SQWebSocketService.subscribeGeneral(listenerId, onWebSocketData);

	$scope.queue = [];
});