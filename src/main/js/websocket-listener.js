'use strict';

const SockJS = require('sockjs-client');
require('stompjs');

//function register(registrations, connectedFunction) {
function register(registrations, stompClientReady) {
	const socket = SockJS('/webwallet');
	const stompClient = Stomp.over(socket);

	stompClient.connect({}, function(frame) {
		registrations.forEach(function (registration) {
			stompClient.subscribe(registration.route, registration.callback);
		});
		stompClientReady();
	});
}

module.exports.register = register;