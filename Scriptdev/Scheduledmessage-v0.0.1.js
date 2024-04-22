// ==SE_module==
// name: test
// displayName: Test
// description: A script for test
// version: 0.0.1
// author: Jacob
// ==/SE_module==

var networking = require("networking");
var messaging = require("messaging");
var config = require("config");
var im = require("interface-manager");
var ipc = require("ipc");
var javaInterfaces = require("java-interfaces");
var hooker = require("hooker");
var events = require("events");

(function () {
	'use strict';
	var conversationToolboxContext = {
		events: [],
	};
	
	// Replace Inside " " with your custom messag
	var inputMessage = "Input You're Text Here";
	// Replace Inside " " with your Time preference
	var time = "2000";
	
	function createConversationToolboxUI() {
		conversationToolboxContext.events.push({
			start: function (builder, args) {
				builder.button("Send Message", function () {
					var conversationId = args["conversationId"];
					sleep(time)
						messaging.sendChatMessage(conversationId, inputMessage, function () { });
				});
			});
	};
	
	var snapActivityContext = {
        activity: null,
        events: [],
	};
	
	function start(_a) {
        _a.snapActivityContext; _a.conversationToolboxContext; _a.settingsContext;
        createInterface();
	}
	
	function createInterface() {
        createConversationToolboxUI();
	}
	
	var snapApplicationContext = {
        context: null,
        events: [],
	};
	
	var snapEnhancerContext = {
        context: null,
        events: [],
	};
	
	var unloadContext = {
        events: [],
	};
	start({
		snapActivityContext: snapActivityContext,
		snapApplicationContext: snapApplicationContext,
		snapEnhancerContext: snapEnhancerContext,
		unloadContext: unloadContext,
		conversationToolboxContext: conversationToolboxContext,
	});
	module.onSnapMainActivityCreate = function (activity) {
		snapActivityContext.activity = activity;
		snapActivityContext.events.forEach(function (event) {
			event.start(activity, null);
		});
	};
	module.onUnload = function () {
		unloadContext.events.forEach(function (event) {
			event.start(null, null);
		});
	};
	im.create("conversationToolbox", function (builder, args) {
		conversationToolboxContext.events.forEach(function (event) {
			event.start(builder, args);
		});
	});
})();
