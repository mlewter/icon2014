

angular.module('icon2014').controller('OAuthCtrl', function($scope, $rootScope, $location, $http, $routeParams) {
    var payload = {
        scope : $routeParams.scope,
        code  : $routeParams.code,
        error : $routeParams.error
    }

    window.opener.postMessage(payload, ["*"]);
});
