angular.module('icon2014').controller('MainCtrl', function($scope, $rootScope, $window, $location, $http, UserService) {

    var PAGE_SIZE = 10;

    $scope.contacts = [];
    $scope.tagId = '';
    $scope.tags = [];
    $scope.contactCount = 0;
    $scope.numPages = 0;
    $scope.currentPage = 0;

    $scope.loadContactCount = function() {
        var url = "/contactCount?oAuthToken=" + UserService.getAuthToken() + "&tagId=" + $scope.tagId;

        $http.get(url).success(function(data) {
            $scope.contactCount = data.count;
            $scope.calcPages();
        }).error(function() {
            console.log("error");
        });
    }

    $scope.loadContacts = function(resetCount) {
        if(resetCount) {
            $scope.loadContactCount();
            $scope.currentPage = 0;
        }

        var url = "/contacts?oAuthToken=" + UserService.getAuthToken()
                  + "&tagId=" + $scope.tagId
                  + "&page="  + $scope.currentPage;

        $http.get(url).success(function(data) {
            $scope.contacts = data.contacts;
        }).error(function() {
            console.log("error");
        });
    }

    $scope.loadTags = function() {
        var url = "/tags?oAuthToken=" + UserService.getAuthToken();

        $http.get(url).success(function(data) {
            $scope.tags = data.tags;
        }).error(function() {
            console.log("error");
        });
    }

    $scope.calcPages = function() {
        if($scope.contactCount > PAGE_SIZE) {
            $scope.numPages = Math.ceil($scope.contactCount / PAGE_SIZE);
        }
        else {
            $scope.numPages = 1;
        }
    }

    $scope.changePage = function(direction) {
        $scope.currentPage = $scope.currentPage + direction;
        $scope.loadContacts(false);
    }

    $scope.loadTags();
    $scope.loadContacts(true);
});